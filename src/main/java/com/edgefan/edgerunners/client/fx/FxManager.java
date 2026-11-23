package com.edgefan.edgerunners.client.fx;

import com.edgefan.edgerunners.network.ClientCyberData;
import com.edgefan.edgerunners.network.FxPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Client-side effects: world FX, camera shake, screen shader, HUD flashes. */
public final class FxManager {
    private static final List<Fx> ACTIVE = new ArrayList<>();
    private static final RandomSource RNG = RandomSource.create();
    private static final ResourceLocation ABERRATION = new ResourceLocation("shaders/post/deconverge.json");

    public static int shakeTicks, shakeMax;
    public static float shakeStrength;
    public static int scanTicks;
    public static int flashTicks;
    public static final int SCAN_DURATION = 24;

    private FxManager() {}

    public static void spawn(FxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 at = new Vec3(p.x(), p.y(), p.z());
        boolean self = mc.player != null && mc.player.getId() == p.entityId();
        switch (p.type()) {
            case FxPacket.SANDE_START -> {
                add(new RingFx(at.add(0, 0.05, 0), new Vec3(0, 1, 0), 7f, 0.6f, 0x39FF88, 14, 0.8f));
                add(new RingFx(at.add(0, 1.0, 0), new Vec3(0, 1, 0), 3.5f, 0.3f, 0xFCEE0A, 10, 0f));
                if (self) shake(5, 1.5f);
            }
            case FxPacket.MONOWIRE -> add(new WhipFx(p.entityId(), at, p.yaw(), p.pitch()));
            case FxPacket.SLASH -> {
                float tilt = RNG.nextFloat() * 120f - 60f;
                add(new ArcFx(at, p.yaw(), p.pitch(), tilt, 1.15f, 0.32f, 0xFF2A6D, 7));
                add(new ArcFx(at, p.yaw(), p.pitch(), tilt + 8f, 0.85f, 0.18f, 0xFCEE0A, 6));
            }
            case FxPacket.LUNGE -> {
                add(new ArcFx(at, p.yaw(), p.pitch(), 0f, 1.6f, 0.45f, 0x00F0FF, 8));
                add(new RingFx(at, Vec3.directionFromRotation(p.pitch(), p.yaw()), 1.8f, 0.35f, 0xFF2A6D, 7, 0f));
            }
            case FxPacket.PUNCH -> {
                Vec3 dir = Vec3.directionFromRotation(p.pitch(), p.yaw());
                add(new RingFx(at, dir, 2.2f, 0.5f, 0xFFFFFF, 6, 0f));
                add(new RingFx(at, dir, 1.4f, 0.4f, 0x00F0FF, 8, 0f));
                add(new RingFx(at.add(0, -0.9, 0), new Vec3(0, 1, 0), 2.5f, 0.4f, 0xFCEE0A, 9, 0.3f));
                if (self) shake(7, 3.5f);
            }
            case FxPacket.SCAN -> {
                add(new RingFx(at.add(0, 0.1, 0), new Vec3(0, 1, 0), 40f, 1.4f, 0x00F0FF, 26, 2.5f));
                add(new RingFx(at.add(0, 1.6, 0), new Vec3(0, 1, 0), 40f, 0.6f, 0xFF1030, 30, 0f));
                if (self) scanTicks = SCAN_DURATION;
            }
            case FxPacket.DOUBLE_JUMP -> add(new RingFx(at.add(0, 0.05, 0), new Vec3(0, 1, 0), 1.8f, 0.35f, 0x00F0FF, 8, 0.25f));
            case FxPacket.BERSERK -> {
                add(new RingFx(at.add(0, 0.05, 0), new Vec3(0, 1, 0), 10f, 0.8f, 0xFF1030, 20, 1.5f));
                if (self) shake(30, 2.5f);
            }
            default -> {}
        }
    }

    private static void add(Fx fx) {
        if (ACTIVE.size() < 64) ACTIVE.add(fx);
    }

    public static void flash(int ticks) { flashTicks = Math.max(flashTicks, ticks); }

    public static void shake(int ticks, float strength) {
        shakeTicks = Math.max(shakeTicks, ticks);
        shakeMax = Math.max(shakeTicks, 1);
        shakeStrength = Math.max(shakeStrength * (shakeTicks > 0 ? 1 : 0), strength);
    }

    public static void tick(Minecraft mc) {
        ACTIVE.removeIf(fx -> !fx.tick());
        Ghosts.tick(mc);
        if (shakeTicks > 0 && --shakeTicks == 0) shakeStrength = 0;
        if (scanTicks > 0) scanTicks--;
        if (flashTicks > 0) flashTicks--;
        updateShader(mc);
    }

    public static void clear() {
        ACTIVE.clear();
        Ghosts.clear();
        shakeTicks = 0;
        scanTicks = 0;
    }

    /** Chromatic aberration while the Sandevistan runs or during cyberpsychosis. */
    private static void updateShader(Minecraft mc) {
        GameRenderer gr = mc.gameRenderer;
        boolean want = mc.player != null && (ClientCyberData.sande > 0 || ClientCyberData.berserk > 0);
        PostChain cur = gr.currentEffect();
        boolean ours = cur != null && cur.getName().equals(ABERRATION.toString());
        if (want && cur == null) gr.loadEffect(ABERRATION);
        else if (!want && ours) gr.shutdownEffect();
    }

    public static float shakeOffset(float partial) {
        if (shakeTicks <= 0) return 0;
        float k = (shakeTicks - partial) / shakeMax;
        return (RNG.nextFloat() - 0.5f) * 2f * shakeStrength * Math.max(0, k);
    }

    public static void render(PoseStack pose, Vec3 cam, float partial) {
        Ghosts.render(pose, cam, partial);
        if (ACTIVE.isEmpty()) return;
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        for (Fx fx : ACTIVE) fx.render(m, vc, cam, partial);
        pose.popPose();
        buffers.endBatch(RenderType.lightning());
    }
}
