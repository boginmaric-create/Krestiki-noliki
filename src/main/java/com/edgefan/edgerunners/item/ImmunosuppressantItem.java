package com.edgefan.edgerunners.item;

import com.edgefan.edgerunners.cyber.CyberData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

public class ImmunosuppressantItem extends Item {
    public ImmunosuppressantItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            float base = CyberData.baseline(player);
            CyberData.setPsychosis(player, Math.max(base, CyberData.psychosis(player) - 35f));
            player.removeEffect(MobEffects.CONFUSION);
            player.removeEffect(MobEffects.DARKNESS);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1f, 1.8f);
            player.displayClientMessage(Component.translatable("msg.edgerunners.immuno").withStyle(ChatFormatting.GREEN), true);
        }
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.edgerunners.immunosuppressant.desc").withStyle(ChatFormatting.AQUA));
    }
}
