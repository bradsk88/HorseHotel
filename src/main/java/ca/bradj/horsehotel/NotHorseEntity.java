package ca.bradj.horsehotel;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NotHorseEntity extends LivingEntity {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String ID = "not_horse";
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(
            NotHorseEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<ItemStack> ARMOR_ITEM = SynchedEntityData.defineId(
            NotHorseEntity.class,
            EntityDataSerializers.ITEM_STACK
    );
    private int tick = 0;
    private int index;
    private boolean warned;
    private boolean everTicked;

    protected NotHorseEntity(
            Level p_20967_
    ) {
        this(p_20967_, -1);
    }

    public NotHorseEntity(
            Level level,
            int index
    ) {
        super(EntitiesInit.FAKE_HORSE.get(), level);
        this.index = index;
    }

    public Markings getMarkings() {
        return Markings.byId((this.getTypeVariant() & '\uff00') >> 8);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ID_TYPE_VARIANT, 0);
        this.entityData.define(ARMOR_ITEM, ItemStack.EMPTY);
    }

    public static AttributeSupplier setAttributes() {
        return PathfinderMob.createMobAttributes().build();
    }

    public Object getVariant() {
        return Variant.byId(this.getTypeVariant() & 255);
    }

    private int getTypeVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_21096_) {
        super.readAdditionalSaveData(p_21096_);
        this.setTypeVariant(p_21096_.getInt("Variant"));
    }

    private void setTypeVariant(int p_30737_) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, p_30737_);
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return ImmutableList.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot equipmentSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(
            EquipmentSlot equipmentSlot,
            ItemStack itemStack
    ) {

    }

    @Override
    public HumanoidArm getMainArm() {
        return null;
    }

    @Override
    public InteractionResult interact(
            Player player,
            InteractionHand p_19979_
    ) {
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        if (Items.STRUCTURE_BLOCK.getDefaultInstance().sameItem(player.getItemInHand(p_19979_))) {
            HHNBT hbt = HHNBT.getPersistentData(this);
            LOGGER.debug("Data is: {}", hbt);
            BlockState p46599 = BlocksInit.FAKE_HORSE_SPAWN_BLOCK.get().defaultBlockState();
            sp.getLevel().setBlockAndUpdate(getOnPos(), p46599);
            BlockEntity ent = sp.getLevel().getBlockEntity(getOnPos());
            if (ent instanceof FakeHorseSpawnBlock.Entity e) {
                e.settingUp = true;
                HHNBT ebt = HHNBT.getPersistentData(e);
                ebt.put(HHNBT.Key.REGISTERED_HORSE_INDEX, hbt.getInt(HHNBT.Key.REGISTERED_HORSE_INDEX));
            }
            remove(RemovalReason.KILLED);
            return InteractionResult.CONSUME;
        }

        trySpawnRealHorse(player, p_19979_);
        return InteractionResult.CONSUME;
    }

    private void trySpawnRealHorse(
            Player player,
            InteractionHand p19979
    ) {

        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (p19979 != InteractionHand.MAIN_HAND) {
            return;
        }

        HHNBT pd = HHNBT.getPersistentData(this);
        if (!pd.contains(HHNBT.Key.REGISTERED_HORSE_INDEX)) {
            LOGGER.error("Fake horse has no real-horse index in data {}", pd);
            return;
        }

        int uuid = pd.getInt(HHNBT.Key.REGISTERED_HORSE_INDEX);

        LOGGER.debug("Horse index: {}", uuid);

        @Nullable Horse spawned = respawnRealHorse(uuid, getOnPos(), sp, sp.getLevel());

        if (spawned != null) {
            deRegisterHorse(sp, spawned.getUUID());
            sp.getLevel().addFreshEntity(spawned);
            sp.startRiding(spawned);
        }
    }

    private static void deRegisterHorse(
            ServerPlayer sp,
            UUID spawnedHorse
    ) {
        HHNBT pd = HHNBT.getPersistentData(sp);
        CompoundTag list = pd.getCompound(HHNBT.Key.REGISTERED_HORSES);
        list.remove(spawnedHorse.toString());
        pd.put(HHNBT.Key.REGISTERED_HORSES, list);
    }


    public static @Nullable Horse respawnRealHorse(
            int horseIndex,
            BlockPos spawnPos,
            Player player,
            ServerLevel sl
    ) {
        HHNBT pd = HHNBT.getPersistentData(player);
        Horse newone;
        if (pd.contains(HHNBT.Key.REGISTERED_HORSES)) {
            CompoundTag l = pd.getCompound(HHNBT.Key.REGISTERED_HORSES);
            if (l.size() < horseIndex + 1) {
                LOGGER.debug("Index {} is greater than registry size {}", horseIndex, l.size());
                return null;
            }
            newone = buildNewHorse(sl);
            ImmutableList<String> ids = ImmutableList.copyOf(l.getAllKeys());
            String uuid = ids.get(horseIndex);
            CompoundTag nbt = l.getCompound(uuid);
            newone.deserializeNBT(Registration.updateDataToFullHealth(sl, nbt));
            newone.setPos(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ());
            if (Config.storageType != Config.StorageType.BOTW) {
                l.remove(uuid);
            }
            Entity existing = sl.getEntity(newone.getUUID());
            if (existing instanceof Horse hExisting) {
                existing.setPos(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ());
                return hExisting;
            }
            sl.addFreshEntity(newone);
            return newone;
        } else {
            LOGGER.debug("No data on player");
        }
        return null;
    }

    @SuppressWarnings("DataFlowIssue")
    private static @NotNull Horse buildNewHorse(ServerLevel sl) {
        return EntityType.HORSE.create(sl);
    }

    @Override
    public boolean hurt(
            DamageSource p_21016_,
            float p_21017_
    ) {
        Entity hurter = p_21016_.getEntity();
        if (hurter instanceof ServerPlayer sp) {
            handleHorseAbuse(sp);
        }
        this.heal(getMaxHealth());
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return false;
    }

    private void handleHorseAbuse(ServerPlayer sp) {
        sp.sendSystemMessage(Component.literal("DON'T DO THAT!")); // TODO: Translate
        sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 300));
        sp.hurt(DamageSource.ANVIL, 1f);
        sp.getLevel().playSound(null, getOnPos(), SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 1, 1);
        for (int i = 0; i < 15; i++) { // There's probably a better way to do a "big push", but oh well.
            sp.push(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel sl)) {
            return;
        }

        if (tick++ < Config.horseRefreshInterval && !everTicked) {
            return;
        }

        tick = 0;
        everTicked = true;

        Player p = level.getNearestPlayer(this, 16);
        if (p == null) {
            return;
        }

        updateIndex();
        HHNBT hpd = HHNBT.getPersistentData(this);
        moveToAnchor(hpd);

        if (index >= 0) {
            BlockPos pos = getOnPos();
            HHNBT pd = HHNBT.getPersistentData(p);
            CompoundTag pRegHorses = pd.getCompound(HHNBT.Key.REGISTERED_HORSES);
            if (shouldHide(sl, pRegHorses, hpd)) {
                setInvisible(true);
                return;
            }
            setInvisible(false);
            ImmutableList<String> ids = ImmutableList.copyOf(pRegHorses.getAllKeys());
            Tag horse = pRegHorses.get(ids.get(index));
            assumeFromTag(sl, (CompoundTag) horse);
            setPos(Vec3.atBottomCenterOf(pos.above()));
        }
    }

    private boolean shouldHide(
            ServerLevel sl,
            CompoundTag pRegHorses,
            HHNBT t
    ) {
        if (index > pRegHorses.size() - 1) {
            return true;
        }
        HHNBT.Key uuidKey = HHNBT.Key.REAL_HORSE_UUID;
        if (t.contains(uuidKey) && isRealHorseNearby(sl, t.getUUID(uuidKey))) {
            return true;
        }
        return false;
    }

    private boolean isRealHorseNearby(
            ServerLevel sl,
            UUID uuid
    ) {
        Entity horse = sl.getEntity(uuid);
        if (horse == null) {
            return false;
        }
        double distToRealHorse = horse.getOnPos().distSqr(getOnPos());
        return distToRealHorse < 500; // TODO: Config. Draw-distance?
    }

    private void moveToAnchor(HHNBT hpd) {
        BlockPos blockPos;
        if (hpd.contains(HHNBT.Key.ANCHOR_POS)) {
            blockPos = hpd.getBlockPos(HHNBT.Key.ANCHOR_POS);
        } else {
            blockPos = getOnPos().above();
            hpd.put(HHNBT.Key.ANCHOR_POS, blockPos);
        }

        setPos(Vec3.atBottomCenterOf(blockPos));
    }

    private void updateIndex() {
        HHNBT pd = HHNBT.getPersistentData(this);
        if (pd.contains(HHNBT.Key.REGISTERED_HORSE_INDEX)) {
            if (index >= 0) {
                return;
            }
            index = pd.getInt(HHNBT.Key.REGISTERED_HORSE_INDEX);
            return;
        }
        if (index >= 0) {
            pd.put(HHNBT.Key.REGISTERED_HORSE_INDEX, index);
            return;
        }
        if (!this.warned) {
            LOGGER.warn("Horse had no data but also no index. This is probably a bug.");
            this.warned = true;
        }
    }

    public void assumeFromTag(
            ServerLevel sl,
            CompoundTag tag
    ) {
        this.deserializeNBT(Registration.updateDataToFullHealth(sl, tag));
        HHNBT pd = HHNBT.getPersistentData(this);
        pd.put(HHNBT.Key.REAL_HORSE_UUID, this.getUUID());
        entityData.set(ARMOR_ITEM, ItemStack.of(new HHNBT(() -> tag).getCompound(HHNBT.Key.ARMOR_ITEM)));
        this.setUUID(UUID.randomUUID());
    }

    public ItemStack getArmor() {
        return entityData.get(ARMOR_ITEM);
    }
}
