package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.compat.Compat;
import ca.bradj.horsehotel.compat.JEI;
import ca.bradj.horsehotel.network.HHNetwork;
import ca.bradj.horsehotel.network.PurchaseHorseMessage;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ca.bradj.horsehotel.gui.GuiUtil.renderEntity;
import static ca.bradj.horsehotel.gui.PagedCardScreen.BIG_PADDING;

/**
 * Confirms buying one Stock horse — the purchase counterpart of {@link HorseRegisterScreen}. The
 * horse is rendered large; "Buy" quotes the Stock UUID back to the server, "Close" backs out.
 */
public class PurchaseScreen extends Screen {

    private final UUID stockUuid;
    private final BlockPos stablePos;
    private final Horse element;
    private final JEI.NineNine background;
    private Button helpButton;
    // Questown's confirm screens use the standard 176-wide recipe background; 300 was too wide for
    // the short prompt and left the content looking stranded.
    private final int backgroundWidth = 176;
    private final int backgroundHeight = 166;

    public PurchaseScreen(
            UUID stockUuid,
            CompoundTag horseData,
            BlockPos stablePos
    ) {
        super(Compat.literal(""));
        this.stockUuid = stockUuid;
        this.stablePos = stablePos;
        this.element = makeEntityForRendering(horseData);
        this.background = JEI.getRecipeBackground();
    }

    @SuppressWarnings("DataFlowIssue")
    private @NotNull Horse makeEntityForRendering(CompoundTag horseData) {
        Horse element = EntityType.HORSE.create(Minecraft.getInstance().level);
        element.deserializeNBT(horseData);
        element.setUUID(UUID.randomUUID());
        element.setYBodyRot(40);
        element.setYHeadRot(40);
        element.equipSaddle(null);
        return element;
    }

    @Override
    public boolean keyReleased(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode == GLFW.GLFW_KEY_Q) { // TODO: Get from user's config
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        super.init();
        int pageTopY = ((this.height - backgroundHeight)) / 2;
        int pageLeftX = ((this.width - backgroundWidth) / 2);
        int buttonHeight = (2 * font.lineHeight) + 2;
        // Bottom row: Buy and Close split the width.
        int buttonWidth = (backgroundWidth - (3 * BIG_PADDING)) / 2;
        int rowY = pageTopY + backgroundHeight - buttonHeight - BIG_PADDING;
        this.addRenderableWidget(
                Compat.button(
                        pageLeftX + BIG_PADDING, rowY, buttonWidth, buttonHeight,
                        Compat.translatable("horsehotel.menus.purchase.buy"),
                        (b) -> this.buy()
                )
        );
        this.addRenderableWidget(
                Compat.button(
                        pageLeftX + BIG_PADDING + buttonWidth + BIG_PADDING, rowY, buttonWidth, buttonHeight,
                        Compat.translatable("horsehotel.menus.common.close"),
                        (b) -> ClientAccess.closeScreens()
                )
        );
        // A "?" help icon tucked into the top-right corner, beside the horse. A player whose first
        // interaction is right-clicking a for-sale horse skips the For Sale list (and its link), so
        // surface the same network explainer here. Its tooltip is drawn at the end of render().
        // Full 20px button height (buttonHeight == 2*lineHeight+2) — the widget texture is 20px
        // tall, so a shorter button would clip its bottom outline.
        int helpSize = buttonHeight;
        int cornerInset = 4;
        this.helpButton = Compat.button(
                pageLeftX + backgroundWidth - helpSize - cornerInset,
                pageTopY + cornerInset,
                helpSize, helpSize,
                Compat.literal("?"),
                (b) -> Minecraft.getInstance().setScreen(new WelcomeScreen())
        );
        this.addRenderableWidget(this.helpButton);
    }

    private void buy() {
        HHNetwork.CHANNEL.sendToServer(new PurchaseHorseMessage(stockUuid, stablePos));
        ClientAccess.closeScreens();
    }

    @Override
    public void render(
            GuiGraphics gui,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        this.renderBackground(gui);

        int bgX = (this.width - backgroundWidth) / 2;
        int bgY = (this.height - backgroundHeight) / 2;
        this.background.draw(gui, bgX, bgY, backgroundWidth, backgroundHeight);
        super.render(gui, mouseX, mouseY, partialTicks);

        int horseSize = 32;
        // Centre the horse in the background. renderEntity draws the horse around xPos + 8.
        int horseX = bgX + (backgroundWidth / 2) - 8;
        int horseY = bgY + horseSize + BIG_PADDING;

        renderEntity(element, gui, horseX, horseY, horseSize);

        MutableComponent unnamed = Component.translatable("horsehotel.menus.common.this_horse");
        Component name = element.getCustomName() == null ? unnamed : element.getCustomName();
        Component t1 = Compat.translatable("horsehotel.menus.purchase.do_you_want_to_buy", name);
        Component t2 = Compat.translatable("horsehotel.menus.purchase.it_is_free_for_now");

        // Wrap the prompt to the background width and centre every line horizontally.
        int textWidth = backgroundWidth - (2 * BIG_PADDING);
        int textY = horseY + horseSize + BIG_PADDING;
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(t1, textWidth));
        lines.addAll(font.split(t2, textWidth));
        for (FormattedCharSequence line : lines) {
            int lineX = bgX + (backgroundWidth - font.width(line)) / 2;
            Compat.drawDarkText(gui, line, lineX, textY);
            textY += (int) (font.lineHeight * 1.5f);
        }

        // Drawn last so the "?" tooltip renders above the horse and prompt text.
        if (this.helpButton != null && this.helpButton.isMouseOver(mouseX, mouseY)) {
            this.setTooltipForNextRenderPass(
                    java.util.List.of(Compat.translatable("horsehotel.menus.common.what_is_this").getVisualOrderText()),
                    Compat.defaultTooltipPositioner(),
                    false
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public List<Rect2i> getExtraAreas() {
        int x = (this.width - backgroundWidth) / 2;
        int y = (this.height - backgroundHeight) / 2;
        return ImmutableList.of(new Rect2i(x, y, backgroundWidth, backgroundHeight));
    }
}
