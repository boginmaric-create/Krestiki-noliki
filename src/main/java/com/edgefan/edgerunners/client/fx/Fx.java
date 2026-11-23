package com.edgefan.edgerunners.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** A short-lived cosmetic effect drawn with additive "neon" quads in world space. */
public abstract class Fx {
    protected final int life;
    protected int age;

    protected Fx(int life) { this.life = life; }

    public boolean tick() { return ++age < life; }

    /** 0..1 progress, smooth between ticks. */
    protected float t(float partial) { return Math.min(1f, (age + partial) / life); }

    public abstract void render(Matrix4f m, VertexConsumer vc, Vec3 cam, float partial);

    // ------------------------------------------------------------ helpers
    /** Double-sided quad (render types with back-face culling still show it). */
    protected static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                               float r, float g, float bl, float alphaAB, float alphaCD) {
        v(vc, m, a, r, g, bl, alphaAB); v(vc, m, b, r, g, bl, alphaAB); v(vc, m, c, r, g, bl, alphaCD); v(vc, m, d, r, g, bl, alphaCD);
        v(vc, m, d, r, g, bl, alphaCD); v(vc, m, c, r, g, bl, alphaCD); v(vc, m, b, r, g, bl, alphaAB); v(vc, m, a, r, g, bl, alphaAB);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, Math.max(0f, Math.min(1f, a))).endVertex();
    }

    /** Camera-facing ribbon along a polyline. */
    protected static void ribbon(VertexConsumer vc, Matrix4f m, Vec3[] pts, Vec3 cam, float width,
                                 float r, float g, float b, float alpha, boolean taper) {
        for (int i = 0; i < pts.length - 1; i++) {
            Vec3 p0 = pts[i], p1 = pts[i + 1];
            Vec3 dir = p1.subtract(p0);
            Vec3 side0 = dir.cross(cam.subtract(p0)).normalize();
            Vec3 side1 = dir.cross(cam.subtract(p1)).normalize();
            float w0 = taper ? width * (1f - (float) i / (pts.length - 1) * 0.8f) : width;
            float w1 = taper ? width * (1f - (float) (i + 1) / (pts.length - 1) * 0.8f) : width;
            quad(vc, m, p0.add(side0.scale(w0)), p0.subtract(side0.scale(w0)), p1.subtract(side1.scale(w1)), p1.add(side1.scale(w1)),
                    r, g, b, alpha, alpha);
        }
    }

    protected static float easeOut(float x) { return 1f - (1f - x) * (1f - x) * (1f - x); }

    /** Orthonormal basis from yaw/pitch (degrees): [forward, right, up]. */
    protected static Vec3[] basis(float yaw, float pitch) {
        Vec3 f = Vec3.directionFromRotation(pitch, yaw);
        Vec3 r = f.cross(new Vec3(0, 1, 0));
        if (r.lengthSqr() < 1e-4) r = new Vec3(1, 0, 0);
        r = r.normalize();
        Vec3 u = r.cross(f).normalize();
        return new Vec3[]{f, r, u};
    }
}
