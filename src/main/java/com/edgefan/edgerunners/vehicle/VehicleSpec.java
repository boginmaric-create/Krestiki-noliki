package com.edgefan.edgerunners.vehicle;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

/** Handling and layout of each vehicle. Speeds are blocks/tick, angles degrees/tick, seats in model pixels (x = left, z = forward). */
public enum VehicleSpec {
    SPEEDSTER("speedster", 2.6f, 1.2f, 1.30f, 0.024f, 0.065f, 4.2f, 0.22f, 60f, 5.5f / 16f, false, DyeColor.YELLOW,
            new Vec3(5.5, 0.17, -4.5), new Vec3(-5.5, 0.17, -4.5)),
    CYBER_BIKE("cyber_bike", 1.2f, 1.2f, 1.50f, 0.032f, 0.075f, 5.6f, 0.34f, 35f, 6f / 16f, true, DyeColor.MAGENTA,
            new Vec3(0, 0.30, -5.0));

    public final String id;
    public final float width, height, maxSpeed, accel, brake, turn, grip, health, wheelRadius;
    public final boolean bike;
    public final DyeColor defaultColor;
    public final Vec3[] seats;

    VehicleSpec(String id, float width, float height, float maxSpeed, float accel, float brake, float turn, float grip,
                float health, float wheelRadius, boolean bike, DyeColor defaultColor, Vec3... seats) {
        this.id = id; this.width = width; this.height = height; this.maxSpeed = maxSpeed; this.accel = accel; this.brake = brake;
        this.turn = turn; this.grip = grip; this.health = health; this.wheelRadius = wheelRadius; this.bike = bike;
        this.defaultColor = defaultColor; this.seats = seats;
    }
}
