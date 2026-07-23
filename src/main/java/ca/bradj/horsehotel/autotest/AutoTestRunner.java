package ca.bradj.horsehotel.autotest;

import ca.bradj.horsehotel.HorseHotel;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Runs the in-game suite on a headless, self-halting server:
 * <pre>
 *     ./gradlew runServer -Dhorsehotel.autotest=true
 * </pre>
 * Without that system property this does nothing at all, so a normal {@code runServer} is
 * unaffected. The suite exits the JVM with status 1 when anything fails, so gradle reports it.
 * <p>
 * Everything it checks is server-side state. That is enough for the visibility rules, which are
 * decided entirely on the server and synced to clients, but it means client-side rendering is
 * genuinely out of reach here.
 */
@Mod.EventBusSubscriber(modid = HorseHotel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AutoTestRunner {

    private static final String SYSPROP = "horsehotel.autotest";

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!"true".equals(System.getProperty(SYSPROP))) {
            return;
        }

        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            HorseHotel.LOGGER.error("[autotest] Overworld not available, aborting");
            server.halt(false);
            Runtime.getRuntime().halt(1);
            return;
        }

        HorseHotel.LOGGER.info("[autotest] BOOT_OK");

        GameProfile profile = new GameProfile(UUID.randomUUID(), "[AutoTest]");
        FakePlayer player = FakePlayerFactory.get(overworld, profile);
        overworld.addNewPlayer(player);

        // The fake player parks itself at the world spawn and stays there — its tick() is a no-op
        // and moveTo routes through a net handler that ignores everything. So rather than put it
        // where we want it, the scenarios are laid out around wherever it landed.
        BlockPos home = player.blockPosition();
        HorseHotel.LOGGER.info("[autotest] Fake player at {}", player.position());

        // Stand-ins only tick in a ticking chunk, and there is no real player to keep these
        // chunks loaded. TicketType.FORCED grants entity ticking, which is exactly what we need.
        for (BlockPos pos : StandInScenarios.positionsNeedingChunks(home)) {
            ChunkPos chunk = new ChunkPos(pos);
            overworld.setChunkForced(chunk.x, chunk.z, true);
            HorseHotel.LOGGER.info("[autotest] Force-loaded chunk {}", chunk);
        }

        ScenarioRunner runner = new ScenarioRunner(overworld, player, StandInScenarios.all(home));
        HorseHotel.LOGGER.info("[autotest] SUITE_START ({} scenarios)", runner.getTotal());
        MinecraftForge.EVENT_BUS.register(new AutoTestTickListener(server, runner));
    }

    static class AutoTestTickListener {

        private final MinecraftServer server;
        private final ScenarioRunner runner;

        AutoTestTickListener(
                MinecraftServer server,
                ScenarioRunner runner
        ) {
            this.server = server;
            this.runner = runner;
        }

        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (!runner.tick()) {
                return;
            }
            MinecraftForge.EVENT_BUS.unregister(this);
            reportAndShutdown();
        }

        private void reportAndShutdown() {
            int passed = runner.getPassed();
            int total = runner.getTotal();
            boolean allPassed = total > 0 && passed == total;

            HorseHotel.LOGGER.info("[autotest] ========================================");
            HorseHotel.LOGGER.info("[autotest] RESULT: {}/{} passed ({})",
                    passed, total, allPassed ? "ALL PASS" : "FAILURES"
            );
            HorseHotel.LOGGER.info("[autotest] ========================================");

            server.halt(false);
            if (!allPassed) {
                Runtime.getRuntime().halt(1);
            }
        }
    }
}
