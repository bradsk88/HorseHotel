package ca.bradj.horsehotel;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroup {

    public static final CreativeModeTab HORSEHOTEL_GROUP = new CreativeModeTab("horsehotel") {
        @Override
        public ItemStack makeIcon() {
            return ca.bradj.horsehotel.ItemsInit.COWBOY_HAT.get().getDefaultInstance();
        }
    };
}
