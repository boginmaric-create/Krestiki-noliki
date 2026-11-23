package com.edgefan.edgerunners.client.vehicle;

import com.edgefan.edgerunners.vehicle.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** Electric-motor hum whose pitch and volume follow the vehicle's speed. */
public class VehicleEngineSound extends AbstractTickableSoundInstance {
    private final VehicleEntity vehicle;

    public VehicleEngineSound(VehicleEntity vehicle) {
        super(SoundEvents.BEACON_AMBIENT, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.4f;
        this.pitch = 0.6f;
        this.x = vehicle.getX(); this.y = vehicle.getY(); this.z = vehicle.getZ();
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (vehicle.isRemoved() || mc.player == null || mc.player.getVehicle() != vehicle) {
            stop();
            return;
        }
        float s = Math.abs(vehicle.visSpeed);
        x = vehicle.getX(); y = vehicle.getY(); z = vehicle.getZ();
        volume = Mth.clamp(0.35f + s * 0.9f, 0f, 1f);
        pitch = Mth.clamp(0.55f + s * 1.25f + (vehicle.boosting ? 0.35f : 0f), 0.5f, 2.0f);
    }

    public boolean isFor(VehicleEntity v) { return vehicle == v; }
}
