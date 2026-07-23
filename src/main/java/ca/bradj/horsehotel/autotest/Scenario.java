package ca.bradj.horsehotel.autotest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * One in-game check.
 * <p>
 * Scenarios are driven by real server ticks rather than by a loop, because the behaviour under
 * test only runs once every {@link ca.bradj.horsehotel.Config#horseRefreshInterval} ticks. A
 * scenario is set up, left alone for {@link #settleTicks()}, then checked.
 */
public interface Scenario {

    String name();

    /**
     * What this scenario asserts, in plain English. Echoed into the log beside the result so a
     * failure reads as a sentence rather than a symbol.
     */
    String expectation();

    void setUp(
            ServerLevel level,
            ServerPlayer player
    );

    /**
     * How many ticks to let pass between {@link #setUp} and {@link #check}.
     */
    int settleTicks();

    /**
     * @return null when the scenario passes, otherwise a description of what went wrong.
     */
    @Nullable String check(
            ServerLevel level,
            ServerPlayer player
    );

    void tearDown(
            ServerLevel level,
            ServerPlayer player
    );
}
