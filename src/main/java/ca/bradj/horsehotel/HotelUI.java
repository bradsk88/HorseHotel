package ca.bradj.horsehotel;

import ca.bradj.horsehotel.network.HHNetwork;
import ca.bradj.horsehotel.network.OpenHotelMessage;
import ca.bradj.horsehotel.network.ShowHorseSummonScreenMessage;
import ca.bradj.horsehotel.network.ShowStockScreenMessage;
import ca.bradj.horsehotel.network.ShowWelcomeScreenMessage;
import ca.bradj.horsehotel.network.UIHorse;
import ca.bradj.horsehotel.network.UIStockHorse;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Opens a tab of the hotel UI for a player, server-side. Each tab is fetched on demand (the
 * round-trip driven by {@link OpenHotelMessage}); this decides which tab to show — honouring an
 * explicit request, or picking the default from what the Registry and Stock actually hold — and
 * stamps the enabled-tab flags the client needs to render its tabs without fetching the other tab.
 */
public class HotelUI {

    public static void open(
            ServerPlayer sp,
            BlockPos stablePos,
            String tab
    ) {
        ServerLevel sl = sp.getLevel();
        @Nullable StableAttendant att = StableAttendant.findNearest(sl, stablePos, Config.horseEvalRadius);
        boolean stockAvailable = att != null && !Stock.isEmpty(HHNBT.getPersistentData(att));

        ImmutableList<UIHorse> registry = Registration.buildFakeHorses(sp);
        boolean registryEmpty = registry.isEmpty();

        String resolved = resolveTab(tab, registryEmpty, stockAvailable);

        switch (resolved) {
            case OpenHotelMessage.YOUR_HORSES -> send(sp, new ShowHorseSummonScreenMessage(registry, stockAvailable, stablePos));
            case OpenHotelMessage.FOR_SALE -> send(sp, new ShowStockScreenMessage(buildStock(att), !registryEmpty, stablePos));
            default -> send(sp, new ShowWelcomeScreenMessage());
        }
    }

    private static String resolveTab(
            String requested,
            boolean registryEmpty,
            boolean stockAvailable
    ) {
        String tab = requested;
        if (tab == null || tab.isEmpty()) {
            // Default: show the tab that has something in it, preferring the player's own horses.
            if (!registryEmpty) {
                return OpenHotelMessage.YOUR_HORSES;
            }
            return stockAvailable ? OpenHotelMessage.FOR_SALE : "welcome";
        }
        // A requested tab that has gone empty (its Stock sold out, or a registry deregistered)
        // falls back the same way, rather than showing an empty tab.
        if (tab.equals(OpenHotelMessage.FOR_SALE) && !stockAvailable) {
            return registryEmpty ? "welcome" : OpenHotelMessage.YOUR_HORSES;
        }
        if (tab.equals(OpenHotelMessage.YOUR_HORSES) && registryEmpty) {
            return stockAvailable ? OpenHotelMessage.FOR_SALE : "welcome";
        }
        return tab;
    }

    private static ImmutableList<UIStockHorse> buildStock(@Nullable StableAttendant att) {
        if (att == null) {
            return ImmutableList.of();
        }
        HHNBT ad = HHNBT.getPersistentData(att);
        ImmutableList.Builder<UIStockHorse> b = ImmutableList.builder();
        for (int i = 0; i < Stock.size(ad); i++) {
            UUID uuid = Stock.uuid(ad, i);
            var data = Stock.horseData(ad, i);
            if (uuid != null && data != null) {
                b.add(new UIStockHorse(uuid, data));
            }
        }
        return b.build();
    }

    private static void send(
            ServerPlayer sp,
            Object message
    ) {
        HHNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), message);
    }
}
