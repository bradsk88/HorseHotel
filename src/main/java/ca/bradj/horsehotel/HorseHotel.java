package ca.bradj.horsehotel;

import ca.bradj.horsehotel.network.HHNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import static ca.bradj.horsehotel.BlocksInit.BLOCKS;
import static ca.bradj.horsehotel.ItemsInit.ITEMS;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(HorseHotel.MODID)
public class HorseHotel {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "horsehotel";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final Item.Properties DEFAULT_ITEM_PROPS = new Item.Properties().
            tab(ModItemGroup.HORSEHOTEL_GROUP);

    public HorseHotel(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);

        TilesInit.TILES.register(modEventBus);

        EntitiesInit.TILES.register(modEventBus);

        SensorsInit.SENSORS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static void logBug(
            String s,
            Object... args
    ) {
        LOGGER.error(s, args);
        LOGGER.error("This is a bug. Please report it to https://github.com/bradsk88/horsehotel/issues");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));

        event.enqueueWork(HHNetwork::init);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> EntityRenderers.register(
                    EntitiesInit.FAKE_HORSE.get(),
                    NotHorseEntityRenderer::new
            ));
            event.enqueueWork(() -> EntityRenderers.register(
                    EntitiesInit.STABLE_ATTENDANT.get(),
                    StableAttendantRenderer::new
            ));
        }
    }
}
