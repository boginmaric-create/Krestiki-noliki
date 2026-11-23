package com.edgefan.edgerunners.menu;

import com.edgefan.edgerunners.block.ModBlocks;
import com.edgefan.edgerunners.cyber.CyberData;
import com.edgefan.edgerunners.cyber.CyberSlot;
import com.edgefan.edgerunners.cyber.Cyberware;
import com.edgefan.edgerunners.item.CyberwareItem;
import com.edgefan.edgerunners.item.ModItems;
import com.edgefan.edgerunners.network.Network;
import com.edgefan.edgerunners.network.SyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Ripperdoc operation screen. Slot 0 holds the implant to install; button 0 installs it,
 * button 1 + slot ordinal removes the implant in that body slot.
 */
public class RipperdocMenu extends AbstractContainerMenu {
    public static final int BUTTON_INSTALL = 0;
    public static final int INPUT_X = 12, INPUT_Y = 22;

    private final SimpleContainer input = new SimpleContainer(1);
    private final ContainerLevelAccess access;

    public RipperdocMenu(int id, Inventory inv) { this(id, inv, ContainerLevelAccess.NULL); }

    public RipperdocMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenus.RIPPERDOC.get(), id);
        this.access = access;
        addSlot(new Slot(input, 0, INPUT_X, INPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof CyberwareItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 140 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 198));
    }

    public ItemStack inputStack() { return input.getItem(0); }

    /** Why the implant in the input slot can't be installed, or null if it can. Works on both sides. */
    public static Component blockReason(ItemStack stack, int mask) {
        if (!(stack.getItem() instanceof CyberwareItem ci)) return null;
        Cyberware c = ci.cyberware;
        if ((mask & c.bit()) != 0) return Component.translatable("gui.edgerunners.already");
        Cyberware occupying = c.slot.installed(mask);
        if (occupying != null)
            return Component.translatable("gui.edgerunners.slot_taken", Component.translatable("item.edgerunners." + occupying.id));
        return null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return true;
        if (id == BUTTON_INSTALL) return install(sp);
        int slotIndex = id - 1;
        if (slotIndex < 0 || slotIndex >= CyberSlot.values().length) return false;
        return remove(sp, CyberSlot.values()[slotIndex]);
    }

    private boolean install(ServerPlayer p) {
        ItemStack stack = input.getItem(0);
        if (!(stack.getItem() instanceof CyberwareItem ci)) return false;
        Component reason = blockReason(stack, CyberData.implants(p));
        if (reason != null) {
            p.displayClientMessage(reason.copy().withStyle(ChatFormatting.RED), true);
            return false;
        }
        Cyberware c = ci.cyberware;
        CyberData.install(p, c);
        CyberData.addPsychosis(p, c.load);
        stack.shrink(1);
        input.setChanged();
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, false));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1f, 0.7f);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.6f);
        p.displayClientMessage(Component.translatable("msg.edgerunners.installed",
                Component.translatable("item.edgerunners." + c.id)).withStyle(ChatFormatting.YELLOW), true);
        Network.toPlayer(p, SyncPacket.of(p));
        return true;
    }

    private boolean remove(ServerPlayer p, CyberSlot slot) {
        Cyberware c = slot.installed(CyberData.implants(p));
        if (c == null) return false;
        CyberData.uninstall(p, c);
        if (c == Cyberware.SANDEVISTAN) CyberData.set(p, CyberData.SANDE, 0);
        p.getInventory().placeItemBackInInventory(new ItemStack(ModItems.IMPLANTS.get(c).get()));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1f, 0.8f);
        p.displayClientMessage(Component.translatable("msg.edgerunners.removed",
                Component.translatable("item.edgerunners." + c.id)).withStyle(ChatFormatting.YELLOW), true);
        Network.toPlayer(p, SyncPacket.of(p));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof CyberwareItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.RIPPERDOC_CHAIR.get());
    }
}
