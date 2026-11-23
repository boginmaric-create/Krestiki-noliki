package com.edgefan.edgerunners.cyber;

import com.edgefan.edgerunners.item.ModItems;
import com.edgefan.edgerunners.network.Network;
import com.edgefan.edgerunners.network.SyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/** The actual effect of a ripperdoc operation, applied when the chair finishes. */
public final class RipperdocOps {
    private RipperdocOps() {}

    /** Why this implant can't go in right now, or null if it can. */
    public static Component blockReason(Cyberware c, int mask) {
        if ((mask & c.bit()) != 0) return Component.translatable("gui.edgerunners.already");
        Cyberware occupying = c.slot.installed(mask);
        if (occupying != null)
            return Component.translatable("gui.edgerunners.slot_taken", Component.translatable("item.edgerunners." + occupying.id));
        return null;
    }

    public static boolean install(ServerPlayer p, Cyberware c) {
        Component reason = blockReason(c, CyberData.implants(p));
        if (reason != null) {
            p.displayClientMessage(reason.copy().withStyle(ChatFormatting.RED), true);
            return false;
        }
        CyberData.install(p, c);
        CyberData.addPsychosis(p, c.load);
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, false));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1f, 0.7f);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.6f);
        p.displayClientMessage(Component.translatable("msg.edgerunners.installed",
                Component.translatable("item.edgerunners." + c.id)).withStyle(ChatFormatting.YELLOW), true);
        Network.toPlayer(p, SyncPacket.of(p));
        ChromeBonuses.sync(p);
        return true;
    }

    public static boolean remove(ServerPlayer p, Cyberware c) {
        if (!CyberData.has(p, c)) return false;
        CyberData.uninstall(p, c);
        if (c == Cyberware.SANDEVISTAN) CyberData.set(p, CyberData.SANDE, 0);
        p.getInventory().placeItemBackInInventory(new ItemStack(ModItems.IMPLANTS.get(c).get()));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1f, 0.8f);
        p.displayClientMessage(Component.translatable("msg.edgerunners.removed",
                Component.translatable("item.edgerunners." + c.id)).withStyle(ChatFormatting.YELLOW), true);
        Network.toPlayer(p, SyncPacket.of(p));
        ChromeBonuses.sync(p);
        return true;
    }
}
