package ca.bradj.horsehotel;

import ca.bradj.horsehotel.network.HHNetwork;
import ca.bradj.horsehotel.network.ShowHorseRegisterScreenMessage;
import ca.bradj.horsehotel.network.ShowHorseSummonScreenMessage;
import ca.bradj.horsehotel.network.UIHorse;
import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

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
        ListTag l = pd.getList(HHNBT.Key.REGISTERED_HORSES);
        for (int i = 0; i < l.size(); i++) {
            b.add(new UIHorse(i, l.getCompound(i)));
        }
        return b.build();
    }

    public static boolean storeHorseOnPlayer(
            Player player,
            net.minecraft.world.entity.Entity veh
    ) {
        if (!(veh instanceof Horse vh)) {
            return false;
        }
        CompoundTag data = vh.serializeNBT();
        LOGGER.debug("Horse: {}", data);
        HHNBT pd = HHNBT.getPersistentData(player);
        ListTag l = pd.getOrDefault(HHNBT.Key.REGISTERED_HORSES, ListTag::new);
        l.add(data);
        pd.put(HHNBT.Key.REGISTERED_HORSES, l);
        return true;
    }

    public static @Nullable Entity getVehicleOrNull(ServerPlayer sender) {
        Entity vehicle = sender.getVehicle();
        if (vehicle instanceof Horse) {
            // TODO: Support registration of other rideable entities?
            return vehicle;
        }
        return null;
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
}
