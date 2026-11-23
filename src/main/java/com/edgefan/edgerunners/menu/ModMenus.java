package com.edgefan.edgerunners.menu;

import com.edgefan.edgerunners.EdgerunnersMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, EdgerunnersMod.MODID);

    public static final RegistryObject<MenuType<RipperdocMenu>> RIPPERDOC = MENUS.register("ripperdoc",
            () -> IForgeMenuType.create((id, inv, buf) -> new RipperdocMenu(id, inv)));

    private ModMenus() {}
}
