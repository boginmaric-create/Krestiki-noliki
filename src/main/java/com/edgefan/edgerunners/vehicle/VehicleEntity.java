package com.edgefan.edgerunners.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Drivable car / bike. Like boats, the driver's client simulates the movement and the server follows;
 * with nobody driving, the server simulates (coasting to a stop, gravity).
 */
public class VehicleEntity extends Entity {
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HURT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);

    public final VehicleSpec spec;

    // driving state (on whichever side simulates)
    private float speed;
    private Vec3 motion = Vec3.ZERO;
    private float yawDelta;
    public float boost = 100f;
    public boolean boosting, drifting;

    // client visuals
    public float wheelAngle, wheelAngleO, steerVis, steerVisO, lean, leanO, throttleVis, brakeVis, visSpeed;
    private int crashCooldown;

    // interpolation for clients that don't drive
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot;

    public VehicleEntity(EntityType<? extends VehicleEntity> type, Level level, VehicleSpec spec) {
        super(type, level);
        this.spec = spec;
        this.blocksBuilding = true;
        setMaxUpStep(1.1f);
        entityData.set(HEALTH, spec.health);
        entityData.set(COLOR, spec.defaultColor.getId());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(HEALTH, 50f);
        entityData.define(COLOR, 0);
        entityData.define(HURT, 0);
    }

    public float health() { return entityData.get(HEALTH); }

    public DyeColor color() { return DyeColor.byId(entityData.get(COLOR)); }

    public void setColor(DyeColor c) { entityData.set(COLOR, c.getId()); }

    public float speed() { return speed; }

    /** Ticks of hit-shake left (synced). */
    public int hurtTime() { return entityData.get(HURT); }

    // ------------------------------------------------------------ persistence
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Health")) entityData.set(HEALTH, tag.getFloat("Health"));
        if (tag.contains("Color")) entityData.set(COLOR, tag.getInt("Color"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", health());
        tag.putInt("Color", entityData.get(COLOR));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }

    // ------------------------------------------------------------ interaction
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof DyeItem dye) {
            if (!level().isClientSide) {
                setColor(dye.getDyeColor());
                if (!player.getAbilities().instabuild) held.shrink(1);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 1f, 1f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive()) {
            if (!getPassengers().isEmpty()) return InteractionResult.PASS;
            if (!level().isClientSide) {
                ItemStack item = VehicleItem.stackFor(this);
                if (!player.getInventory().add(item)) spawnAtLocation(item);
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide && canAddPassenger(player)) {
            boolean ok = player.startRiding(this);
            if (ok && getPassengers().size() == 1)
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8f, 1.6f);
            return ok ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide || isRemoved()) return !level().isClientSide;
        boolean creative = source.getEntity() instanceof Player p && p.getAbilities().instabuild;
        float hp = creative ? 0 : health() - amount;
        entityData.set(HEALTH, hp);
        markHurt();
        entityData.set(HURT, 10);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 0.6f, 1.4f);
        if (hp <= 0) {
            ejectPassengers();
            if (!creative) spawnAtLocation(VehicleItem.stackFor(this));
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 3, 0.6, 0.3, 0.6, 0);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 20, 0.8, 0.4, 0.8, 0.02);
            }
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 0.8f, 1.2f);
            discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() { return !isRemoved(); }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canCollideWith(Entity other) {
        return (other.canBeCollidedWith() || other.isPushable()) && !isPassengerOfSameVehicle(other);
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) { return false; }

    @Override
    public ItemStack getPickResult() { return VehicleItem.stackFor(this); }

    // ------------------------------------------------------------ passengers
    @Override
    protected boolean canAddPassenger(Entity passenger) { return getPassengers().size() < spec.seats.length; }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        if (!hasPassenger(passenger)) return;
        int i = getPassengers().indexOf(passenger);
        Vec3 seat = spec.seats[Math.min(i, spec.seats.length - 1)];
        Vec3 off = new Vec3(seat.x / 16.0, 0, seat.z / 16.0).yRot(-getYRot() * Mth.DEG_TO_RAD);
        move.accept(passenger, getX() + off.x, getY() + seat.y + passenger.getMyRidingOffset(), getZ() + off.z);
        passenger.setYRot(passenger.getYRot() + yawDelta);
        passenger.setYHeadRot(passenger.getYHeadRot() + yawDelta);
        clampRotation(passenger);
    }

    @Override
    public void onPassengerTurned(Entity passenger) { clampRotation(passenger); }

    private void clampRotation(Entity passenger) {
        passenger.setYBodyRot(getYRot());
        float rel = Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float clamped = Mth.clamp(rel, -120f, 120f);
        passenger.yRotO += clamped - rel;
        passenger.setYRot(passenger.getYRot() + clamped - rel);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 side = new Vec3(spec.width / 2 + 0.6, 0, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
        for (Vec3 c : new Vec3[]{position().add(side), position().subtract(side), position().add(side.yRot(Mth.HALF_PI).scale(1.6))}) {
            Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), BlockPos.containing(c), true);
            if (safe != null) return safe;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // ------------------------------------------------------------ ticking
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x; lerpY = y; lerpZ = z; lerpYRot = yRot;
        lerpSteps = 6;
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            double x = getX() + (lerpX - getX()) / lerpSteps;
            double y = getY() + (lerpY - getY()) / lerpSteps;
            double z = getZ() + (lerpZ - getZ()) / lerpSteps;
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
            lerpSteps--;
            setPos(x, y, z);
            setRot(getYRot(), getXRot());
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickLerp();
        yawDelta = 0;
        if (isControlledByLocalInstance()) {
            boolean localDriver = level().isClientSide && getControllingPassenger() != null;
            drive(localDriver);
            move(MoverType.SELF, getDeltaMovement());
            if (horizontalCollision && Math.abs(speed) > 0.45f && crashCooldown == 0) {
                crashCooldown = 15;
                level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.5f, 0.6f, false);
                for (int i = 0; i < 12; i++)
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5, getZ(), random.nextGaussian() * 0.3, 0.2, random.nextGaussian() * 0.3);
            }
            if (horizontalCollision) { speed *= 0.25f; motion = motion.scale(0.25); }
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (crashCooldown > 0) crashCooldown--;
        if (!level().isClientSide && hurtTime() > 0) entityData.set(HURT, hurtTime() - 1);
        if (level().isClientSide) clientVisuals();
        else ram();
        checkInsideBlocks();
    }

    private void drive(boolean localDriver) {
        float fwd = localDriver ? VehicleInput.forward : 0f;
        float steer = localDriver ? VehicleInput.strafe : 0f;
        boolean hand = localDriver && VehicleInput.handbrake;
        boosting = localDriver && VehicleInput.boost && fwd > 0 && boost > 1f && onGround();
        if (boosting) boost -= 1.6f; else boost = Math.min(100f, boost + 0.35f);
        if (isInWater()) speed *= 0.85f;

        float max = spec.maxSpeed * (boosting ? 1.45f : 1f);
        if (onGround()) {
            if (fwd > 0) speed += spec.accel * (boosting ? 2.2f : 1f) * (1f - Math.max(0, speed) / max * 0.6f);
            else if (fwd < 0) speed -= speed > 0.02f ? spec.brake : spec.accel * 0.6f;
            else speed *= 0.985f;
            if (hand) speed *= 0.955f;
        } else speed *= 0.998f;
        speed = Mth.clamp(speed, -spec.maxSpeed * 0.35f, max);
        if (Math.abs(speed) < 0.002f && fwd == 0) speed = 0;

        float grip = onGround() ? (hand && Math.abs(speed) > 0.35f ? 0.045f : spec.grip) : 0.02f;
        float rate = spec.turn * Mth.clamp(Math.abs(speed) / 0.25f, 0f, 1f) * Math.signum(speed) * (hand ? 1.5f : 1f) * (onGround() ? 1f : 0.3f);
        yawDelta = -steer * rate;
        setYRot(getYRot() + yawDelta);

        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 heading = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 target = heading.scale(speed);
        motion = motion.add(target.subtract(motion).scale(grip));
        drifting = onGround() && Math.abs(speed) > 0.3f && motion.lengthSqr() > 0.01 && motion.normalize().dot(heading) < 0.97;

        double vy = getDeltaMovement().y;
        vy = onGround() ? -0.04 : Math.max(vy - 0.08, -3.0) * 0.98;
        setDeltaMovement(motion.x, vy, motion.z);

        if (localDriver) {
            throttleVis += ((fwd > 0 ? 1f : 0f) - throttleVis) * 0.3f;
            brakeVis += ((fwd < 0 && speed > 0.02f) || hand ? 1f - brakeVis : -brakeVis) * 0.4f;
        }
    }

    private void clientVisuals() {
        double dx = getX() - xo, dz = getZ() - zo;
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float fwd = (float) (-Mth.sin(yaw) * dx + Mth.cos(yaw) * dz);
        visSpeed = fwd;
        wheelAngleO = wheelAngle;
        wheelAngle += fwd / spec.wheelRadius * Mth.RAD_TO_DEG;
        if (wheelAngle > 3600f) { wheelAngle -= 3600f; wheelAngleO -= 3600f; }
        float turn = Mth.wrapDegrees(getYRot() - yRotO);
        boolean localDriver = getControllingPassenger() != null && getControllingPassenger().isControlledByLocalInstance()
                && getControllingPassenger() instanceof Player p && p.isLocalPlayer();
        steerVisO = steerVis;
        float steerTarget = localDriver ? VehicleInput.strafe * 26f : Mth.clamp(-turn * 6f * Math.signum(fwd == 0 ? 1 : fwd), -26f, 26f);
        steerVis += (steerTarget - steerVis) * 0.35f;
        leanO = lean;
        lean += (Mth.clamp(turn * 5.5f * Math.min(1f, Math.abs(fwd) * 3f), -38f, 38f) - lean) * 0.2f;
        if (!localDriver) {
            throttleVis += ((Math.abs(fwd) > 0.05f ? 1f : 0f) - throttleVis) * 0.2f;
            brakeVis *= 0.8f;
        }

        // exhaust flames while boosting, tyre smoke while drifting
        Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw));
        if (boosting) {
            Vec3 p = position().add(back.scale(spec.bike ? 1.0 : 2.1)).add(0, spec.bike ? 0.35 : 0.3, 0);
            for (int i = 0; i < 3; i++)
                level().addParticle(i == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, p.x, p.y, p.z,
                        back.x * 0.2 + random.nextGaussian() * 0.02, 0.01, back.z * 0.2 + random.nextGaussian() * 0.02);
        }
        if (drifting && tickCount % 2 == 0) {
            Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw)).scale(spec.bike ? 0.1 : 0.8);
            Vec3 rear = position().add(back.scale(spec.bike ? 0.85 : 1.25));
            for (Vec3 w : new Vec3[]{rear.add(side), rear.subtract(side)})
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, w.x, w.y + 0.1, w.z, 0, 0.02, 0);
        }
    }

    /** Server: running into mobs at speed hurts them. */
    private void ram() {
        double sp = Math.sqrt((getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo));
        if (sp < 0.35 || !(getControllingPassenger() instanceof Player driver)) return;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.25),
                e -> e.isAlive() && !hasPassenger(e) && !(e.getVehicle() instanceof VehicleEntity))) {
            if (e.hurt(damageSources().mobAttack(driver), (float) (sp * 14))) {
                Vec3 push = e.position().subtract(position()).normalize().scale(sp * 1.2);
                e.push(push.x, 0.35 + sp * 0.2, push.z);
                level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 1f, 0.7f);
            }
        }
    }

    public void honk() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.NEUTRAL, 2.5f, spec.bike ? 1.2f : 0.75f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.NEUTRAL, 2.5f, spec.bike ? 1.5f : 0.95f);
    }
}
