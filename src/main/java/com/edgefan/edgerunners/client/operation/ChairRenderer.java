package com.edgefan.edgerunners.client.operation;

import com.edgefan.edgerunners.block.RipperdocChairBlockEntity;
import com.edgefan.edgerunners.client.chrome.ChromeModel;
import com.edgefan.edgerunners.client.chrome.ChromeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Animates the chair's surgical arm: idle sway, and the full operation sequence with laser beams. */
public class ChairRenderer implements BlockEntityRenderer<RipperdocChairBlockEntity> {
    private static final float PIVOT_X = 14.75f, PIVOT_Y = 39f, PIVOT_Z = 14.25f;
    private static final float FULL_SWING = 41f, FULL_REACH = 2.9f, FULL_DROP = 2.5f;
    private static final float[][] EMITTERS = {{13.9f, 33.35f, 6.0f, 1f, 0.1f, 0.15f}, {15.6f, 33.35f, 6.0f, 1f, 0.1f, 0.15f}, {14.75f, 33.35f, 8.1f, 0.1f, 0.9f, 1f}};

    /** Arm pose at a moment of the operation. */
    public record Pose(float swing, float reach, float drop, float spin, float laser) {}

    public static Pose pose(RipperdocChairBlockEntity be, float partial, float age) {
        if (!be.busy()) return new Pose(Mth.sin(age * 0.03f) * 3f, 0, 0, 0, 0.12f);
        float p = be.progress(partial);
        float swing, drop;
        if (p < RipperdocChairBlockEntity.SWING_END) { swing = ease(p / RipperdocChairBlockEntity.SWING_END); drop = 0; }
        else if (p < RipperdocChairBlockEntity.DROP_END) { swing = 1; drop = ease((p - RipperdocChairBlockEntity.SWING_END) / (RipperdocChairBlockEntity.DROP_END - RipperdocChairBlockEntity.SWING_END)); }
        else if (p < RipperdocChairBlockEntity.SURGERY_END) { swing = 1; drop = 1; }
        else if (p < RipperdocChairBlockEntity.RISE_END) { swing = 1; drop = 1 - ease((p - RipperdocChairBlockEntity.SURGERY_END) / (RipperdocChairBlockEntity.RISE_END - RipperdocChairBlockEntity.SURGERY_END)); }
        else { swing = 1 - ease((p - RipperdocChairBlockEntity.RISE_END) / (1 - RipperdocChairBlockEntity.RISE_END)); drop = 0; }
        boolean surgery = p > RipperdocChairBlockEntity.DROP_END && p < RipperdocChairBlockEntity.SURGERY_END;
        float laser = surgery ? 0.65f + 0.35f * Mth.sin(age * 2.3f) : 0.12f;
        float wobble = surgery ? Mth.sin(age * 0.7f) * 1.5f : 0f;
        return new Pose(swing * FULL_SWING + wobble, swing * FULL_REACH, drop * FULL_DROP, surgery ? age * 24f : 0f, laser);
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    @Override
    public void render(RipperdocChairBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ChromeModel model = ChromeRenderer.load("machine/chair_arm.json");
        if (model == null || be.getLevel() == null) return;
        float age = be.getLevel().getGameTime() + partial;
        Pose arm = pose(be, partial, age);

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(be.facing().toYRot() - 180f)));
        pose.translate(-0.5, 0, -0.5);
        ChromeRenderer.renderGroups(pose, buffers, model, model.parts.get("root"), new ChromeRenderer.AnimSource() {
            @Override public float signal(String name, float phase) {
                return switch (name) {
                    case "swing" -> arm.swing();
                    case "reach" -> arm.reach();
                    case "drop" -> arm.drop();
                    case "spin" -> arm.spin();
                    case "age" -> age;
                    default -> 0f;
                };
            }

            @Override public float glow(String mode) {
                return switch (mode) {
                    case "laser" -> arm.laser();
                    case "pulse" -> 0.7f + 0.3f * Mth.sin(age * 0.15f);
                    default -> 1f;
                };
            }
        }, light);
        if (arm.laser() > 0.3f) renderLasers(pose, buffers, be, arm, age);
        pose.popPose();
    }

    /** Beams from the three emitters to the body part being operated on. */
    private static void renderLasers(PoseStack pose, MultiBufferSource buffers, RipperdocChairBlockEntity be, Pose arm, float age) {
        Vector3f tg = RipperdocChairBlockEntity.target(be.cyberware());
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        double sw = Math.toRadians(arm.swing());
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (int i = 0; i < EMITTERS.length; i++) {
            float[] e = EMITTERS[i];
            double dx = e[0] - PIVOT_X, dy = e[1] - arm.drop() - PIVOT_Y, dz = e[2] - arm.reach() - PIVOT_Z;
            double rx = dx * Math.cos(sw) + dz * Math.sin(sw), rz = -dx * Math.sin(sw) + dz * Math.cos(sw);
            Vec3 a = new Vec3((PIVOT_X + rx) / 16, (PIVOT_Y + dy) / 16, (PIVOT_Z + rz) / 16);
            float jitter = Mth.sin(age * 3.1f + i * 2f) * 0.35f;
            Vec3 b = new Vec3((tg.x + jitter) / 16, (tg.y + Mth.cos(age * 2.7f + i) * 0.3f) / 16, tg.z / 16);
            // billboard the beam toward the camera, in the chair's local frame
            Vec3 toCam = be.toLocalBlocks(cam).subtract(a);
            Vec3 dir = b.subtract(a);
            Vec3 side = dir.cross(toCam).normalize().scale(0.012 + 0.006 * arm.laser());
            if (side.lengthSqr() < 1e-8) side = new Vec3(0.012, 0, 0);
            float alpha = 0.55f * arm.laser();
            quad(vc, m, a.add(side), a.subtract(side), b.subtract(side), b.add(side), e[3], e[4], e[5], alpha);
            Vec3 core = side.scale(0.35);
            quad(vc, m, a.add(core), a.subtract(core), b.subtract(core), b.add(core), 1f, 1f, 1f, alpha);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float alpha) {
        Vec3[] pts = {a, b, c, d, d, c, b, a};
        for (Vec3 p : pts) vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, bl, alpha).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(RipperdocChairBlockEntity be) { return true; }

    @Override
    public int getViewDistance() { return 48; }
}
