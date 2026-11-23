package com.edgefan.edgerunners.cyber;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.effect.ModEffects;
import com.edgefan.edgerunners.item.MantisBladeItem;
import com.edgefan.edgerunners.network.FxPacket;
import com.edgefan.edgerunners.network.ImplantsPacket;
import com.edgefan.edgerunners.network.Network;
import com.edgefan.edgerunners.network.SyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EdgerunnersMod.MODID)
public final class CyberEvents {
    private static final String[] WHISPERS = {
            "msg.edgerunners.whisper1", "msg.edgerunners.whisper2", "msg.edgerunners.whisper3", "msg.edgerunners.whisper4"
    };

    private static final String LAST_VISIBLE = "lastVisible";

    private CyberEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;

        for (String k : new String[]{CyberData.SANDE_CD, CyberData.MONO_CD, CyberData.SCAN_CD}) {
            int v = CyberData.get(p, k);
            if (v > 0) CyberData.set(p, k, v - 1);
        }

        int sande = CyberData.get(p, CyberData.SANDE);
        if (sande > 0) {
            Abilities.tickSandevistan(p, sande);
            CyberData.set(p, CyberData.SANDE, sande - 1);
        }

        tickPsychosis(p);
        if (p.tickCount % 20 == 0) ChromeBonuses.sync(p);

        if (p.tickCount % 4 == 0) Network.toPlayer(p, SyncPacket.of(p));
        // Let everyone nearby see this player's chrome (cheap: one small packet per second, or on state change).
        int mask = CyberData.implants(p);
        boolean active = CyberData.get(p, CyberData.SANDE) > 0;
        int visible = mask | (active ? 1 << 30 : 0);
        if (p.tickCount % 20 == 0 || CyberData.get(p, LAST_VISIBLE) != visible) {
            CyberData.set(p, LAST_VISIBLE, visible);
            Network.toTrackingAndSelf(p, new ImplantsPacket(p.getId(), mask, active));
        }
    }

    private static void tickPsychosis(ServerPlayer p) {
        float psy = CyberData.psychosis(p);
        float base = CyberData.baseline(p);
        int berserk = CyberData.get(p, CyberData.BERSERK);

        if (berserk > 0) {
            CyberData.set(p, CyberData.BERSERK, berserk - 1);
            if (berserk == 1) {
                CyberData.setPsychosis(p, Math.max(base, 70));
                p.displayClientMessage(Component.translatable("msg.edgerunners.berserk_end").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }

        if (psy > base) CyberData.setPsychosis(p, Math.max(base, psy - 0.02f));
        else if (psy < base) CyberData.setPsychosis(p, Math.min(base, psy + 0.05f));
        psy = CyberData.psychosis(p);

        if (psy >= 100f) {
            triggerCyberpsychosis(p);
        } else if (psy >= 75f && p.tickCount % 20 == 0 && p.getRandom().nextFloat() < 0.08f) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
            String key = WHISPERS[p.getRandom().nextInt(WHISPERS.length)];
            p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        }
    }

    private static void triggerCyberpsychosis(ServerPlayer p) {
        int d = CyberData.BERSERK_DURATION;
        CyberData.set(p, CyberData.BERSERK, d);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, d, 2));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, d, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, d, 1));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, d, 2));
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("msg.edgerunners.berserk_sub").withStyle(ChatFormatting.RED)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("msg.edgerunners.berserk").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.8f, 1.3f);
        Network.fx(p, FxPacket.BERSERK, p.getX(), p.getY(), p.getZ(), 0, 0);
    }

    /** Melee hits: mantis blade slashes and gorilla-arm punches. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p) || event.getSource().getDirectEntity() != p) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        LivingEntity target = event.getEntity();
        double cy = target.getY() + target.getBbHeight() * 0.55;
        if (p.getMainHandItem().getItem() instanceof MantisBladeItem) {
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), 80, 0));
            Network.fx(p, FxPacket.SLASH, target.getX(), cy, target.getZ(), p.getYRot(), p.getXRot());
            return;
        }
        if (!p.getMainHandItem().isEmpty() || !CyberData.has(p, Cyberware.GORILLA_ARMS)) return;
        Network.fx(p, FxPacket.PUNCH, target.getX(), cy, target.getZ(), p.getYRot(), p.getXRot());
        event.setAmount(event.getAmount() + 7f);
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        target.knockback(1.4, Mth.sin(yaw), -Mth.cos(yaw));
        p.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35f, 1.6f);
    }

    /** Reinforced Tendons: softer landings. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && CyberData.has(p, Cyberware.REINFORCED_TENDONS)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.35f);
        }
    }

    /** Chrome stays with you after death; running timers do not. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData().getCompound(CyberData.KEY).copy();
        old.remove(CyberData.SANDE);
        old.remove(CyberData.BERSERK);
        event.getEntity().getPersistentData().put(CyberData.KEY, old);
    }
}
