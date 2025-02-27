package ca.bradj.horsehotel;

import ca.bradj.horsehotel.network.HHNetwork;
import ca.bradj.horsehotel.network.ShowHorseSummonScreenMessage;
import ca.bradj.horsehotel.network.UIHorse;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RegisterBlock extends Block implements EntityBlock {


    public static final Logger LOGGER = LogManager.getLogger();
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
        tryUseRegisterBlock(p_60507_, bp, player);
        return super.use(p_60503_, lvl, bp, player, p_60507_, p_60508_);
    }

    private void tryUseRegisterBlock(
            InteractionHand hand,
            BlockPos bp,
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

        useRegisterBlock(sp);
    }

    private static void useRegisterBlock(
            ServerPlayer player
    ) {
        net.minecraft.world.entity.Entity veh = player.getVehicle();
        if (veh == null) {
            ImmutableList<UIHorse> fh = buildFakeHorses(player);
            ShowHorseSummonScreenMessage msg = new ShowHorseSummonScreenMessage(fh);
            HHNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
            return;
        }
        if (storeHorseOnPlayer(player, veh)) {
            veh.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    public static ImmutableList<UIHorse> buildFakeHorses(
            Player player
    ) {
        HHNBT pd = HHNBT.getPersistentData(player);
        ImmutableList.Builder<UIHorse> b = ImmutableList.builder();
        if (!pd.contains(HHNBT.Key.REGISTERED_HORSES)) {
            LOGGER.debug("No data");
            return b.build();
        }
        ListTag l = pd.getList(HHNBT.Key.REGISTERED_HORSES);
        for (int i = 0; i < l.size(); i++) {
            b.add(new UIHorse(i, l.getCompound(i)));
        }
        return b.build();
    }

    private static @NotNull NotHorseEntity buildFakeFromTag(
            ServerLevel sl,
            CompoundTag tag
    ) {
        NotHorseEntity newone = buildEmptyFakeHorse(sl);
        newone.assumeFromTag(tag);
        return newone;
    }

    @SuppressWarnings("DataFlowIssue")
    private static @NotNull NotHorseEntity buildEmptyFakeHorse(ServerLevel sl) {
        return EntitiesInit.FAKE_HORSE.get().create(sl);
    }


    public static boolean storeHorseOnPlayer(
            Player player,
            net.minecraft.world.entity.Entity veh
    ) {
        if (!(veh instanceof Horse vh)) {
            return false;
        }
        CompoundTag data = vh.serializeNBT();
        LOGGER.debug("Horse: {}", data);
        HHNBT pd = HHNBT.getPersistentData(player);
        ListTag l = pd.getOrDefault(HHNBT.Key.REGISTERED_HORSES, ListTag::new);
        l.add(data);
        pd.put(HHNBT.Key.REGISTERED_HORSES, l);
        return true;
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
