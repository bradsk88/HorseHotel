package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.network.ClientRunnable;
import ca.bradj.horsehotel.network.ShowHorseRegisterScreenMessage;
import ca.bradj.horsehotel.network.ShowHorseSummonScreenMessage;
import ca.bradj.horsehotel.network.ShowPurchaseScreenMessage;
import ca.bradj.horsehotel.network.ShowStockScreenMessage;
import ca.bradj.horsehotel.network.ShowWelcomeScreenMessage;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Function;

public class ClientAccess {

    private static final ImmutableMap<Class<? extends ClientRunnable>, Function<Object, Screen>> creators;

    static {
        ImmutableMap.Builder<Class<? extends ClientRunnable>, Function<Object, Screen>> b = ImmutableMap.builder();
        b.put(
                ShowHorseSummonScreenMessage.class,
                (Object msg) -> {
                    ShowHorseSummonScreenMessage m = (ShowHorseSummonScreenMessage) msg;
                    return new HorseSummonScreen(m.horses(), m.forSaleAvailable(), m.stablePos());
                }
        );
        b.put(
                ShowStockScreenMessage.class,
                (Object msg) -> {
                    ShowStockScreenMessage m = (ShowStockScreenMessage) msg;
                    return new StockScreen(m.stock(), m.yourHorsesAvailable(), m.stablePos());
                }
        );
        b.put(
                ShowPurchaseScreenMessage.class,
                (Object msg) -> {
                    ShowPurchaseScreenMessage m = (ShowPurchaseScreenMessage) msg;
                    return new PurchaseScreen(m.stockUuid, m.horseData, m.stablePos);
                }
        );
        b.put(
                ShowHorseRegisterScreenMessage.class,
                (Object msg) -> new HorseRegisterScreen(((ShowHorseRegisterScreenMessage) msg).horseData)
        );
        b.put(
                ShowWelcomeScreenMessage.class,
                (Object msg) -> new WelcomeScreen()
        );
        creators = b.build();
    }

    @SuppressWarnings({"DataFlowIssue"})
    public static <X extends ClientRunnable> void openScreenForMessage(X msg) {
        Minecraft.getInstance().setScreen(creators.get(msg.getClass()).apply(msg));
    }

    public static void closeScreens() {
        Minecraft.getInstance().setScreen(null);
    }
}
