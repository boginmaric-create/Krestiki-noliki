package com.edgefan.edgerunners.weapon;

import com.edgefan.edgerunners.network.FxPacket;
import com.edgefan.edgerunners.network.Network;
import com.edgefan.edgerunners.vehicle.ModVehicles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Micro-missile from the projectile launch system: homes in on the nearest target ahead, explodes without breaking blocks. */
public class MissileEntity extends Projectile {
    private static final double SPEED = 1.5;
    private int life;

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) { super(type, level); }

    public static MissileEntity launch(LivingEntity owner, Vec3 pos, Vec3 dir) {
        MissileEntity m = new MissileEntity(ModVehicles.MISSILE.get(), owner.level());
        m.setOwner(owner);
        m.setPos(pos.x, pos.y, pos.z);
        m.setDeltaMovement(dir.normalize().scale(SPEED));
        m.faceMotion();
        owner.level().addFreshEntity(m);
        return m;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    private void faceMotion() {
        Vec3 v = getDeltaMovement();
        setYRot((float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    @Override
    public void tick() {
        super.tick();
        life++;
        Vec3 v = getDeltaMovement();
        if (!level().isClientSide && life > 4) {
            LivingEntity target = null;
            double best = Math.cos(Math.toRadians(40));
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(28),
                    e -> e.isAlive() && e != getOwner() && !e.isSpectator())) {
                Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(position());
                double dot = to.normalize().dot(v.normalize());
                if (dot > best) { best = dot; target = e; }
            }
            if (target != null) {
                Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position()).normalize();
                v = v.normalize().lerp(to, 0.14).normalize().scale(SPEED);
                setDeltaMovement(v);
            }
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !level().isClientSide) {
            explode(hit.getLocation());
            return;
        }
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        yRotO = getYRot(); xRotO = getXRot();
        faceMotion();
        if (level().isClientSide) {
            Vec3 back = position().subtract(v.normalize().scale(0.35));
            level().addParticle(ParticleTypes.FLAME, back.x, back.y, back.z, 0, 0, 0);
            level().addParticle(ParticleTypes.SMOKE, back.x, back.y, back.z, 0, 0.01, 0);
        } else if (life > 80 || isInWater()) {
            explode(position());
        }
    }

    private void explode(Vec3 at) {
        Entity owner = getOwner();
        level().explode(this, at.x, at.y, at.z, 2.2f, Level.ExplosionInteraction.NONE);
        Network.fx(owner != null ? owner : this, FxPacket.BLAST, at.x, at.y, at.z, 0, 0);
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
}
