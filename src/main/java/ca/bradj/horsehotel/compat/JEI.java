package ca.bradj.horsehotel.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.elements.DrawableNineSliceTexture;
import mezz.jei.gui.elements.GuiIconButtonSmall;
import mezz.jei.gui.input.MouseUtil;
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

    public static GuiIconButtonSmall guiIconButtonSmall(
            int x, int y, int widthIn, int heightIn, IDrawable icon, Button.OnPress pressable
    ) {
        return new GuiIconButtonSmall(
                x, y, widthIn, heightIn, icon, pressable, Internal.getTextures()
        );
    }

    public static double getX() {
        return MouseUtil.getX();
    }
    public static double getY() {
        return MouseUtil.getY();
    }

    public static IDrawableStatic getSlotDrawable() {
        return Internal.getTextures().getSlotDrawable();
    }

    public static class NineNine {

        private final DrawableNineSliceTexture delegate;

        public NineNine(DrawableNineSliceTexture input) {
            this.delegate = input;
        }

        public void draw(PoseStack stack, int x, int y, int backgroundWidth, int backgroundHeight) {
            this.delegate.draw(stack, x, y, backgroundWidth, backgroundHeight);
        }
    }
}
