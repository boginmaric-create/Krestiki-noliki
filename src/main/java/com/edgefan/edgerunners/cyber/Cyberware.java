package com.edgefan.edgerunners.cyber;

/** Installable cyberware. "load" is how much it strains the user's humanity. */
public enum Cyberware {
    SANDEVISTAN("sandevistan", 30, CyberSlot.OPERATING_SYSTEM),
    MONOWIRE("monowire", 20, CyberSlot.ARMS),
    KIROSHI_OPTICS("kiroshi_optics", 10, CyberSlot.EYES),
    REINFORCED_TENDONS("reinforced_tendons", 10, CyberSlot.LEGS),
    GORILLA_ARMS("gorilla_arms", 15, CyberSlot.ARMS),
    // External chrome: visible, animated full-body sets (see assets/edgerunners/chrome/*.json)
    STREET_SAMURAI("street_samurai", 25, CyberSlot.CHROME),
    NETRUNNER_RIG("netrunner_rig", 20, CyberSlot.CHROME),
    HEAVY_BORG("heavy_borg", 35, CyberSlot.CHROME),
    MERC_ARMS("merc_arms", 25, CyberSlot.CHROME);

    public final String id;
    public final int load;
    public final CyberSlot slot;

    Cyberware(String id, int load, CyberSlot slot) {
        this.id = id;
        this.load = load;
        this.slot = slot;
    }

    public int bit() {
        return 1 << ordinal();
    }
}
