package com.edgefan.edgerunners.hero;

import com.edgefan.edgerunners.cyber.ChromeBonuses;
import com.edgefan.edgerunners.cyber.CyberData;
import com.edgefan.edgerunners.network.FxPacket;
import com.edgefan.edgerunners.network.Network;
import com.edgefan.edgerunners.network.SyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Personality chip: slot it in to become the hero (full model, stats, ability); use again to eject it. */
public class PersonaChipItem extends Item {
    public final HeroSpec hero;

    public PersonaChipItem(HeroSpec hero, Properties props) {
        super(props.stacksTo(1));
        this.hero = hero;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer p) {
            boolean eject = HeroSpec.of(p) == hero;
            CyberData.set(p, CyberData.HERO, eject ? 0 : hero.ordinal() + 1);
            CyberData.set(p, CyberData.HERO_CD, 0);
            if (!eject) {
                CyberData.addPsychosis(p, 8);
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
            }
            ChromeBonuses.sync(p);
            Network.toPlayer(p, SyncPacket.of(p));
            Network.fx(p, FxPacket.TRANSFORM, p.getX(), p.getY(), p.getZ(), hero.ordinal(), eject ? 1f : 0f);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, eject ? 0.7f : 1.4f);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1f, 0.8f);
            p.displayClientMessage(Component.translatable(eject ? "msg.edgerunners.hero_off" : "msg.edgerunners.hero_on",
                    Component.translatable("hero.edgerunners." + hero.id)).withStyle(eject ? ChatFormatting.GRAY : ChatFormatting.LIGHT_PURPLE), true);
        }
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("hero.edgerunners." + hero.id + ".desc").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("hero.edgerunners." + hero.id + ".ability").withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.translatable("tooltip.edgerunners.persona_use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
