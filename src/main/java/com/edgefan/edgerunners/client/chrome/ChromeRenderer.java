package com.edgefan.edgerunners.client.chrome;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.client.CyberwareLayer;
import com.edgefan.edgerunners.cyber.CyberSlot;
import com.edgefan.edgerunners.cyber.Cyberware;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Renders the installed external chrome set on a player (third person) or on the first-person arm. */
public final class ChromeRenderer {
    private static final ResourceLocation BASE_ATLAS = new ResourceLocation(EdgerunnersMod.MODID, "textures/item/cyberware.png");
    private static final ResourceLocation CHROME_ATLAS = new ResourceLocation(EdgerunnersMod.MODID, "textures/item/chrome.png");
    private static final Map<String, Optional<ChromeModel>> CACHE = new java.util.HashMap<>();

    private ChromeRenderer() {}

    public static void clearCache() { CACHE.clear(); }

    public static ChromeModel model(Cyberware set) { return load("chrome/" + set.id + ".json"); }

    /** Loads (once) a group/cube model from assets/edgerunners/&lt;path&gt;. */
    public static ChromeModel load(String path) {
        return CACHE.computeIfAbsent(path, s -> {
            ResourceLocation rl = new ResourceLocation(EdgerunnersMod.MODID, s);
            try (Reader r = Minecraft.getInstance().getResourceManager().openAsReader(rl)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                return Optional.of(ChromeModel.parse(o));
            } catch (Exception e) {
                EdgerunnersMod.LOGGER.error("Failed to load chrome model {}", rl, e);
                return Optional.empty();
            }
        }).orElse(null);
    }

    /** Supplies animation values and glow brightness to the group renderer. */
    public interface AnimSource {
        float signal(String name, float phase);

        float glow(String mode);

        /** Paint colour for cubes marked "paint" (r, g, b), or null for untinted. */
        default float[] paint() { return null; }
    }

    /** Animation inputs for one frame of external chrome. */
    public record Signals(float age, float deploy, float sprint, float swing, float sway, float breath) implements AnimSource {
        @Override
        public float signal(String name, float phase) {
            return switch (name) {
                case "deploy" -> deploy;
                case "sprint" -> sprint;
                case "swing" -> swing;
                case "sway" -> sway * Mth.cos(phase) + Mth.sin(age * 0.3f + phase) * 0.15f;
                case "breath" -> Mth.sin(age * 0.08f + phase);
                case "age" -> age;
                default -> 0f;
            };
        }

        @Override
        public float glow(String mode) {
            return switch (mode) {
                case "pulse" -> 0.7f + 0.3f * Mth.sin(age * 0.15f);
                case "breath" -> 0.55f + 0.45f * (0.5f + 0.5f * breath);
                case "heat" -> 0.35f + 0.65f * sprint;
                case "deploy" -> 0.15f + 0.85f * deploy;
                case "flicker" -> (Mth.sin(age * 1.7f) > -0.92f ? 0.85f : 0.25f) + 0.15f * Mth.sin(age * 0.4f);
                default -> 1f;
            };
        }

        public static Signals of(AbstractClientPlayer p, float partial, float limbSwing, float limbSwingAmount) {
            float age = p.tickCount + partial;
            float attack = p.getAttackAnim(partial);
            return new Signals(age, ChromeState.deploy(p, partial), ChromeState.sprint(p, partial),
                    Mth.sin(attack * Mth.PI), Mth.sin(limbSwing * 0.6662f) * Math.min(1f, limbSwingAmount * 1.6f), Mth.sin(age * 0.08f));
        }
    }

    /** Draw passes: each texture/glow combination needs its own buffer. */
    private enum Pass { BASE_SOLID, CHROME_SOLID, BASE_GLOW, CHROME_GLOW }

    /** Renders a list of top-level groups in the current pose (used for vehicles). */
    public static void renderGroups(PoseStack pose, MultiBufferSource buffers, List<ChromeModel.Group> groups, AnimSource src, int light) {
        for (Pass pass : Pass.values()) {
            VertexConsumer vc = buffers.getBuffer(renderType(pass));
            for (ChromeModel.Group g : groups) renderGroup(pose, vc, g, src, pass, light);
        }
    }

    public static Cyberware installedSet(int mask) { return CyberSlot.CHROME.installed(mask); }

    /** Third-person: every body part. */
    public static void renderAll(PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> m,
                                 ChromeModel model, AnimSource sig, boolean slim) {
        for (Pass pass : Pass.values()) {
            VertexConsumer vc = buffers.getBuffer(renderType(pass));
            for (Map.Entry<String, List<ChromeModel.Group>> e : model.parts.entrySet()) {
                ModelPart part = part(m, e.getKey());
                if (part == null || !part.visible) continue;
                pose.pushPose();
                part.translateAndRotate(pose);
                slimAdjust(pose, e.getKey(), slim);
                for (ChromeModel.Group g : e.getValue()) renderGroup(pose, vc, g, sig, pass, light);
                pose.popPose();
            }
        }
    }

    /** First-person: one arm, pose already positioned by the hand renderer. */
    public static void renderArm(PoseStack pose, MultiBufferSource buffers, int light, ModelPart arm, String partName,
                                 ChromeModel model, AnimSource sig, boolean slim) {
        List<ChromeModel.Group> groups = model.parts.get(partName);
        if (groups == null) return;
        for (Pass pass : Pass.values()) {
            VertexConsumer vc = buffers.getBuffer(renderType(pass));
            pose.pushPose();
            arm.translateAndRotate(pose);
            slimAdjust(pose, partName, slim);
            for (ChromeModel.Group g : groups) renderGroup(pose, vc, g, sig, pass, light);
            pose.popPose();
        }
    }

    private static RenderType renderType(Pass pass) {
        return switch (pass) {
            case BASE_SOLID -> RenderType.entityCutoutNoCull(BASE_ATLAS);
            case CHROME_SOLID -> RenderType.entityCutoutNoCull(CHROME_ATLAS);
            case BASE_GLOW -> RenderType.entityTranslucentEmissive(BASE_ATLAS);
            case CHROME_GLOW -> RenderType.entityTranslucentEmissive(CHROME_ATLAS);
        };
    }

    /** Slim (3px) arms: squeeze the 4px-wide design toward the arm's centre. */
    private static void slimAdjust(PoseStack pose, String part, boolean slim) {
        if (!slim) return;
        // wide arm centre is x=-1 (right) / +1 (left), slim centre is -0.5 / +0.5: x' = 0.75x +/- 0.25px
        if (part.equals("right_arm")) { pose.translate(0.25f / 16f, 0, 0); pose.scale(0.75f, 1f, 1f); }
        else if (part.equals("left_arm")) { pose.translate(-0.25f / 16f, 0, 0); pose.scale(0.75f, 1f, 1f); }
    }

    private static ModelPart part(PlayerModel<?> m, String name) {
        return switch (name) {
            case "head" -> m.head;
            case "body" -> m.body;
            case "right_arm" -> m.rightArm;
            case "left_arm" -> m.leftArm;
            case "right_leg" -> m.rightLeg;
            case "left_leg" -> m.leftLeg;
            default -> null;
        };
    }

    private static void renderGroup(PoseStack pose, VertexConsumer vc, ChromeModel.Group g, AnimSource sig, Pass pass, int light) {
        float rx = g.rx(), ry = g.ry(), rz = g.rz(), mx = 0, my = 0, mz = 0;
        for (ChromeModel.Anim a : g.anims()) {
            float v = sig.signal(a.signal(), a.phase());
            float val = a.range() ? a.from() + (a.to() - a.from()) * v : "age".equals(a.signal()) ? v * a.speed() : a.amp() * v;
            if (a.rotation()) {
                if (a.axis() == 0) rx += val; else if (a.axis() == 1) ry += val; else rz += val;
            } else {
                if (a.axis() == 0) mx += val; else if (a.axis() == 1) my += val; else mz += val;
            }
        }
        pose.pushPose();
        pose.translate((g.px() + mx) / 16f, (g.py() + my) / 16f, (g.pz() + mz) / 16f);
        if (rx != 0 || ry != 0 || rz != 0)
            pose.mulPose(new Quaternionf().rotationZYX(rz * Mth.DEG_TO_RAD, ry * Mth.DEG_TO_RAD, rx * Mth.DEG_TO_RAD));
        pose.translate(-g.px() / 16f, -g.py() / 16f, -g.pz() / 16f);

        boolean glowPass = pass == Pass.BASE_GLOW || pass == Pass.CHROME_GLOW;
        boolean chromePass = pass == Pass.CHROME_SOLID || pass == Pass.CHROME_GLOW;
        for (ChromeModel.Cube c : g.cubes()) {
            if (c.chromeAtlas() != chromePass || (c.glow() != null) != glowPass) continue;
            float bright = glowPass ? sig.glow(c.glow()) : 1f;
            if (glowPass && bright <= 0.02f) continue;
            float[] tint = c.paint() ? sig.paint() : null;
            if (tint != null)
                CyberwareLayer.drawBox(pose, vc, c.x0(), c.y0(), c.z0(), c.x1(), c.y1(), c.z1(), c.mat(), tint[0], tint[1], tint[2], light);
            else
                CyberwareLayer.drawBox(pose, vc, c.x0(), c.y0(), c.z0(), c.x1(), c.y1(), c.z1(), c.mat(), bright,
                        glowPass ? LightTexture.FULL_BRIGHT : light);
        }
        for (ChromeModel.Group ch : g.children()) renderGroup(pose, vc, ch, sig, pass, light);
        pose.popPose();
    }
}
