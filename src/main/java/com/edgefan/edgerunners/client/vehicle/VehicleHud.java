package com.edgefan.edgerunners.client.vehicle;

import com.edgefan.edgerunners.vehicle.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Neon speedometer, boost and integrity bars while driving. */
public final class VehicleHud {
    private VehicleHud() {}

    public static final IGuiOverlay HUD = (gui, g, partialTick, w, h) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof VehicleEntity v)) return;
        Font font = mc.font;
        boolean driver = v.getControllingPassenger() == mc.player;
        int kmh = Math.round(Math.abs(driver ? v.speed() : v.visSpeed) * 20f * 3.6f);
        int x = w - 112, y = h - 62;
        g.fill(x - 4, y - 4, w - 6, h - 8, 0x90000000);
        g.fill(x - 4, y - 4, w - 6, y - 3, 0xFFFCEE0A);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2.4f, 2.4f, 1f);
        g.drawString(font, String.format("%03d", kmh), 0, 0, v.boosting ? 0xFFFF2A6D : 0xFF00F0FF, true);
        g.pose().popPose();
        g.drawString(font, Component.translatable("hud.edgerunners.kmh"), x + 58, y + 10, 0xFF8A8F98, false);
        if (driver) {
            g.drawString(font, Component.translatable("hud.edgerunners.boost"), x, y + 26, 0xFF8A8F98, false);
            g.fill(x + 34, y + 27, x + 100, y + 32, 0xFF1A1A22);
            g.fill(x + 34, y + 27, x + 34 + (int) (66 * v.boost / 100f), y + 32, v.boosting ? 0xFFFF2A6D : 0xFFFCEE0A);
        }
        g.drawString(font, Component.translatable("hud.edgerunners.hull"), x, y + 38, 0xFF8A8F98, false);
        float hp = Math.max(0, v.health() / v.spec.health);
        g.fill(x + 34, y + 39, x + 100, y + 44, 0xFF1A1A22);
        g.fill(x + 34, y + 39, x + 34 + (int) (66 * hp), y + 44, hp < 0.3f ? 0xFFFF2A6D : 0xFF39FF88);
    };
}
