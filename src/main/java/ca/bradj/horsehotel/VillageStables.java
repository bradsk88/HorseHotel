package ca.bradj.horsehotel;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes the stable generate as part of most villages by injecting its building piece into the
 * vanilla village house pools when the server starts (after datapacks have loaded).
 * <p>
 * This is done in code rather than by overriding the vanilla pool JSON so we don't clobber other
 * mods or datapacks that touch the same pools. The standalone {@code horsehotel:stable} structure
 * that scatters across the overworld is left untouched; villages use compact, biome-matched variants
 * ({@code horsehotel:stable_village_<type>}) that each carry a {@code building_entrance} jigsaw so
 * they attach to village streets. The compact footprint (7x12 vs the standalone 14x17) lets the
 * stable win jigsaw slots often enough to land in most villages.
 */
@Mod.EventBusSubscriber(modid = HorseHotel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillageStables {

    // Weight of the stable within each village's house pool (pool totals sit around 80-100). Even a
    // compact building loses some jigsaw slots to collision, so it needs a healthy weight to land in
    // most villages. Raise to make stables more common, lower to make them rarer.
    private static final int WEIGHT = 22;

    // Each village type gets a stable reskinned to its own materials (plains oak, savanna acacia,
    // taiga/snowy spruce, desert sandstone), matching the vanilla buildings around it.
    private static final String[] VILLAGE_TYPES = {"plains", "desert", "savanna", "snowy", "taiga"};

    @SubscribeEvent
    public static void addStableToVillages(ServerAboutToStartEvent event) {
        RegistryAccess registries = event.getServer().registryAccess();
        Registry<StructureTemplatePool> pools =
                registries.registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processors =
                registries.registryOrThrow(Registries.PROCESSOR_LIST);

        Holder<StructureProcessorList> emptyProcessors = processors.getHolderOrThrow(
                ResourceKey.create(Registries.PROCESSOR_LIST,
                        new ResourceLocation("minecraft", "empty")));

        for (String type : VILLAGE_TYPES) {
            ResourceLocation poolId =
                    new ResourceLocation("minecraft", "village/" + type + "/houses");
            StructureTemplatePool pool = pools.get(poolId);
            if (pool == null) {
                HorseHotel.LOGGER.warn("Village house pool {} not found; stables will not spawn in {} villages", poolId, type);
                continue;
            }

            // Non-legacy single element: the stable template carves its interior with explicit air
            // blocks, which a legacy element would treat as "keep existing terrain" and fill in.
            ResourceLocation piece = new ResourceLocation(HorseHotel.MODID, "stable_village_" + type);
            StructurePoolElement stable = StructurePoolElement
                    .single(piece.toString(), emptyProcessors)
                    .apply(StructureTemplatePool.Projection.RIGID);

            // templates is the expanded list generation samples from: add one copy per weight point.
            for (int i = 0; i < WEIGHT; i++) {
                pool.templates.add(stable);
            }

            // rawTemplates is immutable, so replace it with a copy that includes the weighted entry.
            // Keeping it in sync means getMaxSize() accounts for the stable's footprint.
            List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList<>(pool.rawTemplates);
            rawTemplates.add(Pair.of(stable, WEIGHT));
            pool.rawTemplates = rawTemplates;
        }
    }
}
