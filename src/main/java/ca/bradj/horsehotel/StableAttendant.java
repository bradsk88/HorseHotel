package ca.bradj.horsehotel;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class StableAttendant extends Mob {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String ID = "stable_attendant";
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(
            StableAttendant.class,
            EntityDataSerializers.INT
    );
    private int tick = 0;
    private int index;
    private boolean warned;

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
        this.index = index;
        setItemSlot(EquipmentSlot.HEAD, ItemsInit.COWBOY_HAT.get().getDefaultInstance());
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

        if (tick++ < 100) {
            return;
        }

        tick = 0;

        Player p = level.getNearestPlayer(this, 16);
        if (p == null) {
            return;
        }

        HHNBT hpd = HHNBT.getPersistentData(this);
        BlockPos blockPos;
        if (hpd.contains(HHNBT.Key.ANCHOR_POS)) {
            blockPos = hpd.getBlockPos(HHNBT.Key.ANCHOR_POS);
        } else {
            blockPos = getOnPos().above();
            hpd.put(HHNBT.Key.ANCHOR_POS, blockPos);
        }

        setPos(Vec3.atBottomCenterOf(blockPos));
    }

    public void assumeFromTag(CompoundTag tag) {
        this.deserializeNBT(tag);
        HHNBT pd = HHNBT.getPersistentData(this);
        pd.put(HHNBT.Key.REAL_HORSE_UUID, this.getUUID());
        this.setUUID(UUID.randomUUID());
    }
}
