package com.edgefan.edgerunners.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Quickhack data stream: a jittering zig-zag beam from the hacker's eyes to the target. */
public class BeamFx extends Fx {
    private final int source;
    private final Vec3 end;

    public BeamFx(int source, Vec3 end) {
        super(14);
        this.source = source;
        this.end = end;
    }

    @Override
    public void render(Matrix4f m, VertexConsumer vc, Vec3 cam, float partial) {
        Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(source);
        if (e == null) return;
        Vec3 start = e.getEyePosition(partial).add(0, -0.1, 0);
        float t = t(partial);
        Vec3[] pts = new Vec3[16];
        long seed = (age / 2) * 31L;
        for (int i = 0; i < pts.length; i++) {
            double s = i / (double) (pts.length - 1);
            double j = (i == 0 || i == pts.length - 1) ? 0 : 0.25;
            double jx = Math.sin(seed + i * 12.9898) * j, jy = Math.cos(seed + i * 78.233) * j, jz = Math.sin(seed * 0.7 + i * 4.1) * j;
            pts[i] = start.add(end.subtract(start).scale(s)).add(jx, jy, jz);
        }
        float a = 1f - t;
        ribbon(vc, m, pts, cam, 0.1f, 0.7f, 0.25f, 1f, a * 0.45f, false);
        ribbon(vc, m, pts, cam, 0.025f, 0.85f, 0.95f, 1f, a, false);
    }
}
