package ca.bradj.horsehotel.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.ItemRenderer;

/**
 * The rendering handles a tab icon needs. Ported from Questown's tab UI.
 */
public record RenderContext(
        ItemRenderer itemRenderer,
        PoseStack stack
) {
}
