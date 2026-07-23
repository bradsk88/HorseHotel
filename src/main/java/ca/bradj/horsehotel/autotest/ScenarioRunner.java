package ca.bradj.horsehotel.autotest;

import ca.bradj.horsehotel.HorseHotel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Walks a list of {@link Scenario}s one at a time, advancing on server ticks. A scenario that
 * throws is reported as a failure rather than taking the server down, so one broken scenario
 * cannot hide the results of the others.
 */
class ScenarioRunner {

    private final ServerLevel level;
    private final ServerPlayer player;
    private final List<Scenario> scenarios;

    private int index = 0;
    private int ticksSinceSetUp = -1;
    private int passed = 0;

    ScenarioRunner(
            ServerLevel level,
            ServerPlayer player,
            List<Scenario> scenarios
    ) {
        this.level = level;
        this.player = player;
        this.scenarios = scenarios;
    }

    /**
     * @return true once every scenario has been checked.
     */
    boolean tick() {
        if (index >= scenarios.size()) {
            return true;
        }

        Scenario s = scenarios.get(index);

        if (ticksSinceSetUp < 0) {
            try {
                s.setUp(level, player);
            } catch (RuntimeException e) {
                HorseHotel.LOGGER.error("[autotest] {} [FAIL] {}: set-up threw {}", s.name(), s.expectation(), e);
                return advance();
            }
            ticksSinceSetUp = 0;
            // settleTicks() == 0 means "check in the same tick it was set up", before anything
            // spawned during set-up has had a chance to tick.
            if (s.settleTicks() > 0) {
                return false;
            }
        } else if (ticksSinceSetUp++ < s.settleTicks()) {
            return false;
        }

        @Nullable String failure;
        try {
            failure = s.check(level, player);
        } catch (RuntimeException e) {
            failure = "check threw " + e;
        }

        if (failure == null) {
            passed++;
            HorseHotel.LOGGER.info("[autotest] {} [PASS] {}", s.name(), s.expectation());
        } else {
            HorseHotel.LOGGER.error("[autotest] {} [FAIL] {}: {}", s.name(), s.expectation(), failure);
        }

        try {
            s.tearDown(level, player);
        } catch (RuntimeException e) {
            HorseHotel.LOGGER.warn("[autotest] {} tear-down threw {}", s.name(), e);
        }

        return advance();
    }

    private boolean advance() {
        index++;
        ticksSinceSetUp = -1;
        return index >= scenarios.size();
    }

    int getPassed() {
        return passed;
    }

    int getTotal() {
        return scenarios.size();
    }
}
