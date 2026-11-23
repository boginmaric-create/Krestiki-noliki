package com.edgefan.edgerunners.client.chrome;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Smoothed per-player animation inputs for external chrome (blades out in combat, vents open while sprinting). */
public final class ChromeState {
    private static final Map<Integer, float[]> STATE = new HashMap<>(); // {deploy, prevDeploy, sprint, prevSprint, lastSwingTick}

    private ChromeState() {}

    public static void tick(Minecraft mc) {
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        for (Player p : mc.level.players()) {
            float[] s = STATE.computeIfAbsent(p.getId(), k -> new float[]{0, 0, 0, 0, -1000});
            if (p.swinging) s[4] = now;
            boolean combat = now - (long) s[4] < 70;
            s[1] = s[0];
            s[0] += ((combat ? 1f : 0f) - s[0]) * (combat ? 0.45f : 0.12f);
            s[3] = s[2];
            s[2] += ((p.isSprinting() ? 1f : 0f) - s[2]) * 0.15f;
        }
        if (mc.level.getGameTime() % 200 == 0) STATE.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    }

    public static void clear() { STATE.clear(); }

    public static float deploy(Player p, float partial) {
        float[] s = STATE.get(p.getId());
        return s == null ? 0f : s[1] + (s[0] - s[1]) * partial;
    }

    public static float sprint(Player p, float partial) {
        float[] s = STATE.get(p.getId());
        return s == null ? 0f : s[3] + (s[2] - s[3]) * partial;
    }
}
