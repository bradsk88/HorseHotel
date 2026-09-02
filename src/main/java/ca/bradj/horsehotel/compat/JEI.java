package ca.bradj.horsehotel.compat;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.elements.DrawableNineSliceTexture;
import mezz.jei.gui.elements.GuiIconButton;
import mezz.jei.gui.input.MouseUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

public class JEI {
    public static IDrawableStatic getTabSelected() {
        return Internal.getTextures().getTabSelected();
    }

    public static IDrawableStatic getTabUnselected() {
        return Internal.getTextures().getTabUnselected();
    }

    public static NineNine getRecipeGuiBackground() {
        return new NineNine(Internal.getTextures().getRecipeGuiBackground());
    }

    public static NineNine getRecipeBackground() {
        return new NineNine(Internal.getTextures().getRecipeBackground());
    }

    public static IDrawableStatic getArrowNext() {
        return Internal.getTextures().getArrowNext();
    }

    public static IDrawableStatic getArrowPrevious() {
        return Internal.getTextures().getArrowPrevious();
    }

    public static IDrawableStatic getPlusIcon() {
        return Internal.getTextures().getRecipeTransfer();
    }

    public static GuiIconButton guiIconButtonSmall(
            int x, int y, int widthIn, int heightIn, IDrawable icon, Button.OnPress pressable
    ) {
        // 1.20.1: GuiIconButtonSmall became GuiIconButton and dropped the TextureAtlasSprite arg.
        return new GuiIconButton(
                x, y, widthIn, heightIn, icon, pressable
        );
    }

    public static double getX() {
        return MouseUtil.getX();
    }
    public static double getY() {
        return MouseUtil.getY();
    }

    public static IDrawableStatic getSlotDrawable() {
        return Internal.getTextures().getSlot();
    }

    public static class NineNine {

        private final DrawableNineSliceTexture delegate;

        public NineNine(DrawableNineSliceTexture input) {
            this.delegate = input;
        }

        public void draw(GuiGraphics gui, int x, int y, int backgroundWidth, int backgroundHeight) {
            // 1.20.1: DrawableNineSliceTexture.draw takes GuiGraphics + position + w/h.
            this.delegate.draw(gui, x, y, backgroundWidth, backgroundHeight);
        }
    }
}
