package ca.bradj.horsehotel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public class ModItemGroup {

    public static final CreativeModeTab HORSEHOTEL_GROUP = new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.horsehotel"))
            .icon(() -> ItemsInit.COWBOY_HAT.get().getDefaultInstance())
            .displayItems((params, output) -> output.accept(ItemsInit.COWBOY_HAT.get()))
            .build();
}
