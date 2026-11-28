package com.edgefan.edgerunners.client.vehicle;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.client.chrome.ChromeModel;
import com.edgefan.edgerunners.client.chrome.ChromeRenderer;
import com.edgefan.edgerunners.vehicle.VehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class VehicleRenderer extends EntityRenderer<VehicleEntity> {
    private static final ResourceLocation ATLAS = new ResourceLocation(EdgerunnersMod.MODID, "textures/item/cyberware.png");

    public VehicleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.9f;
    }

    @Override
    public void render(VehicleEntity v, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ChromeModel model = ChromeRenderer.load("vehicle/" + v.spec.id + ".json");
        if (model == null) return;
        float yaw = Mth.rotLerp(partial, v.yRotO, v.getYRot());
        float wheel = Mth.lerp(partial, v.wheelAngleO, v.wheelAngle);
        float steer = Mth.lerp(partial, v.steerVisO, v.steerVis);
        float lean = Mth.lerp(partial, v.leanO, v.lean);
        float age = v.tickCount + partial;
        float[] dye = v.color().getTextureDiffuseColors();
        float[] paint = {Math.min(1f, dye[0] * 1.15f + 0.04f), Math.min(1f, dye[1] * 1.15f + 0.04f), Math.min(1f, dye[2] * 1.15f + 0.04f)};

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        if (v.spec.bike) pose.mulPose(Axis.ZP.rotationDegrees(lean));
        int hurt = v.hurtTime();
        if (hurt > 0) pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin((hurt - partial) * 1.3f) * hurt * 0.35f));
        float throttle = v.throttleVis, brake = v.brakeVis;
        boolean boosting = v.boosting;
        ChromeRenderer.renderGroups(pose, buffers, model, model.parts.get("root"), new ChromeRenderer.AnimSource() {
            @Override public float signal(String name, float phase) {
                return switch (name) {
                    case "wheel" -> wheel;
                    case "steer" -> steer;
                    case "age" -> age;
                    default -> 0f;
                };
            }

            @Override public float glow(String mode) {
                return switch (mode) {
                    case "throttle" -> boosting ? 1f : 0.3f + 0.7f * throttle;
                    case "brake" -> 0.45f + 0.55f * brake;
                    case "pulse" -> 0.75f + 0.25f * Mth.sin(age * 0.12f);
                    default -> 1f;
                };
            }

            @Override public float[] paint() { return paint; }
        }, light);
        renderUnderglow(pose, buffers, v, age);
        pose.popPose();
        super.render(v, entityYaw, partial, pose, buffers, light);
    }

    /** Soft neon light pool on the ground under the vehicle. */
    private static void renderUnderglow(PoseStack pose, MultiBufferSource buffers, VehicleEntity v, float age) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        float hw = v.spec.bike ? 0.35f : 1.0f, hl = v.spec.bike ? 1.1f : 2.0f, y = 0.03f;
        float pulse = 0.8f + 0.2f * Mth.sin(age * 0.12f);
        float r = v.spec.bike ? 0.9f : 0f, g = v.spec.bike ? 0.15f : 0.85f, b = 1f;
        for (int ring = 0; ring < 3; ring++) {
            float grow = 1f + ring * 0.35f, a = (0.35f - ring * 0.1f) * pulse;
            float x = hw * grow, z = hl * (1f + ring * 0.12f);
            vc.vertex(m, -x, y, -z).color(r, g, b, a).endVertex();
            vc.vertex(m, -x, y, z).color(r, g, b, a).endVertex();
            vc.vertex(m, x, y, z).color(r, g, b, a).endVertex();
            vc.vertex(m, x, y, -z).color(r, g, b, a).endVertex();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(VehicleEntity v) { return ATLAS; }
}
