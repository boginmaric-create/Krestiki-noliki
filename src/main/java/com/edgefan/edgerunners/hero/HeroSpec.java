package com.edgefan.edgerunners.hero;

import com.edgefan.edgerunners.cyber.CyberData;
import net.minecraft.world.entity.player.Player;

/** Hero personas loaded from personality chips. Stored per player as ordinal + 1 (0 = none). */
public enum HeroSpec {
    RUNNER_KID("runner_kid", 1.0f, 200),
    NETRUNNER_GIRL("netrunner_girl", 0.95f, 160),
    GUNNER_GIRL("gunner_girl", 0.88f, 400),
    VETERAN_BORG("veteran_borg", 1.12f, 240),
    CHROME_TYRANT("chrome_tyrant", 1.3f, 320);

    public final String id;
    public final float scale;
    public final int cooldown;

    HeroSpec(String id, float scale, int cooldown) {
        this.id = id;
        this.scale = scale;
        this.cooldown = cooldown;
    }

    public static HeroSpec byStored(int v) { return v <= 0 || v > values().length ? null : values()[v - 1]; }

    public static HeroSpec of(Player p) { return byStored(CyberData.get(p, CyberData.HERO)); }
}
