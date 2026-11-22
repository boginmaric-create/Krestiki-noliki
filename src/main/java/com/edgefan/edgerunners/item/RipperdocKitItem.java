package com.edgefan.edgerunners.item;

import com.edgefan.edgerunners.cyber.CyberData;
import com.edgefan.edgerunners.cyber.Cyberware;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Removes all installed cyberware and returns the implants. */
public class RipperdocKitItem extends Item {
    public RipperdocKitItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("msg.edgerunners.ripper_hint").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.pass(stack);
        }
        int mask = CyberData.implants(player);
        if (mask == 0) {
            player.displayClientMessage(Component.translatable("msg.edgerunners.ripper_none").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        for (Cyberware c : Cyberware.values()) {
            if ((mask & c.bit()) == 0) continue;
            ItemStack implant = new ItemStack(ModItems.IMPLANTS.get(c).get());
            if (!player.getInventory().add(implant)) player.drop(implant, false);
        }
        CyberData.clearImplants(player);
        CyberData.set(player, CyberData.SANDE, 0);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1f, 0.8f);
        player.displayClientMessage(Component.translatable("msg.edgerunners.ripper_done").withStyle(ChatFormatting.YELLOW), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.edgerunners.ripperdoc_kit.desc").withStyle(ChatFormatting.AQUA));
    }
}
