package com.edgefan.edgerunners.client.weapon;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.client.chrome.ChromeModel;
import com.edgefan.edgerunners.client.chrome.ChromeRenderer;
import com.edgefan.edgerunners.weapon.MissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MissileRenderer extends EntityRenderer<MissileEntity> {
    public MissileRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(MissileEntity m, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ChromeModel model = ChromeRenderer.load("machine/missile.json");
        if (model == null) return;
        float age = m.tickCount + partial;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, m.yRotO, m.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, m.xRotO, m.getXRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 25f));
        ChromeRenderer.renderGroups(pose, buffers, model, model.parts.get("root"), new ChromeRenderer.AnimSource() {
            @Override public float signal(String name, float phase) { return 0f; }
            @Override public float glow(String mode) { return 1f; }
        }, LightTexture.FULL_BRIGHT);
        pose.popPose();
        super.render(m, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity m) { return new ResourceLocation(EdgerunnersMod.MODID, "textures/item/chrome.png"); }
}
