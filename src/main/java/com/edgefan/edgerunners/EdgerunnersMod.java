package com.edgefan.edgerunners;

import com.edgefan.edgerunners.item.ModItems;
import com.edgefan.edgerunners.network.Network;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EdgerunnersMod.MODID)
public class EdgerunnersMod {
    public static final String MODID = "edgerunners";

    public EdgerunnersMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        Network.register();
    }
}
