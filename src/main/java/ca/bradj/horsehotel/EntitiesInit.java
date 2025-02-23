package ca.bradj.horsehotel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntitiesInit {
    public static final DeferredRegister<EntityType<?>> TILES = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES, HorseHotel.MODID
    );

    public static final RegistryObject<EntityType<NotHorseEntity>> VISITOR = TILES.register(
            NotHorseEntity.ID,
            () -> EntityType.Builder.of(
                                    (EntityType<NotHorseEntity> a, Level b) -> new NotHorseEntity(b),
                                    MobCategory.CREATURE
                            )
                                    .sized(0.6f, 1.6f)
                                    .build(ResourceLocation.tryBuild(HorseHotel.MODID, NotHorseEntity.ID).toString())
    );

}