package com.edgefan.edgerunners.weapon;

import com.edgefan.edgerunners.client.fx.FxManager;
import com.edgefan.edgerunners.cyber.CyberData;
import com.edgefan.edgerunners.hero.HeroSpec;
import com.edgefan.edgerunners.item.ModItems;
import com.edgefan.edgerunners.network.FxPacket;
import com.edgefan.edgerunners.network.Network;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/** Hitscan firearm: hold right-click to fire, sneak + right-click to reload; ammo comes from Ammo items. */
public class GunItem extends Item {
    public final GunSpec spec;

    public GunItem(GunSpec spec, Properties props) {
        super(props.stacksTo(1));
        this.spec = spec;
    }

    public static int ammo(ItemStack s) {
        CompoundTag t = s.getTag();
        return t != null && t.contains("Ammo") ? t.getInt("Ammo") : ((GunItem) s.getItem()).spec.mag;
    }

    private static void setAmmo(ItemStack s, int v) { s.getOrCreateTag().putInt("Ammo", v); }

    public static long reloadUntil(ItemStack s) { return s.hasTag() ? s.getTag().getLong("ReloadUntil") : 0L; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) startReload((ServerPlayer) player, stack);
            return InteractionResultHolder.consume(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (level.isClientSide || !(living instanceof ServerPlayer p)) return;
        CompoundTag tag = stack.getOrCreateTag();
        long now = level.getGameTime();
        if (tag.getLong("ReloadUntil") > now) return;
        if (!spec.auto && tag.getBoolean("Fired")) return;
        if (tag.getLong("Next") > now) return;
        boolean barrage = CyberData.get(p, CyberData.BARRAGE) > 0;
        boolean infinite = p.getAbilities().instabuild || barrage;
        int ammo = ammo(stack);
        tag.putBoolean("Fired", true);
        if (ammo <= 0 && !infinite) {
            if (!startReload(p, stack))
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.6f);
            return;
        }
        fire(p);
        if (!infinite) setAmmo(stack, ammo - 1);
        tag.putLong("Next", now + Math.max(1, barrage ? spec.cooldown / 2 : spec.cooldown));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        stack.getOrCreateTag().putBoolean("Fired", false);
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        stack.getOrCreateTag().putBoolean("Fired", false);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || !stack.hasTag()) return;
        long until = stack.getTag().getLong("ReloadUntil");
        if (until == 0 || level.getGameTime() < until) return;
        stack.getTag().remove("ReloadUntil");
        int need = spec.mag - ammo(stack);
        int got = p.getAbilities().instabuild ? need : takeAmmo(p, need);
        setAmmo(stack, ammo(stack) + got);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    private boolean startReload(ServerPlayer p, ItemStack stack) {
        if (ammo(stack) >= spec.mag || reloadUntil(stack) > 0) return false;
        if (!p.getAbilities().instabuild && countAmmo(p) == 0) {
            p.displayClientMessage(Component.translatable("msg.edgerunners.no_ammo").withStyle(ChatFormatting.RED), true);
            return false;
        }
        float mult = HeroSpec.of(p) == HeroSpec.GUNNER_GIRL ? 0.5f : 1f;
        stack.getOrCreateTag().putLong("ReloadUntil", p.level().getGameTime() + Math.max(5, (int) (spec.reload * mult)));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 0.8f, 1.2f);
        return true;
    }

    public static int countAmmo(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(ModItems.AMMO.get())) n += s.getCount();
        return n;
    }

    private static int takeAmmo(Player p, int need) {
        int got = 0;
        for (ItemStack s : p.getInventory().items) {
            if (got >= need) break;
            if (!s.is(ModItems.AMMO.get())) continue;
            int take = Math.min(need - got, s.getCount());
            s.shrink(take);
            got += take;
        }
        return got;
    }

    // ------------------------------------------------------------ firing
    private void fire(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        float dmgMult = HeroSpec.of(p) == HeroSpec.GUNNER_GIRL ? 1.4f : 1f;
        Map<LivingEntity, Float> hits = new LinkedHashMap<>();
        LivingEntity homingTarget = spec.homing ? findHomingTarget(p, eye, look) : null;

        for (int i = 0; i < spec.pellets; i++) {
            Vec3 dir = homingTarget != null
                    ? homingTarget.position().add(0, homingTarget.getBbHeight() * 0.6, 0).subtract(eye).normalize()
                    : spread(look, spec.spread, p);
            Vec3 end = eye.add(dir.scale(spec.range));
            BlockHitResult bh = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 stop = bh.getType() == HitResult.Type.MISS ? end : bh.getLocation();

            List<Map.Entry<LivingEntity, Vec3>> along = new ArrayList<>();
            for (Entity e : level.getEntities(p, new AABB(eye, stop).inflate(1.0),
                    e -> e instanceof LivingEntity && e.isAlive() && e.isPickable() && !e.isSpectator())) {
                Optional<Vec3> h = e.getBoundingBox().inflate(0.25).clip(eye, stop);
                h.ifPresent(v -> along.add(Map.entry((LivingEntity) e, v)));
            }
            along.sort(Comparator.comparingDouble(en -> en.getValue().distanceToSqr(eye)));
            for (Map.Entry<LivingEntity, Vec3> en : along) {
                hits.merge(en.getKey(), spec.damage * dmgMult, Float::sum);
                if (!spec.pierce) { stop = en.getValue(); break; }
            }
            Network.fx(p, FxPacket.TRACER, stop.x, stop.y, stop.z, spec.ordinal(), homingTarget != null ? 1f : 0f);
            if (bh.getType() != HitResult.Type.MISS && along.isEmpty())
                level.sendParticles(ParticleTypes.CRIT, stop.x, stop.y, stop.z, 3, 0.05, 0.05, 0.05, 0.1);
        }
        for (Map.Entry<LivingEntity, Float> h : hits.entrySet()) {
            LivingEntity t = h.getKey();
            t.invulnerableTime = 0;
            t.hurt(p.damageSources().playerAttack(p), h.getValue());
            if (spec == GunSpec.SHOTGUN) t.knockback(0.6, -look.x, -look.z);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 2, 0.2, 0.2, 0.2, 0);
        }
        switch (spec) {
            case SMART_PISTOL -> level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.9f, 1.4f);
            case SMG -> level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.6f, 1.9f);
            case SHOTGUN -> level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.8f);
            case TECH_RIFLE -> {
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7f, 2.0f);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 2.0f);
            }
        }
    }

    private static Vec3 spread(Vec3 look, float deg, Player p) {
        if (deg <= 0) return look;
        double r = Math.toRadians(deg);
        return look.add(p.getRandom().nextGaussian() * r * 0.5, p.getRandom().nextGaussian() * r * 0.5, p.getRandom().nextGaussian() * r * 0.5).normalize();
    }

    private LivingEntity findHomingTarget(Player p, Vec3 eye, Vec3 look) {
        LivingEntity best = null;
        double bestDot = Math.cos(Math.toRadians(26));
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(spec.range),
                e -> e != p && e.isAlive() && !e.isSpectator() && !(e instanceof Player pl && pl.isCreative()))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.6, 0).subtract(eye);
            if (to.length() > spec.range) continue;
            double dot = to.normalize().dot(look);
            if (dot > bestDot && p.hasLineOfSight(e)) { bestDot = dot; best = e; }
        }
        return best;
    }

    // ------------------------------------------------------------ client
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.edgerunners." + spec.id + ".desc").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.edgerunners.gun_stats", spec.damage * spec.pellets, spec.mag, String.format("%.1f", 20f / spec.cooldown))
                .withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.edgerunners.gun_controls").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProcess, float swingProcess) {
                float r = FxManager.recoil(partialTick);
                long until = reloadUntil(stack);
                if (until > 0 && player.level().getGameTime() < until) {
                    float k = Mth.sin((until - player.level().getGameTime() - partialTick) / spec.reload * Mth.PI);
                    pose.translate(0, -0.18 * k, 0.05 * k);
                    pose.mulPose(Axis.XP.rotationDegrees(-25 * k));
                }
                pose.translate(0, 0.02 * r, 0.12 * r);
                pose.mulPose(Axis.XP.rotationDegrees(7 * r));
                return false;
            }
        });
    }
}
