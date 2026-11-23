package com.edgefan.edgerunners.client.operation;

import com.edgefan.edgerunners.block.RipperdocChairBlockEntity;
import com.edgefan.edgerunners.client.fx.FxManager;
import com.edgefan.edgerunners.cyber.Cyberware;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Operation screen: dimmed vignette, ECG trace, stage name and progress; plus the white flash at the end. */
public final class OperationHud {
    private static final String[] STAGES = {"anesthesia", "incision", "integration", "calibration", "closing"};

    private OperationHud() {}

    public static final IGuiOverlay HUD = (gui, g, partialTick, w, h) -> {
        Minecraft mc = Minecraft.getInstance();
        if (FxManager.flashTicks > 0) {
            int a = (int) (Mth.clamp((FxManager.flashTicks - partialTick) / 14f, 0f, 1f) * 0xE0);
            g.fill(0, 0, w, h, (a << 24) | 0xF0FFFF);
        }
        if (!OperationClient.active() || mc.options.hideGui) return;
        RipperdocChairBlockEntity be = OperationClient.current(mc);
        if (be == null) return;
        float p = be.progress(partialTick);
        float age = mc.player.tickCount + partialTick;

        // vignette that breathes with the heartbeat
        int edge = (int) (0x70 + 0x30 * (OperationClient.beatFlash / 5f));
        for (int i = 0; i < 40; i++) {
            int a = (int) (edge * (1f - i / 40f));
            int col = (a << 24) | 0x05000A;
            g.fill(0, i * h / 160, w, i * h / 160 + h / 160 + 1, col);
            g.fill(0, h - i * h / 160 - h / 160 - 1, w, h - i * h / 160, col);
        }

        int pw = 220, ph = 62, x = (w - pw) / 2, y = h - ph - 46;
        g.fill(x, y, x + pw, y + ph, 0xC0060812);
        g.fill(x, y, x + pw, y + 1, 0xFFFCEE0A);
        g.fill(x, y + ph - 1, x + pw, y + ph, 0xFFFCEE0A);
        Cyberware c = be.cyberware();
        Component what = c == null ? Component.empty() : Component.translatable("item.edgerunners." + c.id);
        String key = be.mode() == RipperdocChairBlockEntity.INSTALL ? "hud.edgerunners.op_install" : "hud.edgerunners.op_remove";
        g.drawString(mc.font, Component.translatable(key, what), x + 6, y + 5, 0xFFFCEE0A, false);
        int stage = Math.min(STAGES.length - 1, (int) (p * STAGES.length));
        g.drawString(mc.font, Component.translatable("hud.edgerunners.op_stage." + STAGES[stage]), x + 6, y + 16, 0xFF8A8F98, false);

        // ECG trace
        int ex = x + 6, ey = y + 34, ew = pw - 12;
        float scroll = age * 3.2f;
        int prev = ey;
        for (int i = 0; i < ew; i++) {
            float t = ((i + scroll) % 60f) / 60f;
            float v = t < 0.42f ? 0 : t < 0.46f ? -3 : t < 0.5f ? 11 : t < 0.54f ? -6 : t < 0.62f ? 0 : t < 0.72f ? 2.5f * Mth.sin((t - 0.62f) / 0.1f * Mth.PI) : 0;
            int yy = ey - (int) v;
            int fade = (int) (0x40 + 0xBF * (i / (float) ew));
            g.fill(ex + i, Math.min(prev, yy), ex + i + 1, Math.max(prev, yy) + 1, (fade << 24) | 0x39FF88);
            prev = yy;
        }

        // progress
        int by = y + ph - 12;
        g.fill(x + 6, by, x + pw - 40, by + 5, 0xFF1A1A22);
        g.fill(x + 6, by, x + 6 + (int) ((pw - 46) * p), by + 5, 0xFF00F0FF);
        g.drawString(mc.font, (int) (p * 100) + "%", x + pw - 32, by - 2, 0xFF00F0FF, false);
    };
}
