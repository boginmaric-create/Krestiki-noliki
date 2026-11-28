package com.edgefan.edgerunners.weapon;

/** Gun stats. Damage is per pellet, cooldown and reload in ticks, spread in degrees, recoil = camera kick in degrees. */
public enum GunSpec {
    SMART_PISTOL("smart_pistol", 12, 6f, 1, 1.2f, 5, false, 48, 30, true, false, 0x00F0FF, 1.6f),
    SMG("smg", 32, 3.5f, 1, 3.5f, 2, true, 40, 40, false, false, 0xFCEE0A, 0.9f),
    SHOTGUN("shotgun", 6, 2.6f, 9, 8f, 16, false, 24, 50, false, false, 0xFF8A20, 4.5f),
    TECH_RIFLE("tech_rifle", 5, 18f, 1, 0.2f, 26, false, 96, 60, false, true, 0x60E0FF, 3.5f);

    public final String id;
    public final int mag, pellets, cooldown, reload, color;
    public final float damage, spread, range, recoil;
    public final boolean auto, homing, pierce;

    GunSpec(String id, int mag, float damage, int pellets, float spread, int cooldown, boolean auto, float range, int reload,
            boolean homing, boolean pierce, int color, float recoil) {
        this.id = id; this.mag = mag; this.damage = damage; this.pellets = pellets; this.spread = spread; this.cooldown = cooldown;
        this.auto = auto; this.range = range; this.reload = reload; this.homing = homing; this.pierce = pierce; this.color = color; this.recoil = recoil;
    }
}
