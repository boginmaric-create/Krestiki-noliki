package com.edgefan.edgerunners.item;

import com.edgefan.edgerunners.cyber.Cyberware;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CyberwareItem extends Item {
    public final Cyberware cyberware;

    public CyberwareItem(Cyberware cyberware, Properties props) {
        super(props);
        this.cyberware = cyberware;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide)
            player.displayClientMessage(Component.translatable("msg.edgerunners.need_ripperdoc").withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.edgerunners." + cyberware.id + ".desc").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.edgerunners.slot", Component.translatable("gui.edgerunners.slot." + cyberware.slot.id)).withStyle(ChatFormatting.YELLOW));
        tip.add(Component.translatable("tooltip.edgerunners.load", cyberware.load).withStyle(ChatFormatting.RED));
        tip.add(Component.translatable("tooltip.edgerunners.install").withStyle(ChatFormatting.DARK_GRAY));
    }
}
