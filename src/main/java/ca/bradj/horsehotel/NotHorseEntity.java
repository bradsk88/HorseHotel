package ca.bradj.horsehotel;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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

    protected NotHorseEntity(
            Level p_20967_
    ) {
        super(EntitiesInit.VISITOR.get(), p_20967_);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ID_TYPE_VARIANT, 0);
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
        if (Items.STRUCTURE_BLOCK.getDefaultInstance().sameItem(player.getItemInHand(p_19979_))) {
            HHNBT pd = HHNBT.getPersistentData(this);
            int index = -1;
            if (pd.contains(HHNBT.Key.REGISTERED_HORSE_INDEX) && !player.isCrouching()) {
                index = pd.getInt(HHNBT.Key.REGISTERED_HORSE_INDEX);
            }
            pd.put(HHNBT.Key.REGISTERED_HORSE_INDEX, index +1);
            LOGGER.debug("Data is now: {}", pd);
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
            LOGGER.error("Fake horse has no real-horse UUID in data {}", pd);
            return;
        }

        int uuid = pd.getInt(HHNBT.Key.REGISTERED_HORSE_INDEX);

        @Nullable Horse spawned = respawnRealHorse(uuid, getOnPos(), sp, sp.getLevel());

        if (spawned != null) {
            this.remove(RemovalReason.DISCARDED);
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
        ListTag list = pd.getList(HHNBT.Key.REGISTERED_HORSES);
        list.removeIf(
                tag -> {
                    @NotNull Horse holder = buildNewHorse(sp.getLevel());
                    holder.deserializeNBT((CompoundTag) tag);
                    return holder.getUUID().equals(spawnedHorse);
                }
        );
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
            ListTag l = pd.getList(HHNBT.Key.REGISTERED_HORSES);
            if (l.size() < horseIndex + 1) {
                LOGGER.debug("Index {} is greater than registry size {}", horseIndex, l.size());
                return null;
            }
            newone = buildNewHorse(sl);
            newone.deserializeNBT((CompoundTag) l.get(horseIndex));
            newone.setPos(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ());
            l.remove(horseIndex);
            sl.addFreshEntity(newone);
            return newone;
        } else {
            LOGGER.debug("No data");
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
}
