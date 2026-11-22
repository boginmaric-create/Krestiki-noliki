package com.edgefan.edgerunners.cyber;

/** Installable cyberware. "load" is how much it strains the user's humanity. */
public enum Cyberware {
    SANDEVISTAN("sandevistan", 30),
    MONOWIRE("monowire", 20),
    KIROSHI_OPTICS("kiroshi_optics", 10),
    REINFORCED_TENDONS("reinforced_tendons", 10),
    GORILLA_ARMS("gorilla_arms", 15);

    public final String id;
    public final int load;

    Cyberware(String id, int load) {
        this.id = id;
        this.load = load;
    }

    public int bit() {
        return 1 << ordinal();
    }
}
