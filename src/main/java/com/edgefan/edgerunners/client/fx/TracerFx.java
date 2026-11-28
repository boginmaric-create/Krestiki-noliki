package com.edgefan.edgerunners.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Bullet tracer from the shooter's muzzle to the impact point, with a muzzle flash; smart rounds curve. */
public class TracerFx extends Fx {
    private final int shooter;
    private final Vec3 end;
    private final float r, g, b;
    private final boolean curved;

    public TracerFx(int shooter, Vec3 end, int rgb, boolean curved) {
        super(curved ? 6 : 4);
        this.shooter = shooter;
        this.end = end;
        this.r = ((rgb >> 16) & 255) / 255f; this.g = ((rgb >> 8) & 255) / 255f; this.b = (rgb & 255) / 255f;
        this.curved = curved;
    }

    private Vec3 muzzle(Entity e, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Vec3[] bs = basis(e.getViewYRot(partial), e.getViewXRot(partial));
        boolean fp = e == mc.player && mc.options.getCameraType().isFirstPerson();
        return e.getEyePosition(partial).add(bs[0].scale(fp ? 0.7 : 0.9)).add(bs[1].scale(fp ? 0.22 : 0.3)).add(bs[2].scale(fp ? -0.13 : -0.22));
    }

    @Override
    public void render(Matrix4f m, VertexConsumer vc, Vec3 cam, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(shooter);
        if (e == null) return;
        Vec3 start = muzzle(e, partial);
        float t = t(partial);
        float alpha = 1f - t;
        Vec3[] pts;
        if (curved) {
            Vec3 ctrl = start.add(e.getViewVector(partial).scale(start.distanceTo(end) * 0.5));
            pts = new Vec3[14];
            for (int i = 0; i < pts.length; i++) {
                double s = i / (double) (pts.length - 1);
                pts[i] = start.scale((1 - s) * (1 - s)).add(ctrl.scale(2 * s * (1 - s))).add(end.scale(s * s));
            }
        } else {
            // a short bright slug racing ahead plus a fading trail
            Vec3 head = start.add(end.subtract(start).scale(Math.min(1.0, t * 3.0 + 0.4)));
            pts = new Vec3[]{start, head};
        }
        ribbon(vc, m, pts, cam, 0.07f, r, g, b, alpha * 0.5f, false);
        ribbon(vc, m, pts, cam, 0.018f, 1f, 1f, 1f, alpha, false);
        if (age < 2) {
            Vec3 f = e.getViewVector(partial);
            Vec3 up = new Vec3(0, 1, 0), side = f.cross(up).normalize();
            float s = 0.18f;
            quad(vc, m, start.add(side.scale(s)), start.add(up.scale(s)), start.subtract(side.scale(s)), start.subtract(up.scale(s)), 1f, 0.85f, 0.4f, 0.9f, 0.9f);
            quad(vc, m, start, start.add(f.scale(0.35)).add(side.scale(0.06)), start.add(f.scale(0.45)), start.add(f.scale(0.35)).subtract(side.scale(0.06)), 1f, 0.9f, 0.5f, 0.9f, 0.4f);
        }
    }
}
