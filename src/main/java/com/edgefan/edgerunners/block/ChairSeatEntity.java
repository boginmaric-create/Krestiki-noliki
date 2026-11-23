package com.edgefan.edgerunners.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Invisible seat that holds the patient in the ripperdoc chair during an operation. */
public class ChairSeatEntity extends Entity {
    /** While true the patient can't get up (server side). */
    public boolean locked;

    public ChairSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getPassengers().isEmpty() && tickCount > 5) discard();
    }

    @Override
    public double getPassengersRidingOffset() { return 0.0; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
}
