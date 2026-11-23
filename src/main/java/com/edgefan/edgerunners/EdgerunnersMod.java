package com.edgefan.edgerunners;

import com.edgefan.edgerunners.block.ModBlocks;
import com.edgefan.edgerunners.effect.ModEffects;
import com.edgefan.edgerunners.item.ModItems;
import com.edgefan.edgerunners.menu.ModMenus;
import com.edgefan.edgerunners.vehicle.ModVehicles;
import com.edgefan.edgerunners.network.Network;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EdgerunnersMod.MODID)
public class EdgerunnersMod {
    public static final String MODID = "edgerunners";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public EdgerunnersMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.BLOCK_ENTITIES.register(bus);
        ModVehicles.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModMenus.MENUS.register(bus);
        ModEffects.EFFECTS.register(bus);
        ModItems.TABS.register(bus);
        Network.register();
    }
}
