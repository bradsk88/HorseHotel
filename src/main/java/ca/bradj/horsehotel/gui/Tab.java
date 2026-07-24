package ca.bradj.horsehotel.gui;

import org.apache.logging.log4j.util.TriConsumer;

/**
 * One tab in the hotel UI. Ported from Questown. {@code renderFunc} draws the tab's icon given the
 * centre of the tab face (so the icon can centre itself); {@code onClick} runs when it is clicked;
 * {@code selected} marks the tab for the screen currently open.
 */
public record Tab(
        TriConsumer<RenderContext, Integer, Integer> renderFunc,
        Runnable onClick,
        String titleKey,
        boolean selected
) {
}
