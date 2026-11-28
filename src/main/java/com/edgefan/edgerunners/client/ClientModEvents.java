package com.edgefan.edgerunners.client;

import com.edgefan.edgerunners.EdgerunnersMod;
import net.minecraftforge.api.distmarker.Dist;
import com.edgefan.edgerunners.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
        e.register(KeyBinds.HORN);
        e.register(KeyBinds.RELOAD);
        e.register(KeyBinds.LAUNCH);
        e.register(KeyBinds.HERO);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> MenuScreens.register(ModMenus.RIPPERDOC.get(), RipperdocScreen::new));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(com.edgefan.edgerunners.vehicle.ModVehicles.SPEEDSTER.get(), com.edgefan.edgerunners.client.vehicle.VehicleRenderer::new);
        e.registerEntityRenderer(com.edgefan.edgerunners.vehicle.ModVehicles.CYBER_BIKE.get(), com.edgefan.edgerunners.client.vehicle.VehicleRenderer::new);
        e.registerEntityRenderer(com.edgefan.edgerunners.vehicle.ModVehicles.CHAIR_SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        e.registerEntityRenderer(com.edgefan.edgerunners.vehicle.ModVehicles.MISSILE.get(), com.edgefan.edgerunners.client.weapon.MissileRenderer::new);
        e.registerBlockEntityRenderer(com.edgefan.edgerunners.block.ModBlocks.CHAIR_BE.get(), ctx -> new com.edgefan.edgerunners.client.operation.ChairRenderer());
    }

    /** Vehicle items show their paint colour. */
    @SubscribeEvent
    public static void itemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> {
            if (tint != 0) return 0xFFFFFF;
            net.minecraft.world.item.DyeColor c = stack.hasTag() && stack.getTag().contains("Color")
                    ? net.minecraft.world.item.DyeColor.byId(stack.getTag().getInt("Color"))
                    : (stack.getItem() == com.edgefan.edgerunners.vehicle.ModVehicles.SPEEDSTER_ITEM.get()
                    ? com.edgefan.edgerunners.vehicle.VehicleSpec.SPEEDSTER.defaultColor : com.edgefan.edgerunners.vehicle.VehicleSpec.CYBER_BIKE.defaultColor);
            float[] d = c.getTextureDiffuseColors();
            int r = (int) (Math.min(1f, d[0] * 1.15f + 0.04f) * 255), g = (int) (Math.min(1f, d[1] * 1.15f + 0.04f) * 255), b = (int) (Math.min(1f, d[2] * 1.15f + 0.04f) * 255);
            return (r << 16) | (g << 8) | b;
        }, com.edgefan.edgerunners.vehicle.ModVehicles.SPEEDSTER_ITEM.get(), com.edgefan.edgerunners.vehicle.ModVehicles.CYBER_BIKE_ITEM.get());
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
        e.registerAboveAll("vehicle_hud", com.edgefan.edgerunners.client.vehicle.VehicleHud.HUD);
        e.registerAboveAll("operation_hud", com.edgefan.edgerunners.client.operation.OperationHud.HUD);
        e.registerAboveAll("gun_hud", com.edgefan.edgerunners.client.weapon.GunHud.HUD);
    }
}
