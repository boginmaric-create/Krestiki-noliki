package com.edgefan.edgerunners.client;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.cyber.Cyberware;
import com.edgefan.edgerunners.network.ClientCyberData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws installed cyberware on the player model. Coordinates are in model pixels, relative to each body part. */
public class CyberwareLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WHITE = new ResourceLocation(EdgerunnersMod.MODID, "textures/entity/white.png");

    private static final int DARK = 0x1A1A24, STEEL = 0x3C404C, GRAY = 0x5F6473, LIGHT = 0xAFB6C4;
    private static final int YELLOW = 0xFCEE0A, GREEN = 0x39FF88, PINK = 0xFF2A6D, CYAN = 0x00F0FF, RED = 0xFF1030, MUSCLE = 0xA8202A;

    private final boolean slim;

    public CyberwareLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, boolean slim) {
        super(parent);
        this.slim = slim;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        Integer data = ClientCyberData.VISIBLE.get(player.getId());
        if (data == null || (data & ~ClientCyberData.SANDE_ACTIVE_BIT) == 0) return;
        boolean sandeActive = (data & ClientCyberData.SANDE_ACTIVE_BIT) != 0;

        VertexConsumer solid = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        float pulse = 0.75f + 0.25f * Mth.sin(ageInTicks * (sandeActive ? 0.9f : 0.15f));

        // Sandevistan: spine on the back.
        if (has(data, Cyberware.SANDEVISTAN)) {
            Part p = new Part(pose, m.body);
            cube(p, solid, -0.6f, 0.5f, 2.0f, 0.6f, 11.5f, 2.9f, STEEL, light);
            for (int i = 0; i < 5; i++) {
                float y = 0.8f + i * 2.2f;
                cube(p, solid, -2f, y, 2.0f, 2f, y + 1.2f, 3.3f, LIGHT, light);
            }
            p.end();
        }
        // Gorilla arms: forearm plating.
        if (has(data, Cyberware.GORILLA_ARMS)) {
            float w = slim ? 3f : 4f;
            Part r = new Part(pose, m.rightArm);
            cube(r, solid, -3.5f, 3.5f, -2.5f, w - 3f + 0.5f, 10.4f, 2.5f, GRAY, light);
            cube(r, solid, -3.6f, 4.5f, -2.6f, w - 3f + 0.6f, 5.3f, 2.6f, YELLOW, light);
            r.end();
            Part l = new Part(pose, m.leftArm);
            cube(l, solid, -1.5f, 3.5f, -2.5f, w - 1f + 0.5f, 10.4f, 2.5f, GRAY, light);
            cube(l, solid, -1.6f, 4.5f, -2.6f, w - 1f + 0.6f, 5.3f, 2.6f, YELLOW, light);
            l.end();
        }
        // Monowire: cuff on the right wrist.
        if (has(data, Cyberware.MONOWIRE)) {
            float w = slim ? 3f : 4f;
            Part r = new Part(pose, m.rightArm);
            cube(r, solid, -3.4f, 8.6f, -2.4f, w - 3f + 0.4f, 9.6f, 2.4f, DARK, light);
            r.end();
        }
        // Reinforced tendons: knee braces.
        if (has(data, Cyberware.REINFORCED_TENDONS)) {
            for (ModelPart leg : new ModelPart[]{m.rightLeg, m.leftLeg}) {
                Part p = new Part(pose, leg);
                cube(p, solid, -2.4f, 4.5f, -2.4f, 2.4f, 6.5f, 2.4f, LIGHT, light);
                cube(p, solid, -1f, 2f, -2.3f, 1f, 9f, -2.05f, MUSCLE, light);
                p.end();
            }
        }

        // Glowing parts are drawn full-bright in a second pass.
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        int fb = LightTexture.FULL_BRIGHT;
        if (has(data, Cyberware.SANDEVISTAN)) {
            Part p = new Part(pose, m.body);
            for (int i = 0; i < 5; i++) {
                float y = 0.9f + i * 2.2f;
                int c = sandeActive ? (i % 2 == 0 ? GREEN : YELLOW) : GREEN;
                cubeA(p, glow, -2.6f, y, 2.3f, -2f, y + 1f, 3.0f, c, pulse, fb);
                cubeA(p, glow, 2f, y, 2.3f, 2.6f, y + 1f, 3.0f, c, pulse, fb);
            }
            p.end();
        }
        if (has(data, Cyberware.KIROSHI_OPTICS)) {
            Part p = new Part(pose, m.head);
            cubeA(p, glow, -3f, -4f, -4.15f, -1f, -3f, -4.02f, RED, 1f, fb);
            cubeA(p, glow, 1f, -4f, -4.15f, 3f, -3f, -4.02f, RED, 1f, fb);
            p.end();
        }
        if (has(data, Cyberware.GORILLA_ARMS)) {
            float w = slim ? 3f : 4f;
            Part r = new Part(pose, m.rightArm);
            cubeA(r, glow, -3f, 9.6f, -2.65f, w - 3f, 10.2f, -2.5f, CYAN, pulse, fb);
            r.end();
            Part l = new Part(pose, m.leftArm);
            cubeA(l, glow, -1f, 9.6f, -2.65f, w - 1f, 10.2f, -2.5f, CYAN, pulse, fb);
            l.end();
        }
        if (has(data, Cyberware.MONOWIRE)) {
            float w = slim ? 3f : 4f;
            Part r = new Part(pose, m.rightArm);
            cubeA(r, glow, -2f, 9.6f, -0.5f, w - 4f, 12.5f, 0.5f, PINK, pulse, fb);
            r.end();
        }
        if (has(data, Cyberware.REINFORCED_TENDONS)) {
            for (ModelPart leg : new ModelPart[]{m.rightLeg, m.leftLeg}) {
                Part p = new Part(pose, leg);
                cubeA(p, glow, -0.5f, 5.1f, -2.6f, 0.5f, 5.9f, -2.4f, CYAN, pulse, fb);
                p.end();
            }
        }
    }

    private static boolean has(int mask, Cyberware c) { return (mask & c.bit()) != 0; }

    /** Pushes the pose of a model part (position, rotation) so cubes follow its animation. */
    private record Part(PoseStack pose, ModelPart part) {
        Part {
            pose.pushPose();
            part.translateAndRotate(pose);
        }

        void end() { pose.popPose(); }
    }

    private static void cube(Part p, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int light) {
        cubeA(p, vc, x0, y0, z0, x1, y1, z1, rgb, 1f, light);
    }

    private static void cubeA(Part p, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                              int rgb, float bright, int light) {
        float r = ((rgb >> 16) & 255) / 255f * bright, g = ((rgb >> 8) & 255) / 255f * bright, b = (rgb & 255) / 255f * bright;
        PoseStack.Pose last = p.pose().last();
        Matrix4f m = last.pose();
        Matrix3f n = last.normal();
        x0 /= 16; y0 /= 16; z0 /= 16; x1 /= 16; y1 /= 16; z1 /= 16;
        // -Z (front)
        quad(vc, m, n, r, g, b, light, 0, 0, -1, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        // +Z (back)
        quad(vc, m, n, r, g, b, light, 0, 0, 1, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
        // -X
        quad(vc, m, n, r, g, b, light, -1, 0, 0, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
        // +X
        quad(vc, m, n, r, g, b, light, 1, 0, 0, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        // -Y (model space: top)
        quad(vc, m, n, r, g, b, light, 0, -1, 0, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
        // +Y (model space: bottom)
        quad(vc, m, n, r, g, b, light, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float r, float g, float b, int light,
                             float nx, float ny, float nz, float... v) {
        float[][] uvs = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        // Emit both windings so the face shows regardless of the render type's culling.
        for (int k = 0; k < 8; k++) {
            int i = k < 4 ? k : 7 - k;
            vc.vertex(m, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).color(r, g, b, 1f).uv(uvs[i][0], uvs[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
