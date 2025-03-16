package ca.bradj.horsehotel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = HorseHotel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityForgeEvents {

    @SubscribeEvent
    public static void handleHorseDeath(LivingDeathEvent event) {
        Level level = event.getEntity().getLevel();
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (!Registration.isRegisterable(event.getEntity())) {
            return;
        }
        UUID uuid = event.getEntity().getUUID();
        for (ServerPlayer player : sl.players()) {
            if (Registration.isAssociated(player, uuid)) {
                // TODO: Also mark as dead and support resurrection
                Registration.storeHorseOnPlayer(player, event.getEntity());
            }
        }
    }

}
