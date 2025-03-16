package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.network.ClientRunnable;
import ca.bradj.horsehotel.network.ShowHorseRegisterScreenMessage;
import ca.bradj.horsehotel.network.ShowHorseSummonScreenMessage;
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
                (Object msg) -> new HorseSummonScreen(((ShowHorseSummonScreenMessage) msg).horses())
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
