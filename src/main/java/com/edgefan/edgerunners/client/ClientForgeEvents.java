package com.edgefan.edgerunners.client;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.cyber.Abilities;
import com.edgefan.edgerunners.cyber.Cyberware;
import com.edgefan.edgerunners.network.AbilityPacket;
import com.edgefan.edgerunners.network.ClientCyberData;
import com.edgefan.edgerunners.network.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EdgerunnersMod.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static boolean wasJumpDown;
    private static boolean doubleJumpUsed;

    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientCyberData.VISIBLE.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;

        while (KeyBinds.SANDEVISTAN.consumeClick()) Network.toServer(new AbilityPacket(Abilities.SANDEVISTAN));
        while (KeyBinds.MONOWIRE.consumeClick()) {
            Network.toServer(new AbilityPacket(Abilities.MONOWIRE));
            if (ClientCyberData.has(Cyberware.MONOWIRE)) p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        while (KeyBinds.SCAN.consumeClick()) Network.toServer(new AbilityPacket(Abilities.SCAN));

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
