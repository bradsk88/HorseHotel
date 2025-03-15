package ca.bradj.horsehotel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import java.util.Optional;

public class SAGoalPackages {
    private static final float STROLL_SPEED_MODIFIER = 0.4F;

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super StableAttendant>>> getCorePackage(float p_24587_) {
        return ImmutableList.of(
                Pair.of(0, new Swim(0.8F)),
                Pair.of(0, new InteractWithDoor()),
                Pair.of(0, new LookAtTargetSink(45, 90)),
                Pair.of(0, new WakeUp()),
                Pair.of(1, new MoveToTargetSink()),
                Pair.of(
                        10, new AcquirePoi(
                                (p_217499_) -> {
                                    return p_217499_.is(PoiTypes.HOME);
                                }, MemoryModuleType.HOME, false, Optional.of((byte) 14)
                        )
                )
        );
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super StableAttendant>>> getWorkPackage(float p_24591_) {
        ImmutableList.Builder<Pair<Integer, ? extends Behavior<? super StableAttendant>>> b = ImmutableList.builder();
        b.add(getMinimalLookBehavior());
        RunOne<StableAttendant> strollAroundOrTo = new RunOne<>(ImmutableList.of(
                Pair.of(new StrollAroundPoi(MemoryModuleType.JOB_SITE, 0.4F, 4), 2),
                Pair.of(new StrollToPoi(MemoryModuleType.JOB_SITE, 0.4F, 1, 10), 5)
        ));
        b.add(Pair.of(5, strollAroundOrTo));
        b.add(Pair.of(10, new SetLookAndInteract(EntityType.PLAYER, 4)));
        b.add(Pair.of(2, new SetWalkTargetFromBlockMemoro(MemoryModuleType.JOB_SITE, p_24591_, 9, 100, 1200)));
        b.add(Pair.of(99, new UpdateActivityFromSchedule()));

        return b.build();
    }

    public static ImmutableList<Pair<Integer, ? extends Behavior<? super StableAttendant>>> getRestPackage(float p_24594_) {
        ImmutableList.Builder<Pair<Integer, ? extends Behavior<? super StableAttendant>>> b = ImmutableList.builder();
        b.add(Pair.of(
                3, new ValidateNearbyPoi(
                        (p_217495_) -> {
                            return p_217495_.is(PoiTypes.HOME);
                        }, MemoryModuleType.HOME
                )
        ));
        b.add(Pair.of(3, new SleepInBed()));
        b.add(Pair.of(
                5, new RunOne<>(
                        ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT), ImmutableList.of(
                        Pair.of(new SetClosestHomeAsWalkTarget(p_24594_), 1),
                        Pair.of(new InsideBrownianWalk(p_24594_), 4),
                        Pair.of(new DoNothing(20, 40), 2)
                )
                )
        ));
        b.add(getMinimalLookBehavior());
        b.add(Pair.of(99, new UpdateActivityFromSchedule()));
        return b.build();
    }

    public static ImmutableList<Pair<Integer, Behavior<? super PathfinderMob>>> getIdlePackage(float normalWalkMod) {
        Behavior<LivingEntity> swtflt = new SetWalkTargetFromLookTarget(normalWalkMod, 2);
        Pair<Behavior<? super PathfinderMob>, Integer> walk = Pair.of(swtflt, 1);
        Pair<Behavior<? super PathfinderMob>, Integer> doNothing = Pair.of(new DoNothing(30, 60), 1);
        ImmutableList.Builder<Pair<Behavior<? super PathfinderMob>, Integer>> b = ImmutableList.builder();
        b.add(walk);
        b.add(doNothing);
        b.add(Pair.of(new StableBoundRandomStroll(normalWalkMod * 0.5f), 1));
        ImmutableList<Pair<Behavior<? super PathfinderMob>, Integer>> pairs = b.build();

        ImmutableList.Builder<Pair<Integer, Behavior<? super PathfinderMob>>> b2 = ImmutableList.builder();
        b2.add(Pair.of(2, new RunOne<>(pairs)));
        b2.add(Pair.of(3, new SetLookAndInteract(EntityType.PLAYER, 4)));
        b2.add(getFullLookBehavior());
        b2.add(Pair.of(99, new UpdateActivityFromSchedule()));

        return b2.build();
    }

    private static Pair<Integer, Behavior<? super PathfinderMob>> getFullLookBehavior() {
        return Pair.of(
                5, new RunOne<>(ImmutableList.of(
                        Pair.of(new SetEntityLookTarget(EntityType.CAT, 8.0F), 8),
                        Pair.of(new SetEntityLookTarget(EntityType.VILLAGER, 8.0F), 2),
                        Pair.of(new SetEntityLookTarget(EntityType.PLAYER, 8.0F), 2),
                        Pair.of(new SetEntityLookTarget(MobCategory.CREATURE, 8.0F), 1),
                        Pair.of(new SetEntityLookTarget(MobCategory.WATER_CREATURE, 8.0F), 1),
                        Pair.of(new SetEntityLookTarget(MobCategory.AXOLOTLS, 8.0F), 1),
                        Pair.of(new SetEntityLookTarget(MobCategory.UNDERGROUND_WATER_CREATURE, 8.0F), 1),
                        Pair.of(new SetEntityLookTarget(MobCategory.WATER_AMBIENT, 8.0F), 1),
                        Pair.of(new SetEntityLookTarget(MobCategory.MONSTER, 8.0F), 1),
                        Pair.of(new DoNothing(30, 60), 2)
                ))
        );
    }

    private static Pair<Integer, Behavior<LivingEntity>> getMinimalLookBehavior() {
        return Pair.of(
                5, new RunOne<>(ImmutableList.of(
                        Pair.of(new SetEntityLookTarget(EntityType.VILLAGER, 8.0F), 2),
                        Pair.of(new SetEntityLookTarget(EntityType.PLAYER, 8.0F), 2),
                        Pair.of(new DoNothing(30, 60), 8)
                ))
        );
    }
}