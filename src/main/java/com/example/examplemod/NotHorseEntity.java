package com.example.examplemod;

import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NotHorseEntity extends LivingEntity {
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
        return PathfinderMob
                .createMobAttributes()
                .build();
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
            return super.interact(player, p_19979_);
        }

        Horse newOne = RegisterBlock.getHorseForWorld(
                this.getPersistentData().getUUID("horsehotel_stored_horse"),
                player.getOnPos(), player, sp.getLevel(), true
        );

        if (newOne != null) {
            this.remove(RemovalReason.DISCARDED);
            sp.getLevel().addFreshEntity(newOne);
            sp.startRiding(newOne);
        }
        return InteractionResult.CONSUME;
    }
}
