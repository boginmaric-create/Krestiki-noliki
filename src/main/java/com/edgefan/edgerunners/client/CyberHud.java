package com.edgefan.edgerunners.client;

import com.edgefan.edgerunners.cyber.CyberData;
import com.edgefan.edgerunners.cyber.Cyberware;
import com.edgefan.edgerunners.network.ClientCyberData;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class CyberHud {
    private static final int YELLOW = 0xFFFCEE0A;
    private static final int CYAN = 0xFF00F0FF;
    private static final int PINK = 0xFFFF2A6D;
    private static final int GREEN = 0xFF39FF88;
    private static final int GRAY = 0xFF8A8F98;

    private CyberHud() {}

    public static final IGuiOverlay HUD = (gui, g, partialTick, w, h) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        float psy = ClientCyberData.psychosis;
        if (ClientCyberData.implants == 0 && psy <= 0.5f) return;
        long t = mc.player.tickCount;

        // Sandevistan screen tint + scanlines.
        if (ClientCyberData.sande > 0) {
            int a = (int) (0x30 + 0x10 * Mth.sin(t * 0.6f));
            g.fill(0, 0, w, h, (a << 24) | 0x33FF99);
            for (int y = (int) (t % 4); y < h; y += 4) g.fill(0, y, w, y + 1, 0x1400FFD0);
        }
        // Psychosis vignette.
        if (psy >= 75 || ClientCyberData.berserk > 0) {
            float k = ClientCyberData.berserk > 0 ? 1f : (psy - 75f) / 25f;
            int a = (int) (Mth.clamp(k, 0f, 1f) * (0x50 + 0x30 * Mth.sin(t * 0.25f)));
            int c = (Math.max(0, a) << 24) | 0xFF0033;
            g.fillGradient(0, 0, w, h / 4, c, 0x00FF0033);
            g.fillGradient(0, h - h / 4, w, h, 0x00FF0033, c);
        }

        Font font = mc.font;
        int x = 6, y = 6;
        g.fill(x - 3, y - 3, x + 122, y + 12 + 10 * countLines() + 8, 0x90000000);
        g.fill(x - 3, y - 3, x - 1, y + 12 + 10 * countLines() + 8, YELLOW);
        g.drawString(font, Component.translatable("hud.edgerunners.title"), x + 2, y, YELLOW, true);
        y += 11;

        // Psychosis bar.
        int barW = 110;
        g.fill(x + 2, y, x + 2 + barW, y + 5, 0xFF1A1A22);
        int fillW = (int) (barW * psy / 100f);
        int col = psy >= 75 ? PINK : psy >= 50 ? YELLOW : CYAN;
        g.fill(x + 2, y, x + 2 + fillW, y + 5, col);
        y += 7;
        g.drawString(font, Component.translatable(ClientCyberData.berserk > 0 ? "hud.edgerunners.berserk" : "hud.edgerunners.psychosis", (int) psy),
                x + 2, y, ClientCyberData.berserk > 0 ? PINK : 0xFFDDDDDD, true);
        y += 10;

        if (ClientCyberData.has(Cyberware.SANDEVISTAN)) {
            line(g, font, x, y, KeyBinds.SANDEVISTAN, "item.edgerunners.sandevistan",
                    ClientCyberData.sande > 0 ? -ClientCyberData.sande : ClientCyberData.sandeCd);
            y += 10;
        }
        if (ClientCyberData.has(Cyberware.MONOWIRE)) {
            line(g, font, x, y, KeyBinds.MONOWIRE, "item.edgerunners.monowire", ClientCyberData.monoCd);
            y += 10;
        }
        if (ClientCyberData.has(Cyberware.KIROSHI_OPTICS)) {
            line(g, font, x, y, KeyBinds.SCAN, "item.edgerunners.kiroshi_optics", ClientCyberData.scanCd);
        }
    };

    private static int countLines() {
        int n = 0;
        if (ClientCyberData.has(Cyberware.SANDEVISTAN)) n++;
        if (ClientCyberData.has(Cyberware.MONOWIRE)) n++;
        if (ClientCyberData.has(Cyberware.KIROSHI_OPTICS)) n++;
        return n;
    }

    /** ticks > 0: cooldown, ticks < 0: active (remaining), 0: ready. */
    private static void line(GuiGraphics g, Font font, int x, int y, KeyMapping key, String nameKey, int ticks) {
        Component status;
        int color;
        if (ticks < 0) {
            status = Component.translatable("hud.edgerunners.active", String.format("%.1f", -ticks / 20f));
            color = GREEN;
        } else if (ticks > 0) {
            status = Component.literal(String.format("%.1fs", ticks / 20f));
            color = GRAY;
        } else {
            status = Component.translatable("hud.edgerunners.ready");
            color = CYAN;
        }
        Component text = Component.literal("[").append(key.getTranslatedKeyMessage()).append("] ")
                .append(Component.translatable(nameKey)).append(": ").append(status);
        g.drawString(font, text, x + 2, y, color, true);
    }
}
