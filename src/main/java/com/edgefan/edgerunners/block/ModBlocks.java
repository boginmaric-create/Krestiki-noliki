package com.edgefan.edgerunners.block;

import com.edgefan.edgerunners.EdgerunnersMod;
import com.edgefan.edgerunners.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EdgerunnersMod.MODID);

    public static final RegistryObject<Block> RIPPERDOC_CHAIR = BLOCKS.register("ripperdoc_chair",
            () -> new RipperdocChairBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().lightLevel(s -> 6)));

    public static final RegistryObject<Item> RIPPERDOC_CHAIR_ITEM = ModItems.ITEMS.register("ripperdoc_chair",
            () -> new BlockItem(RIPPERDOC_CHAIR.get(), new Item.Properties().rarity(Rarity.RARE)));

    private ModBlocks() {}
}
