package ca.bradj.horsehotel;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = HorseHotel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();


    public enum StorageType {
        GTA,
        BOTW;
    }

    private static final ForgeConfigSpec.EnumValue<StorageType> STORAGE_TYPE = BUILDER
            .comment("Storage type \"GTA\" means that horses will be de-registered when they are summoned from a stable.")
            .comment("Storage type \"BOTW\" means that horses will stay registered unless the player explicitly de-registers them.")
            .defineEnum("storageType", StorageType.BOTW);
    private static final ForgeConfigSpec.IntValue HORSE_REFRESH_INTERVAL = BUILDER
            .comment("The number of ticks that pass before a horse at a stable will update its visual characteristics.")
            .comment("Setting a higher value will reduce server demand, but may cause minor visual glitches.")
            .defineInRange("horseRefreshInterval", 50, 1, 24000);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static StorageType storageType;
    public static int horseRefreshInterval;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        storageType = STORAGE_TYPE.get();
        horseRefreshInterval = HORSE_REFRESH_INTERVAL.get();
    }
}
