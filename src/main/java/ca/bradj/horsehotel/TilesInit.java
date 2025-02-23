package ca.bradj.horsehotel;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TilesInit {
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, HorseHotel.MODID
    );

    public static final RegistryObject<BlockEntityType<RegisterBlock.Entity>> REGISTER_BLOCK = TILES.register(
            RegisterBlock.ID, () -> BlockEntityType.Builder.of(
                    RegisterBlock.Entity::new, BlocksInit.REGISTER_BLOCK.get()
            ).build(null)
    );

}