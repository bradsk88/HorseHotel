package ca.bradj.horsehotel.autotest;

import ca.bradj.horsehotel.Config;
import ca.bradj.horsehotel.EntitiesInit;
import ca.bradj.horsehotel.HHNBT;
import ca.bradj.horsehotel.HorseHotel;
import ca.bradj.horsehotel.NotHorseEntity;
import ca.bradj.horsehotel.StableAttendant;
import ca.bradj.horsehotel.Stock;
import ca.bradj.horsehotel.gui.HorseSummonScreen;
import ca.bradj.horsehotel.gui.PurchaseScreen;
import ca.bradj.horsehotel.gui.StockScreen;
import ca.bradj.horsehotel.network.UIHorse;
import ca.bradj.horsehotel.network.UIStockHorse;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A client-side screenshot harness — the visual counterpart to {@link AutoTestRunner}, which can
 * only assert server state. Gated on {@code -Dhorsehotel.uishot=true}; a normal {@code runClient}
 * is unaffected. It loads a world, builds a fresh-stable scene (an Attendant with real Stock plus
 * Stand-Ins in the reserved stalls) so the for-sale horses render exactly as current code produces
 * them, points the camera at them, screenshots, then opens each hotel screen and screenshots those
 * too. PNGs land in {@code run/screenshots/}.
 */
@Mod.EventBusSubscriber(modid = HorseHotel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class UIShotHarness {

    private static final String SYSPROP = "horsehotel.uishot";
    private static final String ENVVAR = "HORSEHOTEL_UISHOT";
    private static final String WORLD = "New World";

    /**
     * ForgeGradle's {@code runClient} does not forward {@code -D} system properties to the forked
     * client JVM (unlike {@code runServer}), so this is gated on an environment variable, which a
     * forked JVM does inherit. The system property is accepted too, for when it does come through.
     */
    private static boolean enabled() {
        return "true".equals(System.getenv(ENVVAR)) || "true".equals(System.getProperty(SYSPROP));
    }

    private enum Phase {
        BOOT, LOAD, WAIT_WORLD, PLACE, WAIT_ALIVE, FORCE_STOCK, SETTLE,
        SHOOT_A, SHOOT_B, SHOOT_C, SHOOT_OVERHEAD,
        SHOW_STOCK_UI, GRAB_STOCK_UI, SHOW_PURCHASE_UI, GRAB_PURCHASE_UI, DONE, IDLE
    }

    private static Phase phase = Phase.BOOT;
    private static int waited = 0;
    private static volatile boolean sceneBuilt = false;
    private static volatile boolean stockForced = false;
    private static boolean announced = false;
    private static BlockPos origin;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!announced) {
            announced = true;
            HorseHotel.LOGGER.info("[uishot] first client tick; enabled={} (env={}, prop={})",
                    enabled(), System.getenv(ENVVAR), System.getProperty(SYSPROP));
        }
        if (!enabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        switch (phase) {
            case BOOT -> {
                // Give the title screen a moment; fall through on a timer if it never settles.
                if ((mc.screen instanceof TitleScreen && mc.getOverlay() == null) || ++waited > 200) {
                    phase = Phase.LOAD;
                }
            }
            case LOAD -> {
                if (mc.getSingleplayerServer() == null && mc.level == null) {
                    HorseHotel.LOGGER.info("[uishot] loading world '{}'", WORLD);
                    mc.createWorldOpenFlows().loadLevel(mc.screen == null ? new TitleScreen() : mc.screen, WORLD);
                }
                waited = 0;
                phase = Phase.WAIT_WORLD;
            }
            case WAIT_WORLD -> {
                if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
                    waited = 0;
                    phase = Phase.PLACE;
                }
            }
            case PLACE -> {
                mc.getSingleplayerServer().execute(UIShotHarness::placeStable);
                waited = 0;
                phase = Phase.WAIT_ALIVE;
            }
            case WAIT_ALIVE -> {
                // Let the spawn blocks convert to stand-ins and spawn the attendant, and let the
                // attendant tick (its gate is 100 ticks).
                if (sceneBuilt && ++waited > 240) {
                    waited = 0;
                    phase = Phase.FORCE_STOCK;
                }
            }
            case FORCE_STOCK -> {
                // Guarantee a visible barn by forcing the (now-spawned) attendant's stock to 3,
                // rather than relying on its random roll being non-zero. Retry until it lands.
                mc.getSingleplayerServer().execute(UIShotHarness::forceStock);
                if (stockForced || ++waited > 100) {
                    waited = 0;
                    phase = Phase.SETTLE;
                }
            }
            case SETTLE -> {
                // Wait a couple of stand-in refresh cycles so the barn stalls take on the stock.
                if (++waited > Math.max(1, Config.horseRefreshInterval) * 2 + 20) {
                    waited = 0;
                    phase = Phase.SHOOT_A;
                }
            }
            case SHOOT_A -> waited = shoot(mc, waited, 8, 1.2, 12, 180f, 3f, "stable_barn_north", Phase.SHOOT_B);
            case SHOOT_B -> waited = shoot(mc, waited, 12, 2.2, 9, 270f, 5f, "stable_barn_west", Phase.SHOOT_C);
            case SHOOT_C -> waited = shoot(mc, waited, 7, 2.2, 1, 0f, 8f, "stable_south", Phase.SHOOT_OVERHEAD);
            case SHOOT_OVERHEAD -> waited = shoot(mc, waited, 7, 26, 8, 0f, 88f, "stable_overhead", Phase.SHOW_STOCK_UI);
            case SHOW_STOCK_UI -> {
                mc.setScreen(new StockScreen(fakeStock(mc), true, BlockPos.ZERO));
                waited = 0;
                phase = Phase.GRAB_STOCK_UI;
            }
            case GRAB_STOCK_UI -> {
                if (++waited > 10) {
                    grab(mc, "ui_for_sale_v2");
                    phase = Phase.SHOW_PURCHASE_UI;
                }
            }
            case SHOW_PURCHASE_UI -> {
                mc.setScreen(new PurchaseScreen(UUID.randomUUID(), fakeHorse(mc), BlockPos.ZERO));
                waited = 0;
                phase = Phase.GRAB_PURCHASE_UI;
            }
            case GRAB_PURCHASE_UI -> {
                if (++waited > 10) {
                    grab(mc, "ui_purchase_v2");
                    mc.setScreen(null);
                    phase = Phase.DONE;
                }
            }
            case DONE -> {
                HorseHotel.LOGGER.info("[uishot] done");
                phase = Phase.IDLE;
                waited = 0;
            }
            case IDLE -> {
                // Leave the scene up briefly for a human watching, then stop.
                if (++waited > 120) {
                    mc.stop();
                }
            }
        }
    }

    /**
     * Teleports the camera to a vantage (given in structure-local coordinates), waits a few ticks
     * for the client to sync, grabs a screenshot, and advances. Returns the new wait counter.
     */
    private static int shoot(
            Minecraft mc, int wait, double lx, double ly, double lz, float yaw, float xRot, String name, Phase next
    ) {
        if (origin == null) {
            phase = next;
            return 0;
        }
        if (wait == 0) {
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer sp = server(mc);
                if (sp != null) {
                    sp.teleportTo(mc.getSingleplayerServer().overworld(),
                            origin.getX() + lx, origin.getY() + ly, origin.getZ() + lz, yaw, xRot);
                }
            });
        }
        if (mc.player != null) {
            mc.player.setYRot(yaw);
            mc.player.setXRot(xRot);
            mc.player.setYHeadRot(yaw);
        }
        if (wait > 15) {
            grab(mc, name);
            phase = next;
            return 0;
        }
        return wait + 1;
    }

    /** Places the real stable structure in the air above the player, in daylight. */
    private static void placeStable() {
        Minecraft mc = Minecraft.getInstance();
        ServerLevel level = mc.getSingleplayerServer().overworld();
        ServerPlayer sp = server(mc);
        if (sp == null) {
            return;
        }
        // Structure min corner, centred over the player's column, up in the open sky.
        origin = new BlockPos(sp.getBlockX() - 7, 100, sp.getBlockZ() - 8);

        level.setDayTime(1000);
        level.setWeatherParameters(1_000_000, 0, false, false);

        StructureTemplateManager stm = level.getStructureManager();
        java.util.Optional<StructureTemplate> opt = stm.get(ResourceLocation.tryBuild(HorseHotel.MODID, "stable"));
        if (opt.isEmpty()) {
            HorseHotel.LOGGER.error("[uishot] stable structure template not found");
            sceneBuilt = true;
            return;
        }
        opt.get().placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        HorseHotel.LOGGER.info("[uishot] placed stable at {}", origin);
        sceneBuilt = true;
    }

    /** Forces the spawned attendant's stock to a full three, so the barn is never empty for the shot. */
    private static void forceStock() {
        Minecraft mc = Minecraft.getInstance();
        ServerLevel level = mc.getSingleplayerServer().overworld();
        if (origin == null) {
            return;
        }
        BlockPos center = origin.offset(7, 2, 8);
        List<StableAttendant> atts = level.getEntitiesOfClass(StableAttendant.class, new AABB(center).inflate(30));
        if (atts.isEmpty()) {
            return;
        }
        HHNBT ad = HHNBT.getPersistentData(atts.get(0));
        Stock.setStock(level, ad, center, 3);
        ad.put(HHNBT.Key.RESTOCK_DEADLINE, level.getGameTime() + 1_000_000L);
        HorseHotel.LOGGER.info("[uishot] forced stock=3 on attendant at {}", atts.get(0).blockPosition());
        stockForced = true;
    }

    private static ServerPlayer server(Minecraft mc) {
        List<ServerPlayer> players = mc.getSingleplayerServer().getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.get(0);
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                c -> HorseHotel.LOGGER.info("[uishot] {}", c.getString()));
        HorseHotel.LOGGER.info("[uishot] grabbed {}", name);
    }

    private static CompoundTag fakeHorse(Minecraft mc) {
        Horse horse = EntityType.HORSE.create(mc.level);
        return horse == null ? new CompoundTag() : horse.serializeNBT();
    }

    private static ImmutableList<UIHorse> fakeRegistry(Minecraft mc) {
        List<UIHorse> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            list.add(new UIHorse(i, fakeHorse(mc)));
        }
        return ImmutableList.copyOf(list);
    }

    private static ImmutableList<UIStockHorse> fakeStock(Minecraft mc) {
        List<UIStockHorse> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            list.add(new UIStockHorse(UUID.randomUUID(), fakeHorse(mc)));
        }
        return ImmutableList.copyOf(list);
    }
}
