package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.network.UIHorse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Collection;
import java.util.function.Supplier;

public class ClientAccess {
    public static boolean openHorseSummonScreen(
            Collection<UIHorse> horses
    ) {
        return openScreen(() -> new HorseSummonScreen(horses));
    }

    public static boolean openScreen(Supplier<Screen> screen) {
        Minecraft.getInstance().setScreen(screen.get());
        return true;
    }

    public static void closeScreens() {
        Minecraft.getInstance().setScreen(null);
    }
}
