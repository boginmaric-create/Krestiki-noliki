package com.edgefan.edgerunners.vehicle;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModVehicles {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EdgerunnersMod.MODID);

    public static final RegistryObject<EntityType<VehicleEntity>> SPEEDSTER = register(VehicleSpec.SPEEDSTER);
    public static final RegistryObject<EntityType<VehicleEntity>> CYBER_BIKE = register(VehicleSpec.CYBER_BIKE);

    public static final RegistryObject<EntityType<com.edgefan.edgerunners.block.ChairSeatEntity>> CHAIR_SEAT = ENTITIES.register("chair_seat",
            () -> EntityType.Builder.<com.edgefan.edgerunners.block.ChairSeatEntity>of(com.edgefan.edgerunners.block.ChairSeatEntity::new, MobCategory.MISC)
                    .sized(0.2f, 0.2f).noSave().clientTrackingRange(10).build("chair_seat"));

    public static final RegistryObject<Item> SPEEDSTER_ITEM = ModItems.ITEMS.register("speedster",
            () -> new VehicleItem(SPEEDSTER, VehicleSpec.SPEEDSTER, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CYBER_BIKE_ITEM = ModItems.ITEMS.register("cyber_bike",
            () -> new VehicleItem(CYBER_BIKE, VehicleSpec.CYBER_BIKE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private static RegistryObject<EntityType<VehicleEntity>> register(VehicleSpec spec) {
        return ENTITIES.register(spec.id, () -> EntityType.Builder.<VehicleEntity>of((t, l) -> new VehicleEntity(t, l, spec), MobCategory.MISC)
                .sized(spec.width, spec.height).clientTrackingRange(10).updateInterval(1).build(spec.id));
    }

    private ModVehicles() {}
}
