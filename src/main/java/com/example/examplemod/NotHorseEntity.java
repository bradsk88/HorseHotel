package com.example.examplemod;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
        if (!pd.contains(HHNBT.Key.REAL_HORSE_UUID)) {
            LOGGER.error("Fake horse has no real-horse UUID");
            return;
        }

        UUID uuid = pd.getUUID(HHNBT.Key.REAL_HORSE_UUID);

        @Nullable Horse spawned = respawnRealHorse(uuid, getOnPos(), sp, sp.getLevel());

        if (spawned != null) {
            this.remove(RemovalReason.DISCARDED);
            deRegisterHorse(sp, spawned.getUUID());
            sp.getLevel().addFreshEntity(spawned);
            sp.startRiding(spawned);
        }
    }

    private static void deRegisterHorse(ServerPlayer sp, UUID spawnedHorse) {
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
            UUID horseUUID,
            BlockPos spawnPos,
            Player player,
            ServerLevel sl
    ) {
        HHNBT pd = HHNBT.getPersistentData(player);
        Horse newone;
        if (pd.contains(HHNBT.Key.REGISTERED_HORSES)) {
            ListTag l = pd.getList(HHNBT.Key.REGISTERED_HORSES);
            for (int i = 0; i < l.size(); i++) {
                newone = buildNewHorse(sl);
                newone.deserializeNBT((CompoundTag) l.get(i));
                newone.setPos(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ());
                if (newone.getUUID().equals(horseUUID)) {
                    l.remove(i);
                    sl.addFreshEntity(newone);
                    return newone;
                }
            }
        } else {
            LOGGER.debug("No data");
        }
        return null;
    }

    @SuppressWarnings("DataFlowIssue")
    private static @NotNull Horse buildNewHorse(ServerLevel sl) {
        return EntityType.HORSE.create(sl);
    }
}
