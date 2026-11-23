package com.edgefan.edgerunners.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Expanding neon shockwave ring. With a vertical normal it also draws a short fading wall. */
public class RingFx extends Fx {
    private final Vec3 center, normal, a, b;
    private final float maxRadius, thickness, r, g, bl, wall;

    public RingFx(Vec3 center, Vec3 normal, float maxRadius, float thickness, int rgb, int life, float wall) {
        super(life);
        this.center = center;
        this.normal = normal.normalize();
        Vec3 ref = Math.abs(this.normal.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        this.a = this.normal.cross(ref).normalize();
        this.b = this.normal.cross(a).normalize();
        this.maxRadius = maxRadius;
        this.thickness = thickness;
        this.r = ((rgb >> 16) & 255) / 255f; this.g = ((rgb >> 8) & 255) / 255f; this.bl = (rgb & 255) / 255f;
        this.wall = wall;
    }

    @Override
    public void render(Matrix4f m, VertexConsumer vc, Vec3 cam, float partial) {
        float t = t(partial);
        float rad = maxRadius * easeOut(t);
        float alpha = (1f - t) * 0.9f;
        float inner = Math.max(0, rad - thickness * (1f - t * 0.5f));
        int seg = Math.max(24, (int) (rad * 6));
        for (int i = 0; i < seg; i++) {
            double t0 = Math.PI * 2 * i / seg, t1 = Math.PI * 2 * (i + 1) / seg;
            Vec3 d0 = a.scale(Math.cos(t0)).add(b.scale(Math.sin(t0)));
            Vec3 d1 = a.scale(Math.cos(t1)).add(b.scale(Math.sin(t1)));
            quad(vc, m, center.add(d0.scale(inner)), center.add(d1.scale(inner)), center.add(d1.scale(rad)), center.add(d0.scale(rad)),
                    r, g, bl, 0f, alpha);
            if (wall > 0) {
                Vec3 up = normal.scale(wall * (1f - t));
                quad(vc, m, center.add(d0.scale(rad)), center.add(d1.scale(rad)), center.add(d1.scale(rad)).add(up), center.add(d0.scale(rad)).add(up),
                        r, g, bl, alpha, 0f);
            }
        }
    }
}
