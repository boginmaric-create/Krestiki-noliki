package com.edgefan.edgerunners.client;

import com.edgefan.edgerunners.EdgerunnersMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EdgerunnersMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(KeyBinds.SANDEVISTAN);
        e.register(KeyBinds.MONOWIRE);
        e.register(KeyBinds.SCAN);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            PlayerRenderer r = e.getSkin(skin);
            if (r != null) r.addLayer(new CyberwareLayer(r, "slim".equals(skin)));
        }
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("cyber_hud", CyberHud.HUD);
    }
}
