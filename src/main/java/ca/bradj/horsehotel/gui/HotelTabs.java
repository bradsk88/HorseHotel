package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.HorseHotel;
import ca.bradj.horsehotel.compat.Compat;
import ca.bradj.horsehotel.network.HHNetwork;
import ca.bradj.horsehotel.network.OpenHotelMessage;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds the two hotel-UI tabs — "Your Horses" (the player's own face) and "Horses for Sale" (a
 * cowboy hat) — for whichever screen is currently open. A tab is omitted rather than shown disabled
 * when it has nothing behind it: no Stock hides the For Sale tab, an empty Registry hides Your
 * Horses. Clicking an unselected tab round-trips an {@link OpenHotelMessage}.
 */
public class HotelTabs {

    private static final ResourceLocation COWBOY_HAT =
            ResourceLocation.tryBuild(HorseHotel.MODID, "textures/menu/cowboy_hat.png");

    /** Tab icon size in px — roughly double the original 9, so the icon reads clearly in the tab. */
    private static final int ICON_SIZE = 16;

    public static Tabs forSummon(
            BlockPos stablePos,
            boolean forSaleAvailable
    ) {
        ImmutableList.Builder<Tab> b = ImmutableList.builder();
        b.add(yourHorses(stablePos, true));
        if (forSaleAvailable) {
            b.add(forSale(stablePos, false));
        }
        return new Tabs(b.build());
    }

    public static Tabs forStock(
            BlockPos stablePos,
            boolean yourHorsesAvailable
    ) {
        ImmutableList.Builder<Tab> b = ImmutableList.builder();
        if (yourHorsesAvailable) {
            b.add(yourHorses(stablePos, false));
        }
        b.add(forSale(stablePos, true));
        return new Tabs(b.build());
    }

    private static Tab yourHorses(
            BlockPos stablePos,
            boolean selected
    ) {
        return new Tab(
                (rc, cx, cy) -> {
                    var player = Minecraft.getInstance().player;
                    if (player == null) {
                        return;
                    }
                    RenderSystem.setShaderTexture(0, player.getSkinTextureLocation());
                    PlayerFaceRenderer.draw(rc.gui(), player.getSkinTextureLocation(), cx - ICON_SIZE / 2, cy - ICON_SIZE / 2, ICON_SIZE);
                },
                open(stablePos, OpenHotelMessage.YOUR_HORSES, selected),
                "horsehotel.menus.tabs.your_horses",
                selected
        );
    }

    private static Tab forSale(
            BlockPos stablePos,
            boolean selected
    ) {
        return new Tab(
                (rc, cx, cy) -> {
                    RenderSystem.setShaderTexture(0, COWBOY_HAT);
                    Compat.blit(rc.gui(), COWBOY_HAT, cx - ICON_SIZE / 2, cy - ICON_SIZE / 2,
                            ICON_SIZE, ICON_SIZE, 0, 0, 16, 16, 16, 16);
                },
                open(stablePos, OpenHotelMessage.FOR_SALE, selected),
                "horsehotel.menus.tabs.for_sale",
                selected
        );
    }

    private static Runnable open(
            BlockPos stablePos,
            String tab,
            boolean selected
    ) {
        if (selected) {
            return () -> {
            };
        }
        return () -> HHNetwork.CHANNEL.sendToServer(new OpenHotelMessage(stablePos, tab));
    }
}
