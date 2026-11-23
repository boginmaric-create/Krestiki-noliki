package com.edgefan.edgerunners.network;

import com.edgefan.edgerunners.cyber.Cyberware;

import java.util.HashMap;
import java.util.Map;

/** Client-side mirror of the local player's cyberware state (plain data, safe on any side). */
public final class ClientCyberData {
    public static int implants, sande, sandeCd, monoCd, scanCd, berserk;
    public static float psychosis;

    /** Cyberware of every player this client can see: entity id -> implant mask (+ SANDE_ACTIVE_BIT). */
    public static final Map<Integer, Integer> VISIBLE = new HashMap<>();
    public static final int SANDE_ACTIVE_BIT = 1 << 30;

    private ClientCyberData() {}

    static void apply(SyncPacket p) {
        implants = p.implants(); psychosis = p.psychosis(); sande = p.sande(); sandeCd = p.sandeCd();
        monoCd = p.monoCd(); scanCd = p.scanCd(); berserk = p.berserk();
    }

    public static boolean has(Cyberware c) { return (implants & c.bit()) != 0; }
}
