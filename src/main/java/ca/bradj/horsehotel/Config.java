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
    private static final ForgeConfigSpec.IntValue HORSE_EVAL_RADIUS = BUILDER
            .comment("How far (in blocks) a player can be from a stable and still have its horses update.")
            .comment("This must be at least as large as the distance at which clients are sent the horses,")
            .comment("which is 80 blocks by default. Setting it lower means a client can be shown a horse")
            .comment("before the server has decided whether that horse should be visible at all.")
            .defineInRange("horseEvalRadius", 80, 16, 512);
    private static final ForgeConfigSpec.IntValue MAX_HORSES_FOR_SALE = BUILDER
            .comment("How many of a stable's stalls are reserved for horses the stable offers for sale.")
            .comment("The reserved stalls are the roofed barn stalls (indices 0..N-1); the player's own")
            .comment("registered horses graze in the remaining stalls. Each restock rolls a fresh count")
            .comment("between 0 and this value. Set to 0 to disable the shop entirely. Note this cannot")
            .comment("validate against a stable's real stall count, so a value above it hides all of a")
            .comment("player's own horses.")
            .defineInRange("maxHorsesForSale", 3, 0, 8);
    private static final ForgeConfigSpec.IntValue RESTOCK_INTERVAL = BUILDER
            .comment("The number of ticks between restocks — a stable discarding its for-sale horses and")
            .comment("rolling a fresh assortment. 24000 ticks is one Minecraft day.")
            .defineInRange("restockIntervalTicks", 24000, 1, 1728000);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static StorageType storageType;
    public static int horseRefreshInterval;
    public static int horseEvalRadius;
    public static int maxHorsesForSale;
    public static int restockIntervalTicks;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        storageType = STORAGE_TYPE.get();
        horseRefreshInterval = HORSE_REFRESH_INTERVAL.get();
        horseEvalRadius = HORSE_EVAL_RADIUS.get();
        maxHorsesForSale = MAX_HORSES_FOR_SALE.get();
        restockIntervalTicks = RESTOCK_INTERVAL.get();
    }
}
