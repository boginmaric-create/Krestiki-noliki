package com.edgefan.edgerunners.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/** Deep cuts from mantis blades: damage every second and a trail of blood. */
public class BleedingEffect extends MobEffect {
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55f, 0.02f, 0.04f), 1.1f);

    public BleedingEffect() { super(MobEffectCategory.HARMFUL, 0x8A0303); }

    @Override
    public void applyEffectTick(LivingEntity e, int amplifier) {
        if (e.level() instanceof ServerLevel sl) {
            e.hurt(e.damageSources().magic(), 1.0f + amplifier);
            sl.sendParticles(BLOOD, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 6, e.getBbWidth() * 0.3, e.getBbHeight() * 0.25, e.getBbWidth() * 0.3, 0);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 20 == 0; }
}
