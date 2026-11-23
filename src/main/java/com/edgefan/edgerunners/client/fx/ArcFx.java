package com.edgefan.edgerunners.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Crescent slash trail: a bright core inside a coloured glow, revealed quickly then fading. */
public class ArcFx extends Fx {
    private final Vec3 center, u, v;
    private final float radius, width, r, g, b;

    public ArcFx(Vec3 center, float yaw, float pitch, float tiltDeg, float radius, float width, int rgb, int life) {
        super(life);
        Vec3[] bs = basis(yaw, pitch);
        double tilt = Math.toRadians(tiltDeg);
        // the slash plane is perpendicular to the attack direction, rotated by the tilt
        this.u = bs[1].scale(Math.cos(tilt)).add(bs[2].scale(Math.sin(tilt)));
        this.v = bs[2].scale(Math.cos(tilt)).subtract(bs[1].scale(Math.sin(tilt))).add(bs[0].scale(0.35)).normalize();
        this.center = center;
        this.radius = radius;
        this.width = width;
        this.r = ((rgb >> 16) & 255) / 255f; this.g = ((rgb >> 8) & 255) / 255f; this.b = (rgb & 255) / 255f;
    }

    @Override
    public void render(Matrix4f m, VertexConsumer vc, Vec3 cam, float partial) {
        float t = t(partial);
        float reveal = Math.min(1f, t * 3.5f);
        float fade = t < 0.35f ? 1f : 1f - (t - 0.35f) / 0.65f;
        int seg = 28;
        double start = Math.toRadians(-110), span = Math.toRadians(220) * reveal;
        for (int i = 0; i < seg; i++) {
            double a0 = start + span * i / seg, a1 = start + span * (i + 1) / seg;
            float k0 = (float) Math.sin(Math.PI * i / seg), k1 = (float) Math.sin(Math.PI * (i + 1) / seg);
            Vec3 d0 = u.scale(Math.sin(a0)).add(v.scale(Math.cos(a0)));
            Vec3 d1 = u.scale(Math.sin(a1)).add(v.scale(Math.cos(a1)));
            float w0 = width * k0, w1 = width * k1;
            // glow
            quad(vc, m, center.add(d0.scale(radius - w0)), center.add(d1.scale(radius - w1)),
                    center.add(d1.scale(radius + w1 * 0.4)), center.add(d0.scale(radius + w0 * 0.4)), r, g, b, 0f, 0.75f * fade);
            // hot white core
            quad(vc, m, center.add(d0.scale(radius - w0 * 0.15)), center.add(d1.scale(radius - w1 * 0.15)),
                    center.add(d1.scale(radius + w1 * 0.15)), center.add(d0.scale(radius + w0 * 0.15)), 1f, 1f, 1f, 0.9f * fade, 0.9f * fade);
        }
    }
}
