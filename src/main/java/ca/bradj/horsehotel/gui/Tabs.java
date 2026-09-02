package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.compat.JEI;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import mezz.jei.api.gui.drawable.IDrawableStatic;

import java.util.function.Consumer;

/**
 * Renders a row of tabs above a hotel screen and routes clicks to them. Ported from Questown's
 * {@code Tabs}, trimmed to what the hotel UI needs (no notification dots).
 */
public class Tabs {
    private final ImmutableList<Tab> tabs;
    private final IDrawableStatic tab;
    private final IDrawableStatic unTab;

    private static final int X_OFFSET = 6;
    private static final int Y_OFFSET = 3;

    public Tabs(ImmutableList<Tab> tabs) {
        this.tabs = tabs;
        this.tab = JEI.getTabSelected();
        this.unTab = JEI.getTabUnselected();
    }

    public boolean renderTooltip(
            int bgX,
            int bgY,
            int mouseX,
            int mouseY,
            Consumer<String> renderFn
    ) {
        int tabTopY = bgY - this.unTab.getHeight() + Y_OFFSET;
        for (int i = 0; i < tabs.size(); i++) {
            int tabLeftX = bgX + (unTab.getWidth() * i) + X_OFFSET;
            if (GuiUtil.isCoordInBox(mouseX, mouseY, tabLeftX, tabTopY, tab.getWidth(), unTab.getHeight())) {
                renderFn.accept(tabs.get(i).titleKey());
                return true;
            }
        }
        return false;
    }

    public void draw(
            GuiGraphics gui,
            int bgX,
            int bgY
    ) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        RenderContext rc = new RenderContext(itemRenderer, gui);
        for (int i = 0; i < tabs.size(); i++) {
            IDrawableStatic drawable = tabs.get(i).selected() ? this.tab : this.unTab;
            int tabX = bgX + (drawable.getWidth() * i);
            int tabY = bgY - drawable.getHeight();
            drawable.draw(gui, tabX + X_OFFSET, tabY + Y_OFFSET);
            // Hand the icon the centre of the drawn tab face, so icons centre themselves regardless
            // of their own size.
            int centerX = tabX + X_OFFSET + drawable.getWidth() / 2;
            int centerY = tabY + Y_OFFSET + drawable.getHeight() / 2;
            tabs.get(i).renderFunc().accept(rc, centerX, centerY);
        }
    }

    public boolean mouseClicked(
            int bgX,
            int bgY,
            double mouseX,
            double mouseY
    ) {
        int tabsY = bgY - this.unTab.getHeight() + Y_OFFSET;
        for (int i = 0; i < tabs.size(); i++) {
            int tabLeftX = bgX + (unTab.getWidth() * i) + X_OFFSET;
            if (GuiUtil.isCoordInBox(mouseX, mouseY, tabLeftX, tabsY, tab.getWidth(), unTab.getHeight())) {
                tabs.get(i).onClick().run();
                return true;
            }
        }
        return false;
    }
}
