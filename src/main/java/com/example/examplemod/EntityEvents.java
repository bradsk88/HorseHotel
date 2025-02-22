package com.example.examplemod;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityEvents {

    @SubscribeEvent
    public static void entityAttrEvent(EntityAttributeCreationEvent event) {
        event.put(
                com.example.examplemod.EntitiesInit.VISITOR.get(),
                NotHorseEntity.setAttributes()
        );
    }

}
