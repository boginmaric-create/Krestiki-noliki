package com.edgefan.edgerunners.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Monowire lash: a glowing wire that whips across in an arc, with motion-blurred echoes. */
public class WhipFx extends Fx {
    private final int entityId;
    private final float yaw, pitch;
    private final Vec3 fallback;

    public WhipFx(int entityId, Vec3 fallback, float yaw, float pitch) {
        super(9);
        this.entityId = entityId;
        this.fallback = fallback;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public void render(Matrix4f m, VertexConsumer vc, Vec3 cam, float partial) {
        Vec3[] bs = basis(yaw, pitch);
        Vec3 f = bs[0], r = bs[1], u = bs[2];
        Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(entityId);
        Vec3 eye = e != null ? e.getEyePosition(partial) : fallback;
        Vec3 hand = eye.add(u.scale(-0.4)).add(r.scale(0.35)).add(f.scale(0.3));
        float t = t(partial);
        float fade = t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f;
        float sweep = easeOut(Math.min(1f, t * 1.7f));
        for (int echo = 3; echo >= 0; echo--) {
            float phi = (float) Math.toRadians(-75 + 150 * sweep - echo * 11 * (1 - t));
            float lag = (1f - sweep) * 0.6f;
            Vec3 tipDir = f.scale(Math.cos(phi)).add(r.scale(Math.sin(phi)));
            Vec3 midDir = f.scale(Math.cos(phi - lag)).add(r.scale(Math.sin(phi - lag)));
            Vec3 tip = hand.add(tipDir.scale(7.5)).add(u.scale(-0.4 * sweep));
            Vec3 mid = hand.add(midDir.scale(4.0)).add(u.scale(0.7 * (1 - sweep)));
            Vec3[] pts = new Vec3[18];
            for (int i = 0; i < pts.length; i++) {
                double s = i / (double) (pts.length - 1);
                pts[i] = hand.scale((1 - s) * (1 - s)).add(mid.scale(2 * s * (1 - s))).add(tip.scale(s * s));
            }
            float a = fade * (echo == 0 ? 1f : 0.35f / echo);
            ribbon(vc, m, pts, cam, 0.14f, 1f, 0.16f, 0.43f, a * 0.45f, true);   // glow
            if (echo == 0) ribbon(vc, m, pts, cam, 0.035f, 1f, 0.75f, 0.85f, a, false); // core
        }
    }
}
