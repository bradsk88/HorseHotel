package ca.bradj.horsehotel;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JobSiteFromHorseSensor extends Sensor<LivingEntity> {
    public static final String ID = "jobsite_from_nothorse";

    public JobSiteFromHorseSensor() {
    }

    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES);
    }

    protected void doTick(
            ServerLevel p_26740_,
            LivingEntity p_26741_
    ) {
        Brain<?> brain = p_26741_.getBrain();
        Optional<List<LivingEntity>> les = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
        if (les.isEmpty()) {
            return;
        }

        List<LivingEntity> ents = les.get();
        List<LivingEntity> nhe = ents.stream().filter(v -> v instanceof NotHorseEntity).toList();
        brain.setMemory(MemoryModuleType.JOB_SITE, getAttendPosition(p_26740_, nhe));
    }

    private static @Nullable GlobalPos getAttendPosition(
            ServerLevel p_26740_,
            List<LivingEntity> list1
    ) {
        if (list1.isEmpty()) {
            return null;
        }
        Direction ranDir = Direction.Plane.HORIZONTAL.getRandomDirection(p_26740_.random);
        int index = p_26740_.random.nextInt(Math.min(list1.size(), 6));
        BlockPos p = list1.get(index).getOnPos().above().relative(ranDir, 4);
        return GlobalPos.of(p_26740_.dimension(), p);
    }
}