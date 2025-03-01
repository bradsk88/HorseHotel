package ca.bradj.horsehotel;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HorseHotel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityEvents {

    @SubscribeEvent
    public static void entityAttrEvent(EntityAttributeCreationEvent event) {
        event.put(
                EntitiesInit.FAKE_HORSE.get(),
                NotHorseEntity.setAttributes()
        );
        event.put(
                EntitiesInit.STABLE_ATTENDANT.get(),
                StableAttendant.setAttributes()
        );
    }

}
