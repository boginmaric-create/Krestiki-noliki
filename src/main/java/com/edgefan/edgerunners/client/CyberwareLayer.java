package com.edgefan.edgerunners.client;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.client.chrome.ChromeModel;
import com.edgefan.edgerunners.client.chrome.ChromeRenderer;
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

/**
 * Draws installed cyberware on the player model, textured with the same material atlas as the items.
 * Coordinates are in model pixels relative to each body part.
 */
public class CyberwareLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation ATLAS = new ResourceLocation(EdgerunnersMod.MODID, "textures/item/cyberware.png");

    // material ids = tiles of the atlas (see tools/gen_materials.py)
    private static final int STEEL = 0, PANEL = 1, CHROME = 2, CARBON = 3, RUBBER = 4, HAZARD = 5,
            G_CYAN = 6, G_PINK = 7, G_GREEN = 8, G_RED = 9, MUSCLE = 10, GOLD = 12;

    private final boolean slim;

    public CyberwareLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, boolean slim) {
        super(parent);
        this.slim = slim;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        // Hero persona replaces the whole look.
        com.edgefan.edgerunners.hero.HeroSpec hero = com.edgefan.edgerunners.hero.HeroSpec.byStored(ClientCyberData.HEROES.getOrDefault(player.getId(), 0));
        if (hero != null) {
            ChromeModel heroModel = ChromeRenderer.load("hero/" + hero.id + ".json");
            if (heroModel != null)
                ChromeRenderer.renderAll(pose, buffers, light, getParentModel(), heroModel,
                        ChromeRenderer.Signals.of(player, partialTick, limbSwing, limbSwingAmount), slim);
            return;
        }
        Integer data = ClientCyberData.VISIBLE.get(player.getId());
        if (data == null || (data & ~ClientCyberData.SANDE_ACTIVE_BIT) == 0) return;
        Cyberware set = ChromeRenderer.installedSet(data);
        if (set != null) {
            ChromeModel model = ChromeRenderer.model(set);
            if (model != null)
                ChromeRenderer.renderAll(pose, buffers, light, getParentModel(), model,
                        ChromeRenderer.Signals.of(player, partialTick, limbSwing, limbSwingAmount), slim);
        }
        boolean sandeActive = (data & ClientCyberData.SANDE_ACTIVE_BIT) != 0;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        float w = slim ? 3f : 4f;

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(ATLAS));
        if (has(data, Cyberware.SANDEVISTAN)) {
            Part p = new Part(pose, m.body);
            cube(p, vc, -0.6f, 0.3f, 2.0f, 0.6f, 11.7f, 2.9f, CHROME, light);
            cube(p, vc, -1.9f, 0.4f, 2.0f, -1.3f, 11.6f, 2.6f, RUBBER, light);
            cube(p, vc, 1.3f, 0.4f, 2.0f, 1.9f, 11.6f, 2.6f, RUBBER, light);
            for (int i = 0; i < 5; i++) {
                float y = 0.8f + i * 2.2f;
                cube(p, vc, -2.2f, y, 2.0f, 2.2f, y + 1.2f, 3.3f, STEEL, light);
                cube(p, vc, -0.5f, y + 0.2f, 3.3f, 0.5f, y + 1.0f, 4.0f, PANEL, light);
            }
            cube(p, vc, -1.5f, -0.2f, 1.9f, 1.5f, 0.5f, 3.0f, GOLD, light);
            p.end();
        }
        if (has(data, Cyberware.GORILLA_ARMS)) {
            for (ModelPart arm : new ModelPart[]{m.rightArm, m.leftArm}) {
                float x0 = arm == m.rightArm ? -3f : -1f, x1 = x0 + w;
                Part p = new Part(pose, arm);
                cube(p, vc, x0 - 0.5f, 3.5f, -2.5f, x1 + 0.5f, 10.4f, 2.5f, STEEL, light);
                cube(p, vc, x0 - 0.6f, 4.5f, -2.6f, x1 + 0.6f, 5.3f, 2.6f, HAZARD, light);
                cube(p, vc, x0 - 0.55f, 6f, 2.5f, x1 + 0.55f, 9.5f, 2.8f, CARBON, light);
                cube(p, vc, x0 - 0.3f, 9.2f, -2.9f, x1 + 0.3f, 10.6f, -2.5f, PANEL, light);
                float px = arm == m.rightArm ? x0 - 0.9f : x1 + 0.5f;
                cube(p, vc, px, 5.5f, -0.4f, px + 0.4f, 9f, 0.4f, CHROME, light);
                p.end();
            }
        }
        if (has(data, Cyberware.PROJECTILE_LAUNCHER)) {
            Part r = new Part(pose, m.rightArm);
            cube(r, vc, -4.3f, 3.5f, -2.6f, -3.0f, 9.0f, 2.6f, PANEL, light);
            cube(r, vc, -4.35f, 4.0f, -2.65f, -3.0f, 4.6f, 2.65f, HAZARD, light);
            for (float z : new float[]{-1.8f, 0.4f}) cube(r, vc, -5.4f, 4.6f, z, -4.3f, 9.4f, z + 1.4f, CHROME, light);
            r.end();
        }
        if (has(data, Cyberware.MONOWIRE)) {
            Part r = new Part(pose, m.rightArm);
            cube(r, vc, -3.4f, 8.4f, -2.4f, w - 3f + 0.4f, 9.6f, 2.4f, PANEL, light);
            cube(r, vc, -3.45f, 8.8f, -2.45f, w - 3f + 0.45f, 9.2f, 2.45f, HAZARD, light);
            r.end();
        }
        if (has(data, Cyberware.REINFORCED_TENDONS)) {
            for (ModelPart leg : new ModelPart[]{m.rightLeg, m.leftLeg}) {
                Part p = new Part(pose, leg);
                cube(p, vc, -2.4f, 4.5f, -2.4f, 2.4f, 6.5f, 2.4f, STEEL, light);
                cube(p, vc, -1.2f, 1.5f, -2.35f, 1.2f, 9.5f, -2.05f, MUSCLE, light);
                cube(p, vc, -0.4f, 7f, 2.05f, 0.4f, 11.5f, 2.5f, CHROME, light);
                p.end();
            }
        }

        // Glowing parts: full-bright, additive-looking emissive pass.
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(ATLAS));
        int fb = LightTexture.FULL_BRIGHT;
        float pulse = 0.7f + 0.3f * Mth.sin(ageInTicks * (sandeActive ? 0.9f : 0.15f));
        if (has(data, Cyberware.SANDEVISTAN)) {
            Part p = new Part(pose, m.body);
            for (int i = 0; i < 5; i++) {
                float y = 0.9f + i * 2.2f;
                int mat = sandeActive && i % 2 == 1 ? G_CYAN : G_GREEN;
                cubeA(p, glow, -2.8f, y, 2.3f, -2.2f, y + 1f, 3.0f, mat, pulse, fb);
                cubeA(p, glow, 2.2f, y, 2.3f, 2.8f, y + 1f, 3.0f, mat, pulse, fb);
            }
            cubeA(p, glow, -0.25f, 0.5f, 2.9f, 0.25f, 11.5f, 3.05f, G_GREEN, pulse, fb);
            p.end();
        }
        if (has(data, Cyberware.KIROSHI_OPTICS)) {
            Part p = new Part(pose, m.head);
            cubeA(p, glow, -3f, -4f, -4.15f, -1f, -3f, -4.02f, G_RED, 1f, fb);
            cubeA(p, glow, 1f, -4f, -4.15f, 3f, -3f, -4.02f, G_RED, 1f, fb);
            cubeA(p, glow, -4.05f, -4.5f, -3f, -4.0f, -3.8f, -0.5f, G_RED, 0.6f, fb);
            cubeA(p, glow, 4.0f, -4.5f, -3f, 4.05f, -3.8f, -0.5f, G_RED, 0.6f, fb);
            p.end();
        }
        if (has(data, Cyberware.GORILLA_ARMS)) {
            for (ModelPart arm : new ModelPart[]{m.rightArm, m.leftArm}) {
                float x0 = arm == m.rightArm ? -3f : -1f;
                Part p = new Part(pose, arm);
                for (int k = 0; k < 3; k++) {
                    float x = x0 + 0.3f + k * (w - 0.6f) / 3f;
                    cubeA(p, glow, x, 9.6f, -3.0f, x + 0.7f, 10.2f, -2.85f, G_CYAN, pulse, fb);
                }
                p.end();
            }
        }
        if (has(data, Cyberware.MONOWIRE)) {
            Part r = new Part(pose, m.rightArm);
            float swing = Mth.sin(ageInTicks * 0.2f) * 0.4f;
            cubeA(r, glow, -1.6f + swing, 9.6f, -0.3f, -1.2f + swing, 13.0f, 0.1f, G_PINK, pulse, fb);
            cubeA(r, glow, -1.9f + swing, 12.8f, -0.6f, -0.9f + swing, 13.6f, 0.4f, G_PINK, 1f, fb);
            r.end();
        }
        if (has(data, Cyberware.PROJECTILE_LAUNCHER)) {
            Part r = new Part(pose, m.rightArm);
            for (float z : new float[]{-1.8f, 0.4f}) cubeA(r, glow, -5.2f, 9.4f, z + 0.2f, -4.5f, 9.5f, z + 1.2f, G_RED, pulse, fb);
            r.end();
        }
        if (has(data, Cyberware.REINFORCED_TENDONS)) {
            for (ModelPart leg : new ModelPart[]{m.rightLeg, m.leftLeg}) {
                Part p = new Part(pose, leg);
                cubeA(p, glow, -0.5f, 5.1f, -2.6f, 0.5f, 5.9f, -2.4f, G_CYAN, pulse, fb);
                p.end();
            }
        }
    }

    private static boolean has(int mask, Cyberware c) { return (mask & c.bit()) != 0; }

    /** Pushes the pose of a model part so cubes follow its animation. */
    private record Part(PoseStack pose, ModelPart part) {
        Part {
            pose.pushPose();
            part.translateAndRotate(pose);
        }

        void end() { pose.popPose(); }
    }

    private static void cube(Part p, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int mat, int light) {
        cubeA(p, vc, x0, y0, z0, x1, y1, z1, mat, 1f, light);
    }

    private static void cubeA(Part p, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                              int mat, float bright, int light) {
        drawBox(p.pose(), vc, x0, y0, z0, x1, y1, z1, mat, bright, light);
    }

    /** Textured box in model pixels; {@code mat} is the tile index of whichever 4x4 atlas the buffer is bound to. */
    public static void drawBox(PoseStack pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                               int mat, float bright, int light) {
        drawBox(pose, vc, x0, y0, z0, x1, y1, z1, mat, bright, bright, bright, light);
    }

    /** Tinted variant (vehicle paint). */
    public static void drawBox(PoseStack pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                               int mat, float r, float g, float b, int light) {
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        Matrix3f n = last.normal();
        float u0 = (mat % 4) * 0.25f, v0 = (mat / 4) * 0.25f;
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        x0 /= 16; y0 /= 16; z0 /= 16; x1 /= 16; y1 /= 16; z1 /= 16;
        face(vc, m, n, r, g, b, light, 0, 0, -1, u0, v0, dx, dy, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        face(vc, m, n, r, g, b, light, 0, 0, 1, u0, v0, dx, dy, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
        face(vc, m, n, r, g, b, light, -1, 0, 0, u0, v0, dz, dy, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
        face(vc, m, n, r, g, b, light, 1, 0, 0, u0, v0, dz, dy, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        face(vc, m, n, r, g, b, light, 0, -1, 0, u0, v0, dx, dz, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
        face(vc, m, n, r, g, b, light, 0, 1, 0, u0, v0, dx, dz, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    /** One textured face. The tile covers 4x4 model pixels; smaller faces use a matching part of it. */
    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n, float r, float g, float b, int light,
                             float nx, float ny, float nz, float u0, float v0, float w, float h, float... v) {
        float us = 0.25f * Math.min(1f, w / 4f), vs = 0.25f * Math.min(1f, h / 4f);
        float[][] uvs = {{u0, v0}, {u0 + us, v0}, {u0 + us, v0 + vs}, {u0, v0 + vs}};
        for (int k = 0; k < 8; k++) { // both windings
            int i = k < 4 ? k : 7 - k;
            vc.vertex(m, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).color(r, g, b, 1f).uv(uvs[i][0], uvs[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
