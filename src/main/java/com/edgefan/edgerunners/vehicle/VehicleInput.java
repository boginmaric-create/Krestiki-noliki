package com.edgefan.edgerunners.vehicle;

/** Driver input, written by the client each tick (plain data so the common entity code can read it). */
public final class VehicleInput {
    public static float forward, strafe;
    public static boolean handbrake, boost;

    private VehicleInput() {}

    public static void clear() { forward = 0; strafe = 0; handbrake = false; boost = false; }
}
