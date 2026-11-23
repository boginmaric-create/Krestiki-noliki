package com.edgefan.edgerunners.client;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.cyber.Abilities;
import com.edgefan.edgerunners.cyber.Cyberware;
import com.edgefan.edgerunners.network.AbilityPacket;
import com.edgefan.edgerunners.network.ClientCyberData;
import com.edgefan.edgerunners.network.Network;
import com.edgefan.edgerunners.client.vehicle.VehicleEngineSound;
import com.edgefan.edgerunners.vehicle.VehicleEntity;
import com.edgefan.edgerunners.vehicle.VehicleInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import com.edgefan.edgerunners.client.chrome.ChromeModel;
import com.edgefan.edgerunners.client.chrome.ChromeRenderer;
import com.edgefan.edgerunners.client.chrome.ChromeState;
import com.edgefan.edgerunners.client.fx.FxManager;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EdgerunnersMod.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static boolean wasJumpDown;
    private static boolean doubleJumpUsed;
    private static VehicleEngineSound engineSound;

    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientCyberData.VISIBLE.clear();
        FxManager.clear();
        ChromeState.clear();
        ChromeRenderer.clearCache();
    }

    /** First person: draw the external chrome on the visible arm. */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent e) {
        AbstractClientPlayer p = e.getPlayer();
        Integer data = ClientCyberData.VISIBLE.get(p.getId());
        if (data == null) return;
        Cyberware set = ChromeRenderer.installedSet(data);
        if (set == null) return;
        ChromeModel model = ChromeRenderer.model(set);
        if (model == null) return;
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer pr)) return;
        PlayerModel<AbstractClientPlayer> m = pr.getModel();
        // mirror what PlayerRenderer.renderHand does right after this event, so our cubes line up with the arm
        m.attackTime = 0f;
        m.crouching = false;
        m.swimAmount = 0f;
        m.setupAnim(p, 0f, 0f, 0f, 0f, 0f);
        boolean right = e.getArm() == HumanoidArm.RIGHT;
        ModelPart arm = right ? m.rightArm : m.leftArm;
        arm.xRot = 0f;
        float partial = Minecraft.getInstance().getFrameTime();
        ChromeRenderer.renderArm(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), arm,
                right ? "right_arm" : "left_arm", model, ChromeRenderer.Signals.of(p, partial, 0f, 0f), "slim".equals(p.getModelName()));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            FxManager.render(e.getPoseStack(), e.getCamera().getPosition(), e.getPartialTick());
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        float off = FxManager.shakeOffset((float) e.getPartialTick());
        if (off != 0) {
            e.setRoll(e.getRoll() + off);
            e.setPitch(e.getPitch() + off * 0.35f);
            e.setYaw(e.getYaw() + off * 0.25f);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        FxManager.tick(mc);
        ChromeState.tick(mc);

        while (KeyBinds.SANDEVISTAN.consumeClick()) Network.toServer(new AbilityPacket(Abilities.SANDEVISTAN));
        while (KeyBinds.MONOWIRE.consumeClick()) {
            Network.toServer(new AbilityPacket(Abilities.MONOWIRE));
            if (ClientCyberData.has(Cyberware.MONOWIRE)) p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        while (KeyBinds.SCAN.consumeClick()) Network.toServer(new AbilityPacket(Abilities.SCAN));
        while (KeyBinds.HORN.consumeClick()) if (p.getVehicle() instanceof VehicleEntity) Network.toServer(new AbilityPacket(Abilities.HORN));

        // Vehicles: feed driver input to the entity and keep the engine sound running.
        if (p.getVehicle() instanceof VehicleEntity v) {
            if (v.getControllingPassenger() == p && mc.screen == null) {
                VehicleInput.forward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
                VehicleInput.strafe = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
                VehicleInput.handbrake = mc.options.keyJump.isDown();
                VehicleInput.boost = mc.options.keySprint.isDown();
            } else VehicleInput.clear();
            if (engineSound == null || engineSound.isStopped() || !engineSound.isFor(v)) {
                engineSound = new VehicleEngineSound(v);
                mc.getSoundManager().play(engineSound);
            }
        } else VehicleInput.clear();

        // Reinforced tendons: double jump (movement is client-authoritative, server just resets fall damage).
        boolean jumpDown = mc.options.keyJump.isDown();
        if (p.onGround() || p.isInWater() || p.onClimbable()) doubleJumpUsed = false;
        else if (jumpDown && !wasJumpDown && !doubleJumpUsed && mc.screen == null
                && ClientCyberData.has(Cyberware.REINFORCED_TENDONS) && !p.getAbilities().flying && !p.isPassenger()) {
            doubleJumpUsed = true;
            Vec3 v = p.getDeltaMovement();
            Vec3 look = p.getLookAngle();
            p.setDeltaMovement(v.x + look.x * 0.25, 0.7, v.z + look.z * 0.25);
            p.fallDistance = 0;
            Network.toServer(new AbilityPacket(Abilities.DOUBLE_JUMP));
        }
        wasJumpDown = jumpDown;
    }
}
