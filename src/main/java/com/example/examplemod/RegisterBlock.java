package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RegisterBlock extends Block implements EntityBlock {


    public static final Logger LOGGER = LogManager.getLogger();
    public static final String ID = "register_block";

    public RegisterBlock() {
        super(Properties
                .of(Material.GLASS, MaterialColor.TERRACOTTA_BROWN)
                .strength(1.0F, 10.0F)
                .noOcclusion());
    }

    @Override
    public InteractionResult use(
            BlockState p_60503_,
            Level lvl,
            BlockPos bp,
            Player player,
            InteractionHand p_60507_,
            BlockHitResult p_60508_
    ) {
        if (p_60507_ != InteractionHand.MAIN_HAND) {
            return super.use(p_60503_, lvl, bp, player, p_60507_, p_60508_);
        }
        if (player.isPassenger()) {
            net.minecraft.world.entity.Entity veh = player.getVehicle();
            if (veh instanceof Horse vh) {
                CompoundTag data = vh.serializeNBT();
                LOGGER.debug("Horse: {}", data);
                if (lvl instanceof ServerLevel sl) {
                    storeHorseOnPlayer(bp, player, sl, data, veh);
                }
            }
        } else {
            if (lvl instanceof ServerLevel sl) {
                Horse newOne = getHorseForWorld(null, bp, player, sl, false);
                if (newOne != null) {
                    sl.addFreshEntity(newOne);
                }
            }
        }
        return super.use(p_60503_, lvl, bp, player, p_60507_, p_60508_);
    }

    public static Horse getHorseForWorld(
            @Nullable UUID horseToGet,
            BlockPos bp,
            Player player,
            ServerLevel sl,
            boolean realHorse
    ) {
        CompoundTag pd = player.getPersistentData();
        String key = "horsehotel_registered_horses";
        LivingEntity newone = null;
        if (pd.contains(key)) {
            ListTag l = pd.getList(key, Tag.TAG_COMPOUND);
            for (Tag tag : l) {
                if (realHorse) {
                    newone = EntityType.HORSE.create(sl);
                } else {
                    newone = EntitiesInit.VISITOR.get().create(sl);
                }
                newone.deserializeNBT((CompoundTag) tag);
                if (!realHorse) {
                    newone.getPersistentData().putUUID("horsehotel_stored_horse", newone.getUUID());
                    newone.setUUID(UUID.randomUUID());
                }
                newone.setPos(bp.getX(), bp.getY() + 1, bp.getZ());

                if (realHorse && newone.getUUID().equals(horseToGet)) {
                    pd.remove(key);
                    return (Horse) newone;
                } else {
                    sl.addFreshEntity(newone);
                }
            }
        } else {
            LOGGER.debug("No data");
        }
        return null;
    }

    public static void storeHorseOnPlayer(
            BlockPos bp,
            Player player,
            ServerLevel sl,
            CompoundTag data,
            net.minecraft.world.entity.Entity veh
    ) {
        BlockEntity ent = sl.getBlockEntity(bp);
        if (ent instanceof Entity rbe) {
            CompoundTag pd = player.getPersistentData();
            String key = "horsehotel_registered_horses";
            ListTag l;
            if (pd.contains(key)) {
                l = pd.getList(key, Tag.TAG_COMPOUND);
            } else {
                l = new ListTag();
            }
            l.add(data);
            pd.put(key, l);
            veh.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos blockPos,
            BlockState blockState
    ) {
        return new Entity(blockPos, blockState);
    }

    public static class Entity extends BlockEntity {

        public Entity(
                BlockPos p_155229_,
                BlockState p_155230_
        ) {
            super(TilesInit.REGISTER_BLOCK.get(), p_155229_, p_155230_);
        }
    }
}
