package ca.bradj.horsehotel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

public class RegisterBlock extends Block implements EntityBlock {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String ID = "register_block";

    public RegisterBlock() {
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
        tryUseRegisterBlock(p_60507_, player);
        return InteractionResult.CONSUME;
    }

    private void tryUseRegisterBlock(
            InteractionHand hand,
            Player player
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }

        if (player.isCrouching()) {
            HHNBT pd = HHNBT.getPersistentData(sp);
            pd.remove(HHNBT.Key.REGISTERED_HORSES);
            return;
        }

        Registration.interactWithRegistry(sp);
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
