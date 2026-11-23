package com.edgefan.edgerunners.block;

import com.edgefan.edgerunners.cyber.CyberSlot;
import com.edgefan.edgerunners.cyber.Cyberware;
import com.edgefan.edgerunners.cyber.RipperdocOps;
import com.edgefan.edgerunners.vehicle.ModVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Runs a ripperdoc operation: seats the patient, animates the surgical arm (client renderer),
 * then installs or removes the implant when the timer runs out.
 */
public class RipperdocChairBlockEntity extends BlockEntity {
    public static final int NONE = 0, INSTALL = 1, REMOVE = 2;
    public static final int INSTALL_TICKS = 140, REMOVE_TICKS = 80;
    /** Fractions of the operation: arm swings in, lowers, operates, rises, swings back. */
    public static final float SWING_END = 0.14f, DROP_END = 0.24f, SURGERY_END = 0.86f, RISE_END = 0.93f;

    private int mode;
    private int cyber = -1;
    private long start;
    private int total = 1;
    private UUID patient;
    private int seatId = -1;
    private ItemStack implant = ItemStack.EMPTY;

    public RipperdocChairBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CHAIR_BE.get(), pos, state);
    }

    public boolean busy() { return mode != NONE; }

    public int mode() { return mode; }

    public Cyberware cyberware() { return cyber >= 0 && cyber < Cyberware.values().length ? Cyberware.values()[cyber] : null; }

    public float progress(float partial) {
        if (level == null || mode == NONE) return 0f;
        return Mth.clamp((level.getGameTime() - start + partial) / total, 0f, 1f);
    }

    public Direction facing() { return getBlockState().getValue(HorizontalDirectionalBlock.FACING); }

    /** Chair-local pixel coordinates (chair facing north) to world coordinates. */
    public Vec3 local(double x, double y, double z) {
        float theta = -(facing().toYRot() - 180f) * Mth.DEG_TO_RAD;
        double dx = x - 8, dz = z - 8;
        double rx = dx * Math.cos(theta) + dz * Math.sin(theta), rz = -dx * Math.sin(theta) + dz * Math.cos(theta);
        return new Vec3(worldPosition.getX() + 0.5 + rx / 16, worldPosition.getY() + y / 16, worldPosition.getZ() + 0.5 + rz / 16);
    }

    /** World coordinates to chair-local block units (inverse of {@link #local}, divided by 16). */
    public Vec3 toLocalBlocks(Vec3 world) {
        float theta = -(facing().toYRot() - 180f) * Mth.DEG_TO_RAD;
        double wx = (world.x - worldPosition.getX() - 0.5) * 16, wz = (world.z - worldPosition.getZ() - 0.5) * 16;
        double dx = wx * Math.cos(-theta) + wz * Math.sin(-theta), dz = -wx * Math.sin(-theta) + wz * Math.cos(-theta);
        return new Vec3((dx + 8) / 16, world.y - worldPosition.getY(), (dz + 8) / 16);
    }

    /** Where the lasers aim, in chair pixels, depending on the body slot. */
    public static Vector3f target(Cyberware c) {
        if (c == null) return new Vector3f(8, 17, 4.3f);
        return switch (c.slot) {
            case OPERATING_SYSTEM -> new Vector3f(8, 22.5f, 9.5f);
            case EYES -> new Vector3f(8, 25, 2.3f);
            case ARMS -> new Vector3f(14, 16, 6.5f);
            case LEGS -> new Vector3f(6, 9.5f, 0.5f);
            case CHROME -> new Vector3f(8, 17, 4.3f);
        };
    }

    public boolean begin(ServerPlayer p, Cyberware c, int newMode, ItemStack heldImplant) {
        if (busy() || level == null) return false;
        Vec3 seatPos = local(8, 2.6, 6.5);
        ChairSeatEntity seat = new ChairSeatEntity(ModVehicles.CHAIR_SEAT.get(), level);
        seat.moveTo(seatPos.x, seatPos.y, seatPos.z, facing().toYRot(), 0f);
        seat.locked = true;
        level.addFreshEntity(seat);
        p.stopRiding();
        if (!p.startRiding(seat, true)) {
            seat.discard();
            return false;
        }
        p.setYRot(facing().toYRot());
        p.setYHeadRot(facing().toYRot());
        mode = newMode;
        cyber = c.ordinal();
        start = level.getGameTime();
        total = newMode == INSTALL ? INSTALL_TICKS : REMOVE_TICKS;
        patient = p.getUUID();
        seatId = seat.getId();
        implant = heldImplant.copy();
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8f, 0.7f);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.4f);
        sync();
        return true;
    }

    public void serverTick() {
        if (mode == NONE || level == null) return;
        Entity seat = level.getEntity(seatId);
        ServerPlayer p = patient == null ? null : (ServerPlayer) level.getPlayerByUUID(patient);
        if (seat == null || p == null || !p.isAlive() || p.getVehicle() != seat) {
            abort();
            return;
        }
        long t = level.getGameTime() - start;
        float pr = t / (float) total;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false));
        if (t == (long) (total * SWING_END)) level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.7f, 1.3f);
        if (pr > DROP_END && pr < SURGERY_END) {
            if (t % 9 == 0) level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35f, 1.9f);
            if (t % 23 == 5) level.playSound(null, worldPosition, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.6f, 2.0f);
            if (mode == INSTALL && t % 31 == 11) level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6f, 0.6f);
        }
        if (t == (long) (total * SURGERY_END)) level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.7f, 1.4f);
        if (t >= total) complete(p, seat);
    }

    /** Client: sparks and blood where the lasers hit. */
    public void clientTick() {
        if (mode == NONE || level == null) return;
        float pr = progress(0);
        if (pr <= DROP_END || pr >= SURGERY_END) return;
        Vector3f tg = target(cyberware());
        Vec3 at = local(tg.x, tg.y, tg.z);
        var rnd = level.random;
        if (rnd.nextFloat() < 0.7f)
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, rnd.nextGaussian() * 0.08, rnd.nextFloat() * 0.08, rnd.nextGaussian() * 0.08);
        if (mode == INSTALL && rnd.nextFloat() < 0.25f)
            level.addParticle(new DustParticleOptions(new Vector3f(0.6f, 0.02f, 0.05f), 0.8f), at.x, at.y, at.z, 0, -0.02, 0);
        if (rnd.nextFloat() < 0.1f)
            level.addParticle(ParticleTypes.SMOKE, at.x, at.y + 0.05, at.z, 0, 0.02, 0);
    }

    private void complete(ServerPlayer p, Entity seat) {
        Cyberware c = cyberware();
        if (c != null) {
            if (mode == INSTALL) {
                if (!RipperdocOps.install(p, c) && !implant.isEmpty()) p.getInventory().placeItemBackInInventory(implant);
            } else {
                RipperdocOps.remove(p, c);
            }
        }
        implant = ItemStack.EMPTY;
        release(seat);
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.7f, 0.8f);
        reset();
    }

    /** Operation interrupted (patient left, chair broken, world reloaded): give the implant back. */
    public void abort() {
        if (level == null || mode == NONE) return;
        if (!implant.isEmpty()) Block.popResource(level, worldPosition.above(), implant);
        implant = ItemStack.EMPTY;
        release(level.getEntity(seatId));
        reset();
    }

    private void release(Entity seat) {
        if (seat instanceof ChairSeatEntity s) {
            s.locked = false;
            s.ejectPassengers();
            s.discard();
        }
    }

    private void reset() {
        mode = NONE;
        cyber = -1;
        patient = null;
        seatId = -1;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    // ------------------------------------------------------------ persistence & sync
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Mode", mode);
        tag.putInt("Cyber", cyber);
        tag.putLong("Start", start);
        tag.putInt("Total", total);
        if (patient != null) tag.putUUID("Patient", patient);
        if (!implant.isEmpty()) tag.put("Implant", implant.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mode = tag.getInt("Mode");
        cyber = tag.contains("Cyber") ? tag.getInt("Cyber") : -1;
        start = tag.getLong("Start");
        total = Math.max(1, tag.getInt("Total"));
        patient = tag.hasUUID("Patient") ? tag.getUUID("Patient") : null;
        implant = tag.contains("Implant") ? ItemStack.of(tag.getCompound("Implant")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0, 0, 1.0).expandTowards(0, 2.0, 0);
    }
}
