package com.edgefan.edgerunners.cyber;

/** Body slots a ripperdoc can fill. Each slot holds one implant. */
public enum CyberSlot {
    OPERATING_SYSTEM("os"), EYES("eyes"), ARMS("arms"), LEGS("legs"), CHROME("chrome");

    public final String id;

    CyberSlot(String id) { this.id = id; }

    /** First implant in this slot for the given implant mask, or null. */
    public Cyberware installed(int mask) {
        for (Cyberware c : Cyberware.values()) if (c.slot == this && (mask & c.bit()) != 0) return c;
        return null;
    }
}
