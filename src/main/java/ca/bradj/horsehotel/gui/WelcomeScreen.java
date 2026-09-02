package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.HorseHotel;
import ca.bradj.horsehotel.compat.Compat;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.common.util.ImmutableRect2i;
import mezz.jei.common.util.MathUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static ca.bradj.horsehotel.gui.PagedCardScreen.Card;

public class WelcomeScreen extends Screen {
    public static final Logger LOGGER = LogManager.getLogger();

    private final PagedCardScreen<WelcomePage> delegate;

    private static final ImmutableList<WelcomePage> PAGES;

    static {
        ImmutableList.Builder<WelcomePage> b = ImmutableList.builder();
        b.add(new WelcomePage(ImmutableList.of(
                new TxKeyOrImage(false, "horsehotel.menus.welcome.welcome"),
                new TxKeyOrImage(true, "textures/menu/overhead_shot.png"),
                new TxKeyOrImage(false, "horsehotel.menus.welcome.horsehotel_network")
        )));
        b.add(new WelcomePage(ImmutableList.of(
                new TxKeyOrImage(false, "horsehotel.menus.welcome.to_register_with_mob"),
                new TxKeyOrImage(true, "textures/menu/register_with_mob.png")
        )));
        b.add(new WelcomePage(ImmutableList.of(
                new TxKeyOrImage(false, "horsehotel.menus.welcome.to_register_with_block"),
                new TxKeyOrImage(true, "textures/menu/register_with_block.png")
        )));
        b.add(new WelcomePage(ImmutableList.of(
                new TxKeyOrImage(false, "horsehotel.menus.welcome.you_can_summon"),
                new TxKeyOrImage(true, "textures/menu/summon_menu_example.png"),
                new TxKeyOrImage(false, "horsehotel.menus.welcome.how_to_summon")
        )));

        PAGES = b.build();
    }

    public WelcomeScreen() {
        super(Compat.literal(""));

        this.delegate = new PagedCardScreen<>(
                () -> height,
                () -> width,
                () -> PAGES,
                horse -> {
                },
                (s, c, p) -> this.renderCardContent(s, c, new Coordinate(p.x(), p.y())),
                2,
                0,
                -40
        );
    }

    @Override
    public boolean keyReleased(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        return delegate.keyReleased(keyCode) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        super.init();
        this.delegate.afterInit(this::addRenderableWidget);
    }

    @Override
    public void render(
            GuiGraphics gui,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        this.delegate.renderBg(gui, mouseX, mouseY);
        super.render(gui, mouseX, mouseY, partialTicks);
        this.delegate.afterRender(gui, mouseX, mouseY, PAGES, false);
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
            GuiGraphics gui,
            Card<WelcomePage> card,
            Coordinate mouse
    ) {
        var font = Compat.font();
        PagedCardScreen.CardCoordinates c = card.coords();
        ImmutableList<TxKeyOrImage> content = card.data().content();
        int screenX = c.leftX();
        int screenY = c.topY();
        for (int i = 0; i < content.size(); i++) {
            TxKeyOrImage iContent = content.get(i);
            if (iContent.isImage()) {
                int firstPixelInFileX = 0;
                int firstPixelInFileY = 0;
                int renderWidth = 160;
                int renderHeight = 40;
                int widthOfEntireFile = renderWidth * 3;
                int heightOfEntireFile = renderHeight * 3;
                int bgX = (width - renderWidth) / 2;
                RenderSystem.setShaderTexture(0, ResourceLocation.tryBuild(HorseHotel.MODID, iContent.value()));
                Compat.blit(
                        gui,
                        ResourceLocation.tryBuild(HorseHotel.MODID, iContent.value()),
                        bgX,
                        screenY,
                        renderWidth,
                        renderHeight,
                        firstPixelInFileX,
                        firstPixelInFileY,
                        widthOfEntireFile,
                        heightOfEntireFile,
                        widthOfEntireFile,
                        heightOfEntireFile
                );
                screenY += (int) (renderHeight + (font.lineHeight * 0.5f));
                continue;
            }

            Component text = Compat.translatable(iContent.value());
            if (card.index() == 0 && i == 0) {
                ImmutableRect2i pageArea = MathUtil.union(delegate.previousPage.getArea(), delegate.nextPage.getArea());
                pageArea = pageArea.moveDown(font.lineHeight * 2);
                ImmutableRect2i textArea = MathUtil.centerTextArea(pageArea, font, text.getString());
                Compat.drawDarkText(gui, text, textArea.getX(), textArea.getY());
                screenY += font.lineHeight;
                continue;
            }

            java.util.List<net.minecraft.util.FormattedCharSequence> split = font.split(text, c.rightX() - c.leftX());
            for (var t : split) {
                Compat.drawDarkText(gui, t, screenX, screenY);
                screenY += (int) (font.lineHeight * 1.5);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public List<Rect2i> getExtraAreas() {
        return this.delegate.getExtraAreas();
    }
}