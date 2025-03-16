package ca.bradj.horsehotel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = HorseHotel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityForgeEvents {

    @SubscribeEvent
    public static void handleHorseDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        updateRegisteredHorse(entity);
    }

    @SubscribeEvent
    public static void handleMoveAwayFromHorse(EntityEvent.EnteringSection event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        HHNBT d = HHNBT.getPersistentData(sp);
        if (!d.contains(HHNBT.Key.REGISTERED_HORSES)) {
            return;
        }

        CompoundTag horses = d.getCompound(HHNBT.Key.REGISTERED_HORSES);

        for (String uuid : horses.getAllKeys()) {
            CompoundTag horseData = horses.getCompound(uuid);
            Horse holder = EntityType.HORSE.create(sp.level);
            holder.deserializeNBT(horseData);
            BlockPos pos = holder.getOnPos();
            if (sp.blockPosition().distSqr(pos) > 200) {
                Entity entity = sp.getLevel().getEntity(holder.getUUID());
                if (entity == null) {
                    continue;
                }
                updateRegisteredHorse((LivingEntity) entity);
            }
        }
    }

    private static void updateRegisteredHorse(LivingEntity entity) {
        Level level = entity.getLevel();
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (!Registration.isRegisterable(entity)) {
            return;
        }
        UUID uuid = entity.getUUID();
        for (ServerPlayer player : sl.players()) {
            if (Registration.isAssociated(player, uuid)) {
                // TODO: Also mark as dead and support resurrection
                Registration.storeHorseOnPlayer(player, entity);
            }
        }
    }
}
