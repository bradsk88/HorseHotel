package ca.bradj.horsehotel.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;

/**
 * The rendering handles a tab icon needs. Ported from Questown's tab UI.
 */
public record RenderContext(
        ItemRenderer itemRenderer,
        GuiGraphics gui
) {
}
