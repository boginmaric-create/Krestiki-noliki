package com.edgefan.edgerunners.client.fx;

import com.edgefan.edgerunners.network.ClientCyberData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Sandevistan afterimages: coloured, fading copies of a player left behind while the implant runs. */
public final class Ghosts {
    private static final int LIFE = 12;
    private static final float[][] COLORS = {{0.2f, 1f, 0.45f}, {0.99f, 0.93f, 0.1f}, {1f, 0.2f, 0.5f}, {0.1f, 0.9f, 1f}};
    private static final List<Ghost> GHOSTS = new ArrayList<>();
    private static int colorIndex;

    private record Ghost(AbstractClientPlayer player, ResourceLocation skin, Vec3 pos, float bodyRot, float headRot, float xRot,
                         float limbPos, float limbSpeed, boolean crouching, float attack, float ageInTicks, float[] color, int[] age) {}

    private Ghosts() {}

    public static void tick(Minecraft mc) {
        for (Iterator<Ghost> it = GHOSTS.iterator(); it.hasNext(); ) if (++it.next().age()[0] >= LIFE) it.remove();
        if (mc.level == null || mc.level.getGameTime() % 2 != 0) return;
        for (Player p : mc.level.players()) {
            Integer data = ClientCyberData.VISIBLE.get(p.getId());
            if (data == null || (data & ClientCyberData.SANDE_ACTIVE_BIT) == 0 || !(p instanceof AbstractClientPlayer cp) || p.isInvisible())
                continue;
            if (p.getDeltaMovement().horizontalDistanceSqr() < 0.0004 && GHOSTS.size() > 0 && p.walkAnimation.speed() < 0.05f) continue;
            GHOSTS.add(new Ghost(cp, cp.getSkinTextureLocation(), p.position(), p.yBodyRot, p.yHeadRot, p.getXRot(),
                    p.walkAnimation.position(), p.walkAnimation.speed(), p.isCrouching(), p.getAttackAnim(0), p.tickCount,
                    COLORS[colorIndex++ % COLORS.length], new int[]{0}));
        }
    }

    public static void clear() { GHOSTS.clear(); }

    @SuppressWarnings("unchecked")
    public static void render(PoseStack pose, Vec3 cam, float partial) {
        if (GHOSTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        for (Ghost g : GHOSTS) {
            if (g.player().isRemoved()) continue;
            if (firstPerson && g.player() == mc.player && g.pos().add(0, 1.6, 0).distanceToSqr(cam) < 1.4) continue;
            EntityRenderer<? super AbstractClientPlayer> er = mc.getEntityRenderDispatcher().getRenderer(g.player());
            if (!(er instanceof PlayerRenderer pr)) continue;
            PlayerModel<AbstractClientPlayer> model = pr.getModel();
            float life = (g.age()[0] + partial) / LIFE;
            float alpha = 0.55f * (1f - life);
            if (alpha <= 0.01f) continue;

            pose.pushPose();
            pose.translate(g.pos().x - cam.x, g.pos().y - cam.y, g.pos().z - cam.z);
            pose.mulPose(Axis.YP.rotationDegrees(180f - g.bodyRot()));
            pose.scale(-1f, -1f, 1f);
            pose.scale(0.9375f, 0.9375f, 0.9375f);
            pose.translate(0f, -1.501f, 0f);
            model.attackTime = g.attack();
            model.crouching = g.crouching();
            model.riding = false;
            model.young = false;
            model.setupAnim(g.player(), g.limbPos(), g.limbSpeed(), g.ageInTicks(), g.headRot() - g.bodyRot(), g.xRot());
            float[] c = g.color();
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(g.skin())),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, c[0], c[1], c[2], alpha);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
