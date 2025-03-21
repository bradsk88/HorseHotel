package ca.bradj.horsehotel;

import ca.bradj.horsehotel.network.HHNetwork;
import ca.bradj.horsehotel.network.ShowHorseRegisterScreenMessage;
import ca.bradj.horsehotel.network.ShowHorseSummonScreenMessage;
import ca.bradj.horsehotel.network.UIHorse;
import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Registration {

    private static final Logger LOGGER = LogManager.getLogger();

    static void interactWithRegistry(
            ServerPlayer player
    ) {
        net.minecraft.world.entity.Entity veh = getVehicleOrNull(player);
        if (veh == null) {
            ImmutableList<UIHorse> fh = buildFakeHorses(player);
            ShowHorseSummonScreenMessage msg = new ShowHorseSummonScreenMessage(fh);
            HHNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
            return;
        }

        HHNBT pd = HHNBT.getPersistentData(player);
        if (pd.contains(HHNBT.Key.REGISTERED_HORSES)) {
            CompoundTag d = pd.getCompound(HHNBT.Key.REGISTERED_HORSES);
            if (d.contains(veh.getUUID().toString())) {
                storeHorseOnPlayer(player, veh);
                veh.remove(Entity.RemovalReason.DISCARDED);
                return;
            }
        }

        ShowHorseRegisterScreenMessage msg = new ShowHorseRegisterScreenMessage(veh.serializeNBT());
        HHNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static ImmutableList<UIHorse> buildFakeHorses(
            Player player
    ) {
        HHNBT pd = HHNBT.getPersistentData(player);
        ImmutableList.Builder<UIHorse> b = ImmutableList.builder();
        if (!pd.contains(HHNBT.Key.REGISTERED_HORSES)) {
            LOGGER.debug("No data");
            return b.build();
        }
        CompoundTag l = pd.getCompound(HHNBT.Key.REGISTERED_HORSES);
        ImmutableList<String> keys = ImmutableList.copyOf(l.getAllKeys());
        for (int i = 0; i < l.size(); i++) {
            b.add(new UIHorse(i, l.getCompound(keys.get(i))));
        }
        return b.build();
    }

    public static boolean storeHorseOnPlayer(
            ServerPlayer player,
            net.minecraft.world.entity.Entity veh
    ) {
        if (!(veh instanceof Horse vh)) {
            return false;
        }
        CompoundTag data = updateDataToFullHealth(player.getLevel(), vh.serializeNBT());
        HHNBT nhd = new HHNBT(() -> data);
        nhd.put(HHNBT.Key.ARMOR_ITEM, vh.getArmor().serializeNBT());
        LOGGER.debug("Horse: {}", data);
        HHNBT pd = HHNBT.getPersistentData(player);
        CompoundTag l = pd.getOrDefault(HHNBT.Key.REGISTERED_HORSES, CompoundTag::new);
        l.put(veh.getUUID().toString(), data);
        pd.put(HHNBT.Key.REGISTERED_HORSES, l);
        return true;
    }

    public static CompoundTag updateDataToFullHealth(
            ServerLevel level,
            CompoundTag data
    ) {
        Horse holder = EntityType.HORSE.create(level);
        holder.deserializeNBT(data);
        holder.setHealth(holder.getMaxHealth());
        holder.hurtTime = 0;
        data = holder.serializeNBT();
        return data;
    }

    public static @Nullable Entity getVehicleOrNull(ServerPlayer sender) {
        Entity vehicle = sender.getVehicle();
        if (isRegisterable(vehicle)) {
            // TODO: Support registration of other rideable entities?
            return vehicle;
        }
        return null;
    }

    public static boolean isRegisterable(Entity vehicle) {
        return vehicle instanceof Horse;
    }

    public static void storeRiddenHorse(ServerPlayer sender) {
        net.minecraft.world.entity.Entity veh = Registration.getVehicleOrNull(sender);
        if (veh == null) {
            HorseHotel.logBug("Vehicle was NULL after confirming registration. As a result, nothing has happened.");
            return;
        }
        storeHorseOnPlayer(sender, veh);
        veh.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    public static boolean isAssociated(
            ServerPlayer player,
            UUID uuid
    ) {
        HHNBT hpd = HHNBT.getPersistentData(player);
        if (!hpd.contains(HHNBT.Key.REGISTERED_HORSES)) {
            return false;
        }
        CompoundTag horses = hpd.getCompound(HHNBT.Key.REGISTERED_HORSES);
        return horses.getAllKeys().contains(uuid.toString());
    }
}
