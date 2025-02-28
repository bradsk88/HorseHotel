package ca.bradj.horsehotel;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemsInit {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            HorseHotel.MODID
    );

    public static final RegistryObject<Item> COWBOY_HAT = ITEMS.register(
            CowboyHat.ITEM_ID, CowboyHat::new
    );
}