package com.edgefan.edgerunners.cyber;

import net.minecraft.ChatFormatting;
import com.edgefan.edgerunners.network.FxPacket;
import com.edgefan.edgerunners.network.Network;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public final class Abilities {
    public static final int SANDEVISTAN = 0;
    public static final int MONOWIRE = 1;
    public static final int SCAN = 2;
    public static final int DOUBLE_JUMP = 3;
    public static final int HORN = 4;

    public static final double SANDE_RADIUS = 24;

    private static final Vector3f[] SANDE_COLORS = {
            new Vector3f(0.2f, 1f, 0.4f), new Vector3f(0.99f, 0.93f, 0.04f),
            new Vector3f(1f, 0.16f, 0.43f), new Vector3f(0f, 0.94f, 1f)
    };

    private Abilities() {}

    public static void handle(ServerPlayer p, int ability) {
        switch (ability) {
            case SANDEVISTAN -> sandevistan(p);
            case MONOWIRE -> monowire(p);
            case SCAN -> scan(p);
            case DOUBLE_JUMP -> doubleJump(p);
            case HORN -> { if (p.getVehicle() instanceof com.edgefan.edgerunners.vehicle.VehicleEntity v) v.honk(); }
            default -> {}
        }
    }

    private static void bar(ServerPlayer p, String key, ChatFormatting color, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    // ---------------------------------------------------------------- Sandevistan
    private static void sandevistan(ServerPlayer p) {
        if (!CyberData.has(p, Cyberware.SANDEVISTAN)) {
            bar(p, "msg.edgerunners.not_installed", ChatFormatting.GRAY, Component.translatable("item.edgerunners.sandevistan"));
            return;
        }
        if (CyberData.get(p, CyberData.SANDE) > 0) return;
        int cd = CyberData.get(p, CyberData.SANDE_CD);
        if (cd > 0) {
            bar(p, "msg.edgerunners.cooldown", ChatFormatting.RED, cd / 20 + 1);
            return;
        }
        CyberData.set(p, CyberData.SANDE, CyberData.SANDE_DURATION);
        CyberData.set(p, CyberData.SANDE_CD, ChromeBonuses.cooldown(p, CyberData.SANDE_COOLDOWN));
        CyberData.addPsychosis(p, 6);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 2f);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1f, 0.6f);
        bar(p, "msg.edgerunners.sande_on", ChatFormatting.GREEN);
        Network.fx(p, FxPacket.SANDE_START, p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
    }

    /** Called every server tick while the Sandevistan is running. */
    public static void tickSandevistan(ServerPlayer p, int ticksLeft) {
        ServerLevel level = p.serverLevel();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 5, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 5, 2, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 5, 1, false, false, true));
        CyberData.addPsychosis(p, 0.12f);

        AABB box = p.getBoundingBox().inflate(SANDE_RADIUS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 6, false, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 5, 3, false, false, false));
        }
        // Bullet time: freeze incoming projectiles in place.
        for (Projectile proj : level.getEntitiesOfClass(Projectile.class, box,
                pr -> pr.getOwner() != p && pr.getDeltaMovement().lengthSqr() > 0.01)) {
            CompoundTag d = proj.getPersistentData();
            if (d.getBoolean("er_slowed")) continue;
            Vec3 m = proj.getDeltaMovement();
            d.putBoolean("er_slowed", true);
            d.putDouble("er_vx", m.x);
            d.putDouble("er_vy", m.y);
            d.putDouble("er_vz", m.z);
            d.putBoolean("er_nograv", proj.isNoGravity());
            proj.setNoGravity(true);
            proj.setDeltaMovement(m.scale(0.04));
            proj.hurtMarked = true;
        }

        // Neon afterimages.
        if (ticksLeft % 2 == 0) {
            Vector3f c = SANDE_COLORS[(ticksLeft / 4) % SANDE_COLORS.length];
            DustParticleOptions dust = new DustParticleOptions(c, 1.6f);
            level.sendParticles(dust, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.25, 0.55, 0.25, 0);
        }
        if (ticksLeft == 1) endSandevistan(p);
    }

    public static void endSandevistan(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AABB box = p.getBoundingBox().inflate(SANDE_RADIUS + 24);
        List<Projectile> list = level.getEntitiesOfClass(Projectile.class, box, pr -> pr.getPersistentData().getBoolean("er_slowed"));
        for (Projectile proj : list) {
            CompoundTag d = proj.getPersistentData();
            proj.setDeltaMovement(d.getDouble("er_vx"), d.getDouble("er_vy"), d.getDouble("er_vz"));
            proj.setNoGravity(d.getBoolean("er_nograv"));
            for (String k : new String[]{"er_slowed", "er_vx", "er_vy", "er_vz", "er_nograv"}) d.remove(k);
            proj.hurtMarked = true;
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 1.8f);
    }

    // ---------------------------------------------------------------- Monowire
    private static void monowire(ServerPlayer p) {
        if (!CyberData.has(p, Cyberware.MONOWIRE)) {
            bar(p, "msg.edgerunners.not_installed", ChatFormatting.GRAY, Component.translatable("item.edgerunners.monowire"));
            return;
        }
        if (CyberData.get(p, CyberData.MONO_CD) > 0) return;
        CyberData.set(p, CyberData.MONO_CD, ChromeBonuses.cooldown(p, CyberData.MONO_COOLDOWN));
        CyberData.addPsychosis(p, 1.5f);

        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double range = 8;
        DustParticleOptions wire = new DustParticleOptions(new Vector3f(1f, 0.16f, 0.43f), 0.7f);

        // The glowing whip itself is drawn client-side.
        Network.fx(p, FxPacket.MONOWIRE, eye.x, eye.y, eye.z, p.getYRot(), p.getXRot());
        int hits = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range), e -> e != p && e.isAlive())) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            if (to.length() > range || to.normalize().dot(look) < 0.55) continue;
            e.hurt(p.damageSources().playerAttack(p), 9f);
            Vec3 pull = to.normalize().scale(-0.45);
            e.setDeltaMovement(e.getDeltaMovement().add(pull.x, 0.15, pull.z));
            e.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 8, 0.2, 0.3, 0.2, 0.2);
            level.sendParticles(wire, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 12, 0.25, 0.4, 0.25, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 10, 0.2, 0.3, 0.2, 0.3);
            hits++;
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.6f);
        if (hits > 0)
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    // ---------------------------------------------------------------- Kiroshi scan
    private static void scan(ServerPlayer p) {
        if (!CyberData.has(p, Cyberware.KIROSHI_OPTICS)) {
            bar(p, "msg.edgerunners.not_installed", ChatFormatting.GRAY, Component.translatable("item.edgerunners.kiroshi_optics"));
            return;
        }
        int cd = CyberData.get(p, CyberData.SCAN_CD);
        if (cd > 0) {
            bar(p, "msg.edgerunners.cooldown", ChatFormatting.RED, cd / 20 + 1);
            return;
        }
        CyberData.set(p, CyberData.SCAN_CD, ChromeBonuses.cooldown(p, CyberData.SCAN_COOLDOWN));
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(40), e -> e != p && e.isAlive());
        for (LivingEntity e : found) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, true));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1f, 1.5f);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 2f);
        bar(p, "msg.edgerunners.scan", ChatFormatting.AQUA, found.size());
        Network.fx(p, FxPacket.SCAN, p.getX(), p.getY(), p.getZ(), 0, 0);
    }

    // ---------------------------------------------------------------- Tendons double jump
    private static void doubleJump(ServerPlayer p) {
        if (!CyberData.has(p, Cyberware.REINFORCED_TENDONS)) return;
        p.fallDistance = 0;
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 10, 0.3, 0.05, 0.3, 0.02);
        Network.fx(p, FxPacket.DOUBLE_JUMP, p.getX(), p.getY(), p.getZ(), 0, 0);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.5f, 1.8f);
    }
}
