package com.example.examplemod;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlocksInit {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS,
            ExampleMod.MODID
    );

    public static final RegistryObject<Block> REGISTER_BLOCK = BLOCKS.register(
            RegisterBlock.ID, RegisterBlock::new
    );
}