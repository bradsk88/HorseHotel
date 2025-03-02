package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.compat.Compat;
import ca.bradj.horsehotel.compat.JEI;
import ca.bradj.horsehotel.network.HHNetwork;
import ca.bradj.horsehotel.network.RegisterRiddenHorseMessage;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

import static ca.bradj.horsehotel.gui.GuiUtil.renderEntity;
import static ca.bradj.horsehotel.gui.PagedCardScreen.BIG_PADDING;
import static ca.bradj.horsehotel.gui.PagedCardScreen.MED_PADDING;

public class HorseRegisterScreen extends Screen {
    public static final Logger LOGGER = LogManager.getLogger();

    private final Horse element;
    private final JEI.NineNine background;
    private final int backgroundWidth = 300;
    private final int backgroundHeight = 166;

    public HorseRegisterScreen(
            CompoundTag ridingHorse
    ) {
        super(Compat.literal(""));
        element = makeEntityForRendering(ridingHorse);
        this.background = JEI.getRecipeBackground();
    }

    @SuppressWarnings("DataFlowIssue")
    private @NotNull Horse makeEntityForRendering(CompoundTag ridingHorse) {
        final Horse element;
        element = EntityType.HORSE.create(Minecraft.getInstance().level);
        element.deserializeNBT(ridingHorse);
        element.setUUID(UUID.randomUUID());
        element.setYBodyRot(40);
        element.setYHeadRot(40);
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
        int buttonWidth = (backgroundWidth - (3 * BIG_PADDING)) / 2;
        this.addRenderableWidget(
                new Button(
                        pageLeftX + BIG_PADDING,
                        pageTopY + backgroundHeight - buttonHeight - BIG_PADDING,
                        buttonWidth,
                        buttonHeight,
                        Compat.translatable("horsehotel.menus.common.register"),
                        (p_96776_) -> this.registerRidingHorse()
                )
        );
        this.addRenderableWidget(
                new Button(
                        pageLeftX + BIG_PADDING + buttonWidth + BIG_PADDING,
                        pageTopY + backgroundHeight - buttonHeight - BIG_PADDING,
                        buttonWidth,
                        buttonHeight,
                        Compat.translatable("horsehotel.menus.common.close"),
                        (p_96776_) -> ClientAccess.closeScreens()
                )
        );
    }

    private void registerRidingHorse() {
        RegisterRiddenHorseMessage msg = new RegisterRiddenHorseMessage();
        HHNetwork.CHANNEL.sendToServer(msg);
        ClientAccess.closeScreens();
    }

    @Override
    public void render(
            PoseStack stack,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        this.renderBackground(stack);

        int bgX = (this.width - backgroundWidth) / 2;
        int bgY = (this.height - backgroundHeight) / 2;
        this.background.draw(stack, bgX, bgY, backgroundWidth, backgroundHeight);
        super.render(stack, mouseX, mouseY, partialTicks);

        int horseSize = 32;
        int arbitraryNudge = 10;
        int horseX = bgX + arbitraryNudge + (backgroundWidth - horseSize) / 2;
        int horseY = bgY + horseSize + BIG_PADDING;

        renderEntity(element, stack, horseX, horseY, horseSize);

        int textX = bgX + MED_PADDING;
        int textY = horseY + horseSize + BIG_PADDING;

        MutableComponent unnamed = Component.translatable("horsehotel.menus.common.this_horse");
        Component name = element.getCustomName() == null ? unnamed : element.getCustomName();
        Component t1 = Compat.translatable("horsehotel.menus.register_horse.do_you_want_to_register", name);
        Component t2 = Compat.translatable("horsehotel.menus.register_horse.your_horse_will_be_kept_in_stable");
        Component t3 = Compat.translatable("horsehotel.menus.register_horse.also_other_stables");

        Compat.drawDarkText(font, stack, t1, textX, textY);
        Compat.drawDarkText(font, stack, t2, textX, textY += (int) (font.lineHeight * 1.75f));
        Compat.drawDarkText(font, stack, t3, textX, textY += (int) (font.lineHeight * 1.75f));
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