package ca.bradj.horsehotel;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JobSiteFromHorseSensor extends Sensor<LivingEntity> {
    public static final String ID = "jobsite_from_nothorse";

    public JobSiteFromHorseSensor() {
    }

    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }

    protected void doTick(
            ServerLevel p_26740_,
            LivingEntity p_26741_
    ) {
        Brain<?> brain = p_26741_.getBrain();
        Optional<NearestVisibleLivingEntities> les = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (les.isEmpty()) {
            return;
        }

        NearestVisibleLivingEntities ents = les.get();
        List<LivingEntity> nhe = ents.find(v -> v instanceof NotHorseEntity).toList();
        List<LivingEntity> list1 = nhe.stream().filter((p_26747_) -> {
            return isEntityTargetable(p_26741_, p_26747_);
        }).collect(Collectors.toList());
        brain.setMemory(
                MemoryModuleType.JOB_SITE,
                list1.isEmpty() ? null : GlobalPos.of(p_26740_.dimension(), list1.get(0).getOnPos().above())
        );
    }
}