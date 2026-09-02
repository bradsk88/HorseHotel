package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.compat.Compat;
import ca.bradj.horsehotel.network.UIStockHorse;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static ca.bradj.horsehotel.gui.GuiUtil.renderEntity;
import static ca.bradj.horsehotel.gui.PagedCardScreen.BIG_PADDING;
import static ca.bradj.horsehotel.gui.PagedCardScreen.Card;

/**
 * The "Horses for Sale" tab — a browse view of the Stable's Stock, cloned from
 * {@link HorseSummonScreen}. Clicking a card opens the {@link PurchaseScreen} for that horse rather
 * than buying outright, so the confirm step is the same however the player reached it.
 */
public class StockScreen extends Screen {

    private final List<UIStockHorse> stock;
    private final BlockPos stablePos;
    private final boolean yourHorsesAvailable;
    private final PagedCardScreen<UIStockHorse> delegate;
    private final ImmutableList<Entity> entities;
    private final Tabs tabs;

    public StockScreen(
            Collection<UIStockHorse> stock,
            boolean yourHorsesAvailable,
            BlockPos stablePos
    ) {
        super(Compat.literal(""));
        this.stock = ImmutableList.copyOf(stock);
        this.stablePos = stablePos;
        this.yourHorsesAvailable = yourHorsesAvailable;
        this.tabs = HotelTabs.forStock(stablePos, yourHorsesAvailable);

        this.delegate = new PagedCardScreen<>(
                () -> height,
                () -> width,
                () -> ImmutableList.copyOf(this.stock),
                horse -> {
                },
                (s, c, p) -> this.renderCardContent(s, c),
                1,
                0,
                this.stock.size() > 2 ? 20 + BIG_PADDING : 0
        );

        ImmutableList.Builder<Entity> entities = ImmutableList.builder();
        for (UIStockHorse h : this.stock) {
            Horse element = EntityType.HORSE.create(Minecraft.getInstance().level);
            element.deserializeNBT(h.horseData());
            element.setUUID(UUID.randomUUID());
            element.setYBodyRot(40);
            element.setYHeadRot(40);
            element.equipSaddle(null);
            entities.add(element);
        }
        this.entities = entities.build();
    }

    @Override
    protected void init() {
        super.init();
        this.delegate.afterInit(this::addRenderableWidget);

        // Mirror the "What is this?" link from the Your Horses tab so a player browsing the for-sale
        // list — who may have no registered horses yet — can still learn about the stable network.
        int pageTopY = ((this.height - delegate.backgroundHeight)) / 2;
        int pageLeftX = ((this.width - PagedCardScreen.backgroundWidth) / 2);
        int buttonHeight = (2 * font.lineHeight) + 2;
        int buttonWidth = (PagedCardScreen.backgroundWidth - (2 * BIG_PADDING));
        this.addRenderableWidget(Compat.button(
                pageLeftX + BIG_PADDING,
                pageTopY + delegate.backgroundHeight - buttonHeight - BIG_PADDING,
                buttonWidth,
                buttonHeight,
                Compat.translatable("horsehotel.menus.common.what_is_this"),
                (b) -> Minecraft.getInstance().setScreen(new WelcomeScreen())
        ));
    }

    @Override
    public void render(
            GuiGraphics gui,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        this.renderBackground(gui);
        this.delegate.renderBg(gui, mouseX, mouseY);
        int bgX = (this.width - PagedCardScreen.backgroundWidth) / 2;
        int bgY = (this.height - delegate.backgroundHeight) / 2;
        this.tabs.draw(gui, bgX, bgY);
        super.render(gui, mouseX, mouseY, partialTicks);
        this.delegate.afterRender(gui, mouseX, mouseY, stock, true);
        this.tabs.renderTooltip(bgX, bgY, mouseX, mouseY,
                key -> this.setTooltipForNextRenderPass(java.util.List.of(Compat.translatable(key).getVisualOrderText()), Compat.defaultTooltipPositioner(), false));
    }

    @Override
    public boolean mouseClicked(
            double x,
            double y,
            int button
    ) {
        int bgX = (this.width - PagedCardScreen.backgroundWidth) / 2;
        int bgY = (this.height - delegate.backgroundHeight) / 2;
        if (this.tabs.mouseClicked(bgX, bgY, x, y)) {
            return true;
        }
        Coordinate coord = new Coordinate((int) x, (int) y);
        for (Card<UIStockHorse> card : delegate.cards()) {
            if (GuiUtil.isCoordInBox(coord, card.coords().topLeft(), card.coords().bottomRight())) {
                UIStockHorse h = card.data();
                Minecraft.getInstance().setScreen(new PurchaseScreen(h.stockUuid(), h.horseData(), stablePos));
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(
            double x,
            double y,
            double delta
    ) {
        return this.delegate.mouseScrolled(
                x, y, delta,
                this::isMouseOver,
                (coord, tick) -> this.mouseScrolled(coord.x(), coord.y(), tick)
        );
    }

    private void renderCardContent(
            GuiGraphics gui,
            PagedCardScreen.Card<UIStockHorse> card
    ) {
        PagedCardScreen.CardCoordinates c = card.coords();
        renderEntity(entities.get(card.index()), gui, c.leftXPadded() + 10, c.topYPadded() + 4, 16);
        Component name = entities.get(card.index()).getCustomName();
        if (name == null) {
            name = Compat.translatable("horse");
        }
        Compat.drawDarkText(gui, name, c.leftXPadded() + 35, c.topYPadded());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public List<Rect2i> getExtraAreas() {
        return this.delegate.getExtraAreas();
    }
}
