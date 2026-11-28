package com.edgefan.edgerunners.cyber;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Per-player cyberware state, stored in the player's persistent NBT. */
public final class CyberData {
    public static final String KEY = "edgerunners";

    public static final String IMPLANTS = "implants";
    public static final String PSYCHOSIS = "psychosis";
    public static final String SANDE = "sande";        // active ticks left
    public static final String SANDE_CD = "sandeCd";
    public static final String MONO_CD = "monoCd";
    public static final String SCAN_CD = "scanCd";
    public static final String BERSERK = "berserk";    // cyberpsychosis episode ticks left
    public static final String PLS_CD = "plsCd";
    public static final String HERO = "hero";          // persona: HeroSpec ordinal + 1, 0 = none
    public static final String HERO_CD = "heroCd";
    public static final String BARRAGE = "barrage";    // gunner's infinite-ammo window

    public static final int SANDE_DURATION = 120;
    public static final int SANDE_COOLDOWN = 600;
    public static final int MONO_COOLDOWN = 30;
    public static final int SCAN_COOLDOWN = 300;
    public static final int BERSERK_DURATION = 400;

    private CyberData() {}

    public static CompoundTag tag(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    public static int implants(Player p) { return tag(p).getInt(IMPLANTS); }

    public static boolean has(Player p, Cyberware c) { return (implants(p) & c.bit()) != 0; }

    public static void install(Player p, Cyberware c) { tag(p).putInt(IMPLANTS, implants(p) | c.bit()); }

    public static void uninstall(Player p, Cyberware c) { tag(p).putInt(IMPLANTS, implants(p) & ~c.bit()); }

    public static void clearImplants(Player p) { tag(p).putInt(IMPLANTS, 0); }

    public static int totalLoad(int mask) {
        int sum = 0;
        for (Cyberware c : Cyberware.values()) if ((mask & c.bit()) != 0) sum += c.load;
        return sum;
    }

    /** Psychosis never decays below half the installed cyberware load. */
    public static float baseline(Player p) {
        float b = totalLoad(implants(p)) * 0.5f;
        if (get(p, HERO) == com.edgefan.edgerunners.hero.HeroSpec.CHROME_TYRANT.ordinal() + 1) b += 20f;
        return Math.min(95f, b);
    }

    public static float psychosis(Player p) { return tag(p).getFloat(PSYCHOSIS); }

    public static void setPsychosis(Player p, float v) { tag(p).putFloat(PSYCHOSIS, Mth.clamp(v, 0f, 100f)); }

    public static void addPsychosis(Player p, float v) { setPsychosis(p, psychosis(p) + v); }

    public static int get(Player p, String key) { return tag(p).getInt(key); }

    public static void set(Player p, String key, int v) { tag(p).putInt(key, v); }
}
