package ca.bradj.horsehotel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import java.util.Optional;

public class SAGoalPackages {
    private static final float STROLL_SPEED_MODIFIER = 0.4F;

    // Behaviors without a declarative create(...) factory in 1.20.1: wrap them so they carry the
    // BehaviorControl type the brain's addActivity() wants (Pair is invariant in its second slot).
    private static BehaviorControl<? super StableAttendant> swim() {
        return new Swim(0.8F);
    }

    private static BehaviorControl<? super StableAttendant> lookAtTargetSink() {
        return new LookAtTargetSink(45, 90);
    }

    private static BehaviorControl<? super StableAttendant> moveToTargetSink() {
        return new MoveToTargetSink();
    }

    private static BehaviorControl<? super StableAttendant> sleepInBed() {
        return new SleepInBed();
    }

    private static BehaviorControl<? super StableAttendant> doNothing(int minTicks, int maxTicks) {
        return new DoNothing(minTicks, maxTicks);
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super StableAttendant>>> getCorePackage(float p_24587_) {
        return ImmutableList.of(
                Pair.of(0, swim()),
                Pair.of(0, InteractWithDoor.create()),
                Pair.of(0, lookAtTargetSink()),
                Pair.of(0, WakeUp.create()),
                Pair.of(1, moveToTargetSink()),
                Pair.of(
                        10, AcquirePoi.create(
                                (p_217499_) -> {
                                    return p_217499_.is(PoiTypes.HOME);
                                }, MemoryModuleType.HOME, false, Optional.of((byte) 14)
                        )
                )
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super StableAttendant>>> getWorkPackage(float p_24591_) {
        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super StableAttendant>>> b = ImmutableList.builder();
        b.add(getMinimalLookBehavior());
        BehaviorControl<StableAttendant> strollAroundOrTo = new RunOne<>(ImmutableList.of(
                Pair.of(StrollAroundPoi.create(MemoryModuleType.JOB_SITE, 0.4F, 4), 2),
                Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE, 0.4F, 1, 10), 5)
        ));
        b.add(Pair.of(5, strollAroundOrTo));
        b.add(Pair.of(10, SetLookAndInteract.create(EntityType.PLAYER, 4)));
        b.add(Pair.of(2, (BehaviorControl<? super StableAttendant>) new SetWalkTargetFromBlockMemoro(MemoryModuleType.JOB_SITE, p_24591_, 9, 100, 1200)));
        b.add(Pair.of(99, UpdateActivityFromSchedule.create()));

        return b.build();
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super StableAttendant>>> getRestPackage(float p_24594_) {
        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super StableAttendant>>> b = ImmutableList.builder();
        b.add(Pair.of(
                3, ValidateNearbyPoi.create(
                        (p_217495_) -> {
                            return p_217495_.is(PoiTypes.HOME);
                        }, MemoryModuleType.HOME
                )
        ));
        b.add(Pair.of(3, sleepInBed()));
        b.add(Pair.of(
                5, (BehaviorControl<? super StableAttendant>) new RunOne<>(
                        ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT), ImmutableList.of(
                        Pair.of(SetClosestHomeAsWalkTarget.create(p_24594_), 1),
                        Pair.of(InsideBrownianWalk.create(p_24594_), 4),
                        Pair.of(doNothing(20, 40), 2)
                )
                )
        ));
        b.add(getMinimalLookBehavior());
        b.add(Pair.of(99, UpdateActivityFromSchedule.create()));
        return b.build();
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super StableAttendant>>> getIdlePackage(float normalWalkMod) {
        ImmutableList.Builder<Pair<? extends BehaviorControl<? super StableAttendant>, Integer>> b = ImmutableList.builder();
        b.add(Pair.of(SetWalkTargetFromLookTarget.create(normalWalkMod, 2), 1));
        b.add(Pair.of(doNothing(30, 60), 1));
        b.add(Pair.of(RandomStroll.stroll(normalWalkMod * 0.5f), 1));

        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super StableAttendant>>> b2 = ImmutableList.builder();
        b2.add(Pair.of(2, (BehaviorControl<? super StableAttendant>) new RunOne<>(b.build())));
        b2.add(Pair.of(3, SetLookAndInteract.create(EntityType.PLAYER, 4)));
        b2.add(getFullLookBehavior());
        b2.add(Pair.of(99, UpdateActivityFromSchedule.create()));

        return b2.build();
    }

    private static Pair<Integer, ? extends BehaviorControl<? super StableAttendant>> getFullLookBehavior() {
        return Pair.of(
                5, (BehaviorControl<? super StableAttendant>) new RunOne<>(ImmutableList.of(
                        Pair.of(SetEntityLookTarget.create(EntityType.CAT, 8.0F), 8),
                        Pair.of(SetEntityLookTarget.create(EntityType.VILLAGER, 8.0F), 2),
                        Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                        Pair.of(SetEntityLookTarget.create(MobCategory.CREATURE, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.WATER_CREATURE, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.AXOLOTLS, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.UNDERGROUND_WATER_CREATURE, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.WATER_AMBIENT, 8.0F), 1),
                        Pair.of(SetEntityLookTarget.create(MobCategory.MONSTER, 8.0F), 1),
                        Pair.of(doNothing(30, 60), 2)
                ))
        );
    }

    private static Pair<Integer, ? extends BehaviorControl<? super StableAttendant>> getMinimalLookBehavior() {
        return Pair.of(
                5, (BehaviorControl<? super StableAttendant>) new RunOne<>(ImmutableList.of(
                        Pair.of(SetEntityLookTarget.create(EntityType.VILLAGER, 8.0F), 2),
                        Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                        Pair.of(doNothing(30, 60), 8)
                ))
        );
    }
}
