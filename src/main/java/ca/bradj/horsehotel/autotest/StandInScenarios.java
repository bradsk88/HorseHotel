package ca.bradj.horsehotel.autotest;

import ca.bradj.horsehotel.Config;
import ca.bradj.horsehotel.HHNBT;
import ca.bradj.horsehotel.HorseHotel;
import ca.bradj.horsehotel.NotHorseEntity;
import ca.bradj.horsehotel.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Checks that a stand-in's visibility is decided by the stable, and decided early enough.
 * <p>
 * {@link HiddenBeforeFirstTick} is the regression test for the reported bug — it is the only one
 * here that fails against the code before the fix. The others are guard rails: they pin the
 * behaviour at a distance and the reveal path, both of which the fix reworked.
 * <p>
 * The fake player never moves. A {@code ServerPlayer} positions itself at the world spawn when it
 * is constructed, and {@code moveTo} on a fake player routes through a net handler whose methods
 * are all no-ops, so trying to relocate it is unreliable. Instead every position here is derived
 * from wherever the player actually landed, which also makes the suite independent of world type
 * and seed.
 */
public final class StandInScenarios {

    /** Comfortably inside the eval radius, and inside the old 16-block radius too. */
    private static final int NEAR = 8;

    /** Inside the eval radius but well outside the old 16-block radius. */
    private static final int MID = 40;

    /** Far outside any eval radius, so no player is in range at all. */
    private static final int FAR = 2000;

    private StandInScenarios() {
    }

    /**
     * Every position whose chunk must be force-loaded. Stand-ins only tick in a ticking chunk, and
     * there is no real player to keep these chunks alive.
     */
    public static List<BlockPos> positionsNeedingChunks(BlockPos home) {
        return List.of(home, standInPos(home, NEAR), standInPos(home, MID), standInPos(home, FAR));
    }

    public static List<Scenario> all(BlockPos home) {
        return List.of(
                new HiddenBeforeFirstTick(home),
                new HiddenWithNoPlayerInRange(home),
                new HiddenWithEmptyRegistry(home),
                new VisibleWhenRegistered(home)
        );
    }

    /**
     * A stand-in must be hidden from the moment it exists, before it has ticked even once.
     * <p>
     * This is the regression test for the reported bug. Clients are sent a stand-in as soon as
     * they come within tracking range, which does not wait for the entity to start ticking — so a
     * stand-in that only becomes hidden in {@code tick()} is briefly a visible, variant-0 (white)
     * horse. That is what players saw in the distance, and why it vanished as they got closer and
     * the chunk began to tick.
     */
    private static class HiddenBeforeFirstTick extends StandInScenario {

        HiddenBeforeFirstTick(BlockPos home) {
            super(home, NEAR);
        }

        @Override
        public String name() {
            return "standin:hidden_before_first_tick";
        }

        @Override
        public String expectation() {
            return "a stand-in is hidden from the moment it is added, before it has ticked";
        }

        @Override
        public int settleTicks() {
            return 0;
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            NotHorseEntity s = standIn();
            if (s.tickCount > 0) {
                return String.format(
                        "stand-in already ticked %d times, so this no longer tests the pre-tick state",
                        s.tickCount
                );
            }
            if (!s.isInvisible()) {
                return "a freshly added stand-in is visible, so clients can be sent a white horse"
                        + " before the stable has hidden it";
            }
            return null;
        }
    }

    private static BlockPos standInPos(
            BlockPos home,
            int blocksEast
    ) {
        return home.offset(blocksEast, 0, 0);
    }

    /**
     * A stand-in with nobody in range must stay hidden. Before the fix this failed: vanilla cleared
     * the invisible flag every tick and the early return meant nothing set it back.
     */
    private static class HiddenWithNoPlayerInRange extends StandInScenario {

        HiddenWithNoPlayerInRange(BlockPos home) {
            super(home, FAR);
        }

        @Override
        public String name() {
            return "standin:hidden_with_no_player_in_range";
        }

        @Override
        public String expectation() {
            return "a stand-in with no player within the eval radius stays hidden";
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            return requireHidden(player);
        }
    }

    /**
     * A stand-in 40 blocks from a player who has registered nothing must be hidden. This is the
     * exact reported symptom: 40 blocks is outside the old 16-block radius but well inside the
     * distance at which a client is sent the entity.
     */
    private static class HiddenWithEmptyRegistry extends StandInScenario {

        HiddenWithEmptyRegistry(BlockPos home) {
            super(home, MID);
        }

        @Override
        public String name() {
            return "standin:hidden_with_empty_registry_at_40_blocks";
        }

        @Override
        public String expectation() {
            return "a stand-in 40 blocks from a player with an empty registry stays hidden";
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            return requireHidden(player);
        }
    }

    /**
     * The positive control. Without it, hiding every stand-in unconditionally would pass the suite.
     */
    private static class VisibleWhenRegistered extends StandInScenario {

        VisibleWhenRegistered(BlockPos home) {
            super(home, NEAR);
        }

        @Override
        public String name() {
            return "standin:visible_when_registered";
        }

        @Override
        public String expectation() {
            return "a stand-in shows the claimant's registered horse";
        }

        @Override
        protected void prepare(
                ServerLevel level,
                ServerPlayer player
        ) {
            Horse horse = EntityType.HORSE.create(level);
            if (horse == null) {
                throw new IllegalStateException("could not create a horse to register");
            }
            // Deliberately never added to the level: a real horse standing next to its stand-in
            // makes shouldHide() hide the stand-in, which is correct behaviour but not what this
            // scenario is checking.
            Registration.storeHorseOnPlayer(player, horse);
        }

        @Override
        public @Nullable String check(
                ServerLevel level,
                ServerPlayer player
        ) {
            @Nullable String unsound = requireSoundSetup(player);
            if (unsound != null) {
                return unsound;
            }
            NotHorseEntity s = standIn();
            if (s.isInvisible()) {
                return String.format(
                        "stand-in is hidden despite a registered horse (%.1f blocks from the player)",
                        Math.sqrt(player.distanceToSqr(s))
                );
            }
            if (!HHNBT.getPersistentData(s).contains(HHNBT.Key.REAL_HORSE_UUID)) {
                return "stand-in is visible but never took on the registered horse's data";
            }
            return null;
        }
    }

    /**
     * Spawns a stand-in on a small stone platform, a fixed distance east of the fake player, and
     * cleans it up afterwards.
     */
    private abstract static class StandInScenario implements Scenario {

        private final BlockPos pos;
        private final int blocksAway;
        private @Nullable NotHorseEntity standIn;

        StandInScenario(
                BlockPos home,
                int blocksAway
        ) {
            this.blocksAway = blocksAway;
            this.pos = standInPos(home, blocksAway);
        }

        @Override
        public void setUp(
                ServerLevel level,
                ServerPlayer player
        ) {
            clearRegistry(player);
            prepare(level, player);

            // Something solid to stand on. Stand-ins are subject to gravity, and a scenario with
            // nobody in range never reaches the code that re-anchors them.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(pos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                }
            }

            NotHorseEntity horse = new NotHorseEntity(level, 0);
            HHNBT.getPersistentData(horse).put(HHNBT.Key.REGISTERED_HORSE_INDEX, 0);
            horse.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(horse);
            standIn = horse;

            HorseHotel.LOGGER.info(
                    "[autotest] {} spawned stand-in at {}, {} blocks from the player",
                    name(), horse.position(), blocksAway
            );
        }

        /**
         * Runs before the stand-in exists, for scenarios that need to set the player up first.
         */
        protected void prepare(
                ServerLevel level,
                ServerPlayer player
        ) {
        }

        @Override
        public int settleTicks() {
            // One full refresh, plus room for the tick the evaluation actually lands on.
            return Math.max(1, Config.horseRefreshInterval) + 10;
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
            clearRegistry(player);
        }

        protected NotHorseEntity standIn() {
            if (standIn == null) {
                throw new IllegalStateException("scenario was checked before it was set up");
            }
            return standIn;
        }

        /**
         * Anything that has moved since set-up invalidates the scenario rather than failing it
         * honestly — a stand-in that has fallen out of the eval radius stays hidden no matter what
         * the code does, so a "hidden" assertion would pass vacuously. Check the setup is still
         * sound before checking the behaviour.
         */
        protected @Nullable String requireSoundSetup(ServerPlayer player) {
            NotHorseEntity s = standIn();
            if (s.isRemoved()) {
                return "stand-in was removed from the world before it could be checked";
            }
            double standInDrift = s.position().distanceTo(Vec3.atBottomCenterOf(pos));
            if (standInDrift > 2.0) {
                return String.format(
                        "stand-in drifted %.1f blocks, from %s to %s — this scenario proves nothing",
                        standInDrift, pos.toShortString(), s.position()
                );
            }
            double actualDistance = Math.sqrt(player.distanceToSqr(s));
            if (Math.abs(actualDistance - blocksAway) > 2.0) {
                return String.format(
                        "stand-in is %.1f blocks from the player, not the %d this scenario assumes"
                                + " — this scenario proves nothing",
                        actualDistance, blocksAway
                );
            }
            return null;
        }

        protected @Nullable String requireHidden(ServerPlayer player) {
            @Nullable String unsound = requireSoundSetup(player);
            if (unsound != null) {
                return unsound;
            }
            if (!standIn().isInvisible()) {
                return String.format(
                        "stand-in is visible %d blocks from the player (eval radius is %d)",
                        blocksAway, Config.horseEvalRadius
                );
            }
            return null;
        }

        private static void clearRegistry(ServerPlayer player) {
            HHNBT.getPersistentData(player).remove(HHNBT.Key.REGISTERED_HORSES);
        }
    }
}
