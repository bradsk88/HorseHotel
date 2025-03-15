package ca.bradj.horsehotel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class StableBoundRandomStroll extends RandomStroll {
    private final float speedModifier;

    public StableBoundRandomStroll(float p_24546_) {
        super(p_24546_);
        this.speedModifier = p_24546_;
    }

    protected void start(
            ServerLevel p_24556_,
            PathfinderMob p_24557_,
            long p_24558_
    ) {
        if (!(p_24557_ instanceof StableAttendant sa)) {
            return;
        }
        HHNBT hpd = HHNBT.getPersistentData(sa);
        if (hpd.contains(HHNBT.Key.ANCHOR_POS)) {
            BlockPos blockPos = hpd.getBlockPos(HHNBT.Key.ANCHOR_POS);
            double v = blockPos.distSqr(p_24557_.blockPosition());
            HorseHotel.LOGGER.debug("SqDist2Anchor: {}", v);
            if (v > 80) {
                HorseHotel.LOGGER.debug("Returning to anchor: {}", blockPos);
                sa.getBrain().setMemory(
                        MemoryModuleType.WALK_TARGET,
                        new WalkTarget(blockPos, this.speedModifier, 0)
                );
                return;
            }
        }
        super.start(p_24556_, p_24557_, p_24558_);
    }
}