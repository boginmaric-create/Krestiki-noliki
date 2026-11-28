package com.edgefan.edgerunners.item;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.cyber.Cyberware;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EdgerunnersMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EdgerunnersMod.MODID);

    public static final RegistryObject<Item> CYBER_CHIP = ITEMS.register("cyber_chip", () -> new Item(new Item.Properties()));

    public static final Map<Cyberware, RegistryObject<Item>> IMPLANTS = new EnumMap<>(Cyberware.class);

    static {
        for (Cyberware c : Cyberware.values()) {
            IMPLANTS.put(c, ITEMS.register(c.id, () -> new CyberwareItem(c, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))));
        }
    }

    public static final RegistryObject<Item> MANTIS_BLADE = ITEMS.register("mantis_blade",
            () -> new MantisBladeItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> IMMUNOSUPPRESSANT = ITEMS.register("immunosuppressant",
            () -> new ImmunosuppressantItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> RIPPERDOC_KIT = ITEMS.register("ripperdoc_kit",
            () -> new RipperdocKitItem(new Item.Properties().durability(8)));

    public static final RegistryObject<Item> AMMO = ITEMS.register("ammo", () -> new Item(new Item.Properties()));

    public static final Map<com.edgefan.edgerunners.weapon.GunSpec, RegistryObject<Item>> GUNS = new java.util.EnumMap<>(com.edgefan.edgerunners.weapon.GunSpec.class);

    static {
        for (com.edgefan.edgerunners.weapon.GunSpec g : com.edgefan.edgerunners.weapon.GunSpec.values())
            GUNS.put(g, ITEMS.register(g.id, () -> new com.edgefan.edgerunners.weapon.GunItem(g, new Item.Properties().rarity(Rarity.RARE))));
    }

    public static final Map<com.edgefan.edgerunners.hero.HeroSpec, RegistryObject<Item>> PERSONAS = new java.util.EnumMap<>(com.edgefan.edgerunners.hero.HeroSpec.class);

    static {
        for (com.edgefan.edgerunners.hero.HeroSpec h : com.edgefan.edgerunners.hero.HeroSpec.values())
            PERSONAS.put(h, ITEMS.register("persona_" + h.id, () -> new com.edgefan.edgerunners.hero.PersonaChipItem(h, new Item.Properties().rarity(Rarity.EPIC))));
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.edgerunners"))
            .icon(() -> new ItemStack(IMPLANTS.get(Cyberware.SANDEVISTAN).get()))
            .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
            .build());

    private ModItems() {}
}
