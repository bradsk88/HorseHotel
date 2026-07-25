package ca.bradj.horsehotel.autotest;

import ca.bradj.horsehotel.Config;
import ca.bradj.horsehotel.EntitiesInit;
import ca.bradj.horsehotel.HHNBT;
import ca.bradj.horsehotel.HorseHotel;
import ca.bradj.horsehotel.Purchasing;
import ca.bradj.horsehotel.Registration;
import ca.bradj.horsehotel.StableAttendant;
import ca.bradj.horsehotel.Stock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Server-observable checks for Stock and purchasing. Everything a Stable's Attendant holds and a
 * purchase produces lives on the server, so these assert real state; the client-side reservation
 * rendering, glow, and tab icons are genuinely out of reach here, like the stand-in rendering.
 * <p>
 * The Attendant is spawned right where the fake player parked, so it is in range and its chunk is
 * force-loaded. Scenarios that test the purchase path perform the purchase in {@link Scenario#setUp}
 * and assert its effects afterwards.
 */
public final class StockScenarios {

    private StockScenarios() {
    }

    public static List<Scenario> all(BlockPos home) {
        return List.of(
                new StockGeneration(home),
                new Restock(home),
                new Purchase(home),
                new MountSwap(home),
                new PurchaseFailure(home),
                new StandInShowsStockVariant(home)
        );
    }

    /**
     * A Stock stand-in must take on the for-sale horse's coat, not render the default white
     * (variant 0). Regression for the "both for-sale horses were white" report.
     */
    private static class StandInShowsStockVariant extends StockScenario {
        private int expectedVariant;
        private ca.bradj.horsehotel.NotHorseEntity standIn;

        StandInShowsStockVariant(BlockPos home) {
            super(home);
        }

        @Override
        public String name() {
            return "stock:standin_shows_stock_variant";
        }

        @Override
        public String expectation() {
            return "a for-sale stand-in takes on the stock horse's coat variant, not the default white";
        }

        @Override
        protected void prepare(
                ServerLevel level,
                ServerPlayer player
        ) {
            HHNBT ad = HHNBT.getPersistentData(attendant());
            Stock.setStock(level, ad, home(), 1);
            expectedVariant = Stock.horseData(ad, 0).getInt("Variant");

            // A stand-in in the first reserved (Stock) stall, on the platform, next to the attendant.
            standIn = new ca.bradj.horsehotel.NotHorseEntity(level, 0);
            HHNBT.getPersistentData(standIn).put(HHNBT.Key.REGISTERED_HORSE_INDEX, 0);
            standIn.moveTo(home().getX() + 0.5, home().getY(), home().getZ() + 0.5);
            level.addFreshEntity(standIn);
        }

        @Override
        public int settleTicks() {
            return Math.max(1, Config.horseRefreshInterval) + 10;
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            if (standIn.isInvisible()) {
                return "the for-sale stand-in is invisible, so it is not showing the stock horse at all";
            }
            int shown = standIn.getTypeVariantForTest();
            if (shown != expectedVariant) {
                return String.format(
                        "stand-in shows variant %d but the stock horse is variant %d (0 = default white)",
                        shown, expectedVariant
                );
            }
            return null;
        }

        @Override
        public void tearDown(
                ServerLevel level,
                ServerPlayer player
        ) {
            if (standIn != null) {
                standIn.discard();
                standIn = null;
            }
            super.tearDown(level, player);
        }
    }

    /** An Attendant rolls Stock within {@code 1..maxHorsesForSale} on its first tick, and sets a deadline. */
    private static class StockGeneration extends StockScenario {
        StockGeneration(BlockPos home) {
            super(home);
        }

        @Override
        public String name() {
            return "stock:generated_on_first_tick";
        }

        @Override
        public String expectation() {
            return "an attendant rolls a for-sale count within [1, maxHorsesForSale] and sets a restock deadline";
        }

        @Override
        public int settleTicks() {
            return 120;
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            HHNBT ad = HHNBT.getPersistentData(attendant());
            if (!ad.contains(HHNBT.Key.RESTOCK_DEADLINE)) {
                return "attendant never set a restock deadline, so it never rolled its stock";
            }
            int size = Stock.size(ad);
            if (size < 1 || size > Config.maxHorsesForSale) {
                return String.format("stock size %d is outside [1, %d]", size, Config.maxHorsesForSale);
            }
            return null;
        }
    }

    /** When the restock deadline has passed, the Attendant re-rolls and pushes the deadline forward. */
    private static class Restock extends StockScenario {
        Restock(BlockPos home) {
            super(home);
        }

        @Override
        public String name() {
            return "stock:restocks_after_deadline";
        }

        @Override
        public String expectation() {
            return "an attendant past its restock deadline re-rolls and pushes the deadline into the future";
        }

        @Override
        protected void prepare(
                ServerLevel level,
                ServerPlayer player
        ) {
            // A deadline already in the past, so the next attendant tick is a restock rather than
            // the initial roll.
            HHNBT.getPersistentData(attendant()).put(HHNBT.Key.RESTOCK_DEADLINE, level.getGameTime() - 1);
        }

        @Override
        public int settleTicks() {
            return 120;
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            HHNBT ad = HHNBT.getPersistentData(attendant());
            long deadline = ad.getLong(HHNBT.Key.RESTOCK_DEADLINE);
            if (deadline <= level.getGameTime()) {
                return String.format(
                        "restock deadline %d is not in the future (now %d), so no restock happened",
                        deadline, level.getGameTime()
                );
            }
            return null;
        }
    }

    /** Buying a Stock horse spawns a real horse, empties its slot, and does not auto-register it. */
    private static class Purchase extends StockScenario {
        private UUID stockUuid;

        Purchase(BlockPos home) {
            super(home);
        }

        @Override
        public String name() {
            return "stock:purchase_spawns_and_registers_horse";
        }

        @Override
        public String expectation() {
            return "buying a stock horse spawns a tamed, saddled horse, empties the slot, and registers it to the buyer";
        }

        @Override
        protected void prepare(
                ServerLevel level,
                ServerPlayer player
        ) {
            HHNBT ad = HHNBT.getPersistentData(attendant());
            Stock.setStock(level, ad, home(), 1);
            stockUuid = Stock.uuid(ad, 0);
            Purchasing.purchase(player, stockUuid, home());
        }

        @Override
        public int settleTicks() {
            return 2;
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            if (Stock.uuid(HHNBT.getPersistentData(attendant()), 0) != null) {
                return "the purchased slot is still filled, so the purchase did not remove it";
            }
            List<Horse> spawned = horsesNear(level, player);
            if (spawned.isEmpty()) {
                return "no real horse was spawned by the purchase";
            }
            Horse bought = spawned.get(0);
            if (!bought.isTamed()) {
                return "the purchased horse is untamed, so it would buck the buyer off";
            }
            if (!bought.isSaddled()) {
                return "the purchased horse has no saddle, so the buyer cannot steer it";
            }
            if (!Registration.isAssociated(player, bought.getUUID())) {
                return "the purchased horse is not in the buyer's registry, so it was not auto-registered";
            }
            return null;
        }
    }

    /** Buying while riding a horse stables the old one into the registry and mounts the purchase. */
    private static class MountSwap extends StockScenario {
        private UUID oldHorseUuid;

        MountSwap(BlockPos home) {
            super(home);
        }

        @Override
        public String name() {
            return "stock:purchase_while_riding_swaps_mount";
        }

        @Override
        public String expectation() {
            return "buying while riding a horse stores the old horse in the registry and removes it from the world";
        }

        @Override
        protected void prepare(
                ServerLevel level,
                ServerPlayer player
        ) {
            Horse old = EntityType.HORSE.create(level);
            if (old == null) {
                throw new IllegalStateException("could not create a horse to ride");
            }
            old.setPos(home().getX() + 0.5, home().getY(), home().getZ() + 0.5);
            level.addFreshEntity(old);
            player.startRiding(old, true);
            oldHorseUuid = old.getUUID();

            HHNBT ad = HHNBT.getPersistentData(attendant());
            Stock.setStock(level, ad, home(), 1);
            Purchasing.purchase(player, Stock.uuid(ad, 0), home());
        }

        @Override
        public int settleTicks() {
            return 2;
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            if (!Registration.isAssociated(player, oldHorseUuid)) {
                return "the horse being ridden was not stored into the registry on purchase";
            }
            if (level.getEntity(oldHorseUuid) != null) {
                return "the old horse is still in the world; a swapped-out mount should be removed";
            }
            return null;
        }
    }

    /** Buying a horse that is no longer in Stock spawns nothing and leaves the Stock intact. */
    private static class PurchaseFailure extends StockScenario {
        PurchaseFailure(BlockPos home) {
            super(home);
        }

        @Override
        public String name() {
            return "stock:purchase_of_missing_horse_does_nothing";
        }

        @Override
        public String expectation() {
            return "buying a horse that is not in stock spawns no horse and leaves the stock unchanged";
        }

        @Override
        protected void prepare(
                ServerLevel level,
                ServerPlayer player
        ) {
            HHNBT ad = HHNBT.getPersistentData(attendant());
            Stock.setStock(level, ad, home(), 1);
            // A UUID that is deliberately not the one in stock.
            Purchasing.purchase(player, UUID.randomUUID(), home());
        }

        @Override
        public int settleTicks() {
            return 2;
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            if (Stock.uuid(HHNBT.getPersistentData(attendant()), 0) == null) {
                return "the stock slot was emptied by a purchase that should have failed";
            }
            if (countHorsesNear(level, player) != 0) {
                return "a horse was spawned by a purchase that should have failed";
            }
            return null;
        }
    }

    /**
     * Spawns a {@link StableAttendant} where the fake player parked, on a small stone platform, and
     * cleans it (and any horses left over) up afterwards.
     */
    private abstract static class StockScenario implements Scenario {

        private final BlockPos home;
        private @Nullable StableAttendant attendant;

        StockScenario(BlockPos home) {
            this.home = home;
        }

        protected BlockPos home() {
            return home;
        }

        protected StableAttendant attendant() {
            if (attendant == null) {
                throw new IllegalStateException("scenario was checked before it was set up");
            }
            return attendant;
        }

        @Override
        public void setUp(
                ServerLevel level,
                ServerPlayer player
        ) {
            clearRegistry(player);
            removeHorsesNear(level, player);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(home.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                }
            }

            StableAttendant att = EntitiesInit.STABLE_ATTENDANT.get().create(level);
            if (att == null) {
                throw new IllegalStateException("could not create a stable attendant");
            }
            att.setPos(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            level.addFreshEntity(att);
            attendant = att;

            prepare(level, player);
            HorseHotel.LOGGER.info("[autotest] {} spawned attendant at {}", name(), att.position());
        }

        /** Runs after the Attendant exists, for scenarios that seed Stock or perform a purchase. */
        protected void prepare(
                ServerLevel level,
                ServerPlayer player
        ) {
        }

        @Override
        public void tearDown(
                ServerLevel level,
                ServerPlayer player
        ) {
            if (attendant != null) {
                attendant.discard();
                attendant = null;
            }
            if (player.getVehicle() != null) {
                player.stopRiding();
            }
            removeHorsesNear(level, player);
            clearRegistry(player);
        }

        protected int countHorsesNear(
                ServerLevel level,
                ServerPlayer player
        ) {
            return horsesNear(level, player).size();
        }

        protected static List<Horse> horsesNear(
                ServerLevel level,
                ServerPlayer player
        ) {
            return level.getEntitiesOfClass(Horse.class, new AABB(player.blockPosition()).inflate(6));
        }

        private static void removeHorsesNear(
                ServerLevel level,
                ServerPlayer player
        ) {
            for (Horse h : horsesNear(level, player)) {
                h.remove(Entity.RemovalReason.DISCARDED);
            }
        }

        private static void clearRegistry(ServerPlayer player) {
            HHNBT.getPersistentData(player).remove(HHNBT.Key.REGISTERED_HORSES);
        }
    }
}
