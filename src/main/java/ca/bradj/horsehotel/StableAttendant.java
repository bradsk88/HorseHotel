package ca.bradj.horsehotel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

public class StableAttendant extends PathfinderMob {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String ID = "stable_attendant";
    private int tick = 0;
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.HOME,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.PATH,
            MemoryModuleType.DOORS_TO_CLOSE,
            MemoryModuleType.NEAREST_BED,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.LAST_SLEPT,
            MemoryModuleType.LAST_WOKEN,
            MemoryModuleType.NEAREST_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_PLAYERS,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryModuleType.JOB_SITE
    );
    private static final ImmutableList<SensorType<? extends Sensor<? super StableAttendant>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_BED,
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.NEAREST_PLAYERS,
            SensorsInit.JOBSITE_FROM_HORSES.get()
    );
    public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<StableAttendant, Holder<PoiType>>> POI_MEMORIES = ImmutableMap.of(
            MemoryModuleType.HOME,
            (p_219625_, p_219626_) -> p_219626_.is(PoiTypes.HOME)
    );

    protected StableAttendant(
            Level p_20967_
    ) {
        this(p_20967_, -1);
    }

    public StableAttendant(
            Level level,
            int index
    ) {
        super(EntitiesInit.STABLE_ATTENDANT.get(), level);
        setItemSlot(EquipmentSlot.HEAD, ItemsInit.COWBOY_HAT.get().getDefaultInstance());
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.setCanPickUpLoot(false);
    }

    public void die(DamageSource p_35419_) {
        LOGGER.info("Villager {} died, message: '{}'", this, p_35419_.getLocalizedDeathMessage(this).getString());
        this.releaseAllPois();
        super.die(p_35419_);
    }

    private void releaseAllPois() {
        this.releasePoi(MemoryModuleType.HOME);
    }

    public void releasePoi(MemoryModuleType<GlobalPos> p_35429_) {
        if (this.level instanceof ServerLevel) {
            MinecraftServer minecraftserver = ((ServerLevel) this.level).getServer();
            this.brain.getMemory(p_35429_).ifPresent((p_186306_) -> {
                ServerLevel serverlevel = minecraftserver.getLevel(p_186306_.dimension());
                if (serverlevel != null) {
                    PoiManager poimanager = serverlevel.getPoiManager();
                    Optional<Holder<PoiType>> optional = poimanager.getType(p_186306_.pos());
                    BiPredicate<StableAttendant, Holder<PoiType>> bipredicate = POI_MEMORIES.get(p_35429_);
                    if (optional.isPresent() && bipredicate.test(this, optional.get())) {
                        poimanager.release(p_186306_.pos());
                        DebugPackets.sendPoiTicketCountPacket(serverlevel, p_186306_.pos());
                    }

                }
            });
        }
    }

    public static AttributeSupplier setAttributes() {
        return PathfinderMob.createMobAttributes().build();
    }

    @Override
    public InteractionResult interactAt(
            Player player,
            Vec3 p_19981_,
            InteractionHand p_19982_
    ) {
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.CONSUME;
        }
        if (!InteractionHand.MAIN_HAND.equals(p_19982_)) {
            return InteractionResult.CONSUME;
        }
        Registration.interactWithRegistry(sp);
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel sl)) {
            return;
        }

        if (tick++ < 100) {
            return;
        }

        tick = 0;
    }

    @Override
    public void startSleeping(BlockPos p_21141_) {
        super.startSleeping(p_21141_);
        this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.level.getGameTime());
        this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    public void stopSleeping() {
        super.stopSleeping();
        this.brain.setMemory(MemoryModuleType.LAST_WOKEN, this.level.getGameTime());
    }

    public Brain<StableAttendant> getBrain() {
        return (Brain<StableAttendant>) super.getBrain();
    }

    protected Brain.Provider<StableAttendant> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    protected Brain<?> makeBrain(Dynamic<?> p_35445_) {
        Brain<StableAttendant> brain = this.brainProvider().makeBrain(p_35445_);
        this.registerBrainGoals(brain);
        return brain;
    }

    private void registerBrainGoals(Brain<StableAttendant> p_35425_) {
        p_35425_.setSchedule(Schedule.VILLAGER_DEFAULT);
        p_35425_.addActivity(Activity.CORE, SAGoalPackages.getCorePackage(0.5F));
        p_35425_.addActivity(Activity.MEET, SAGoalPackages.getIdlePackage(0.5F));
        p_35425_.addActivity(Activity.REST, SAGoalPackages.getRestPackage(0.5F));
        p_35425_.addActivity(Activity.IDLE, SAGoalPackages.getIdlePackage(0.5F));
        p_35425_.addActivity(Activity.WORK, SAGoalPackages.getWorkPackage(0.5F));
        p_35425_.setCoreActivities(ImmutableSet.of(Activity.CORE));
        p_35425_.setDefaultActivity(Activity.IDLE);
        p_35425_.setActiveActivityIfPossible(Activity.IDLE);
        p_35425_.updateActivityFromSchedule(this.level.getDayTime(), this.level.getGameTime());
    }


    @Override
    protected void customServerAiStep() {
        this.level.getProfiler().push("villagerBrain");
        this.getBrain().tick((ServerLevel) this.level, this);
        this.level.getProfiler().pop();
        super.customServerAiStep();
    }
}
