package com.edgefan.edgerunners.client.weapon;

import com.edgefan.edgerunners.weapon.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Ammo counter (magazine / reserve) and reload bar while a gun is in hand. */
public final class GunHud {
    private GunHud() {}

    public static final IGuiOverlay HUD = (gui, g, partialTick, w, h) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        ItemStack s = mc.player.getMainHandItem();
        if (!(s.getItem() instanceof GunItem gun)) return;
        int ammo = GunItem.ammo(s), reserve = GunItem.countAmmo(mc.player);
        int x = w / 2 + 96, y = h - 38;
        g.fill(x - 3, y - 3, x + 86, y + 22, 0x90000000);
        g.fill(x - 3, y - 3, x - 2, y + 22, 0xFFFCEE0A);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(1.8f, 1.8f, 1f);
        int col = ammo == 0 ? 0xFFFF2A6D : ammo <= gun.spec.mag / 4 ? 0xFFFCEE0A : 0xFF00F0FF;
        g.drawString(mc.font, String.valueOf(ammo), 0, 0, col, true);
        g.pose().popPose();
        g.drawString(mc.font, "/ " + gun.spec.mag + "  +" + (mc.player.getAbilities().instabuild ? "∞" : String.valueOf(reserve)), x + 26, y + 4, 0xFF8A8F98, false);
        long until = GunItem.reloadUntil(s);
        long now = mc.player.level().getGameTime();
        if (until > now) {
            float p = 1f - Mth.clamp((until - now - partialTick) / gun.spec.reload, 0f, 1f);
            g.drawString(mc.font, Component.translatable("hud.edgerunners.reloading"), x, y + 13, 0xFFFCEE0A, false);
            g.fill(x + 50, y + 15, x + 82, y + 19, 0xFF1A1A22);
            g.fill(x + 50, y + 15, x + 50 + (int) (32 * p), y + 19, 0xFFFCEE0A);
        }
    };
}
