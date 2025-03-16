package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.compat.Compat;
import ca.bradj.horsehotel.network.HHNetwork;
import ca.bradj.horsehotel.network.SummonHorseMessage;
import ca.bradj.horsehotel.network.UIHorse;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static ca.bradj.horsehotel.gui.GuiUtil.renderEntity;
import static ca.bradj.horsehotel.gui.PagedCardScreen.BIG_PADDING;
import static ca.bradj.horsehotel.gui.PagedCardScreen.Card;

public class HorseSummonScreen extends Screen {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final int EXTRA_HEIGHT = 56;
    private final List<UIHorse> horses;
    private final PagedCardScreen<UIHorse> delegate;
    private final ImmutableList<Entity> entities;

    public HorseSummonScreen(
            Collection<UIHorse> horses
    ) {
        super(Compat.translatable("menu.work_add_confirm.title"));

        this.delegate = new PagedCardScreen<>(
                () -> height,
                () -> width,
                () -> ImmutableList.copyOf(horses),
                horse -> {
                },
                (s, c, p) -> this.renderCardContent(s, c, new ca.bradj.horsehotel.gui.Coordinate(p.x(), p.y())),
                1,
                0,
                horses.size() > 2 ? 20 : 0
        );
        this.horses = ImmutableList.copyOf(horses);
        ImmutableList.Builder<Entity> entities = ImmutableList.builder();
        for (int i = 0; i < this.horses.size(); i++) {
            Horse element = EntityType.HORSE.create(Minecraft.getInstance().level);
            element.deserializeNBT(this.horses.get(i).horseData());
            element.setUUID(UUID.randomUUID());
            element.setYBodyRot(40);
            element.setYHeadRot(40);
            entities.add(element);
        }
        this.entities = entities.build();
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
        if (entities.isEmpty()) {
            Minecraft.getInstance().setScreen(new WelcomeScreen());
            return;
        }
        super.init();
        this.delegate.afterInit(this::addRenderableWidget);

        int pageTopY = ((this.height - delegate.backgroundHeight)) / 2;
        int pageLeftX = ((this.width - PagedCardScreen.backgroundWidth) / 2);
        int buttonHeight = (2 * font.lineHeight) + 2;
        int buttonWidth = (PagedCardScreen.backgroundWidth - (2 * BIG_PADDING));
        this.addRenderableWidget(new Button(
                pageLeftX + BIG_PADDING,
                pageTopY + delegate.backgroundHeight - buttonHeight - BIG_PADDING,
                buttonWidth,
                buttonHeight,
                Compat.translatable("horsehotel.menus.common.what_is_this"),
                (p_96776_) -> Minecraft.getInstance().setScreen(new WelcomeScreen())
        ));
    }

    @Override
    public void render(
            PoseStack stack,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        this.renderBackground(stack);
        this.delegate.renderBg(stack, partialTicks, mouseX, mouseY);
        super.render(stack, mouseX, mouseY, partialTicks);
        this.delegate.afterRender(stack, font, mouseX, mouseY, partialTicks, horses, true);
    }

    @Override
    public boolean mouseClicked(
            double x,
            double y,
            int p_94697_
    ) {
        Coordinate coord = new Coordinate((int) x, (int) y);
        for (Card<UIHorse> card : delegate.cards()) {
            if (GuiUtil.isCoordInBox(coord, card.coords().topLeft(), card.coords().bottomRight())) {
                SummonHorseMessage m = new SummonHorseMessage(card.data().horseIndex());
                HHNetwork.CHANNEL.sendToServer(m);
                ClientAccess.closeScreens();
                return true;
            }
        }
        return super.mouseClicked(x, y, p_94697_);
    }


    @Override
    public boolean mouseScrolled(
            double p_94686_,
            double p_94687_,
            double p_94688_
    ) {
        return this.delegate.mouseScrolled(
                p_94686_,
                p_94687_,
                p_94688_,
                this::isMouseOver,
                (coord, tick) -> this.mouseScrolled(coord.x(), coord.y(), tick)
        );
    }

    private void renderCardContent(
            PoseStack stack,
            PagedCardScreen.Card<UIHorse> card,
            ca.bradj.horsehotel.gui.Coordinate mouse
    ) {
        PagedCardScreen.CardCoordinates c = card.coords();
        renderEntity(entities.get(card.index()), stack, c.leftXPadded() + 10, c.topYPadded() + 4, 16);
        Component name = entities.get(card.index()).getCustomName();
        if (name == null) {
            name = Compat.translatable("horse");
        }
        Compat.drawDarkText(font, stack, name, c.leftXPadded() + 35, c.topYPadded());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public List<Rect2i> getExtraAreas() {
        return this.delegate.getExtraAreas();
    }
}