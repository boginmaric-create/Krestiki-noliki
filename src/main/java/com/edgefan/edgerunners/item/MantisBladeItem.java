package com.edgefan.edgerunners.item;

import com.edgefan.edgerunners.network.FxPacket;
import com.edgefan.edgerunners.network.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Fast, sharp arm blade. Right-click to lunge forward. */
public class MantisBladeItem extends SwordItem {
    public MantisBladeItem(Properties props) {
        super(Tiers.NETHERITE, 4, -2.0f, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 1.7, Math.max(0.3, look.y * 0.9), look.z * 1.7);
        player.hurtMarked = true;
        player.fallDistance = 0;
        player.getCooldowns().addCooldown(this, 40);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.6f);
        if (!level.isClientSide) {
            Vec3 at = player.getEyePosition().add(look.scale(1.2)).add(0, -0.3, 0);
            Network.fx(player, FxPacket.LUNGE, at.x, at.y, at.z, player.getYRot(), player.getXRot());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.edgerunners.mantis_blade.desc").withStyle(ChatFormatting.AQUA));
    }
}
