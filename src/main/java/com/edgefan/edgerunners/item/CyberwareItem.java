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

public class CyberwareItem extends Item {
    public final Cyberware cyberware;

    public CyberwareItem(Cyberware cyberware, Properties props) {
        super(props);
        this.cyberware = cyberware;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (CyberData.has(player, cyberware)) {
                player.displayClientMessage(Component.translatable("msg.edgerunners.already").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            CyberData.install(player, cyberware);
            CyberData.addPsychosis(player, cyberware.load);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1f, 0.7f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.6f);
            player.displayClientMessage(Component.translatable("msg.edgerunners.installed", getDescription()).withStyle(ChatFormatting.YELLOW), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.edgerunners." + cyberware.id + ".desc").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.edgerunners.load", cyberware.load).withStyle(ChatFormatting.RED));
        tip.add(Component.translatable("tooltip.edgerunners.install").withStyle(ChatFormatting.DARK_GRAY));
    }
}
