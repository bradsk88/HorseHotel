package ca.bradj.horsehotel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FakeHorseSpawnBlock extends BaseEntityBlock implements EntityBlock {


    public static final Logger LOGGER = LogManager.getLogger();
    public static final String ID = "fake_horse_spawn_block";

    public FakeHorseSpawnBlock() {
        super(Properties.of(Material.GLASS, MaterialColor.TERRACOTTA_BROWN).strength(1.0F, 10.0F).noOcclusion());
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
        if (player.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (Items.STRUCTURE_BLOCK.getDefaultInstance().sameItem(player.getItemInHand(p_60507_))) {
            HHNBT pd = HHNBT.getPersistentData(lvl.getBlockEntity(bp));
            int index = -1;
            if (pd.contains(HHNBT.Key.REGISTERED_HORSE_INDEX) && !player.isCrouching()) {
                index = pd.getInt(HHNBT.Key.REGISTERED_HORSE_INDEX);
            }
            pd.put(HHNBT.Key.REGISTERED_HORSE_INDEX, index + 1);
            LOGGER.debug("Data is now: {}", pd);
            return InteractionResult.CONSUME;
        }
        LOGGER.debug("Data is: {}", HHNBT.getPersistentData(lvl.getBlockEntity(bp)));
        return InteractionResult.CONSUME;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos blockPos,
            BlockState blockState
    ) {
        return new Entity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level p_153212_,
            BlockState p_153213_,
            BlockEntityType<T> p_153214_
    ) {
        return p_153212_.isClientSide ? null : createTickerHelper(
                p_153214_, TilesInit.SPAWN_BLOCK.get(), Entity::tick
        );
    }

    public static class Entity extends BlockEntity {

        boolean settingUp;

        public Entity(
                BlockPos p_155229_,
                BlockState p_155230_
        ) {
            super(TilesInit.SPAWN_BLOCK.get(), p_155229_, p_155230_);
        }

        public static void tick(
                Level level,
                BlockPos pos,
                BlockState p_155255_,
                Entity p_155256_
        ) {
            if (p_155256_.settingUp) {
                return;
            }
            HHNBT pd = HHNBT.getPersistentData(p_155256_);
            if (!pd.contains(HHNBT.Key.REGISTERED_HORSE_INDEX)) {
                p_155256_.settingUp = true;
                LOGGER.info("Spawn Block is in setup mode.");
                LOGGER.info("You can modify its index by right-clicking with a structure block in hand.");
                LOGGER.info("When the world reloads, this block will be replaced by a horse entity");
                return;
            }
            int index = pd.getInt(HHNBT.Key.REGISTERED_HORSE_INDEX);
            NotHorseEntity horse = createHorseWithIndex(level, index);
            horse.setPos(Vec3.atBottomCenterOf(pos.above()));
            level.addFreshEntity(horse);
            level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
            level.removeBlockEntity(pos);

            if (index == 0) {
                level.addFreshEntity(EntitiesInit.STABLE_ATTENDANT.get().create(level));
            }
        }

        @SuppressWarnings("DataFlowIssue")
        private static @NotNull NotHorseEntity createHorseWithIndex(
                Level level,
                int index
        ) {
            NotHorseEntity horse = new NotHorseEntity(level, index);
            HHNBT pd2 = HHNBT.getPersistentData(horse);
            pd2.put(HHNBT.Key.REGISTERED_HORSE_INDEX, index);
            return horse;
        }
    }
}
