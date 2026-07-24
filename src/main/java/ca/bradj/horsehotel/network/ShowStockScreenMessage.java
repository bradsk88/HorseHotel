package ca.bradj.horsehotel.network;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Opens the "Horses for Sale" tab with the Stable's current Stock. Carries the stable position (to
 * quote back on tab-switch and purchase) and whether the "Your Horses" tab should be offered
 * alongside it — the cheap enabled-tab flag that lets tabs render without fetching the other tab's
 * data.
 */
public final class ShowStockScreenMessage extends ClientAccessMessage {
    private final ImmutableList<UIStockHorse> stock;
    private final boolean yourHorsesAvailable;
    private final BlockPos stablePos;

    public ShowStockScreenMessage(
            ImmutableList<UIStockHorse> stock,
            boolean yourHorsesAvailable,
            BlockPos stablePos
    ) {
        this.stock = stock;
        this.yourHorsesAvailable = yourHorsesAvailable;
        this.stablePos = stablePos;
    }

    public static void encode(
            ShowStockScreenMessage msg,
            FriendlyByteBuf buffer
    ) {
        buffer.writeCollection(msg.stock, UIStockHorse::toNetwork);
        buffer.writeBoolean(msg.yourHorsesAvailable);
        buffer.writeBlockPos(msg.stablePos);
    }

    public static ShowStockScreenMessage decode(FriendlyByteBuf buffer) {
        ImmutableList<UIStockHorse> stock = ImmutableList.copyOf(buffer.readList(UIStockHorse::fromNetwork));
        boolean yourHorses = buffer.readBoolean();
        BlockPos pos = buffer.readBlockPos();
        return new ShowStockScreenMessage(stock, yourHorses, pos);
    }

    public ImmutableList<UIStockHorse> stock() {
        return stock;
    }

    public boolean yourHorsesAvailable() {
        return yourHorsesAvailable;
    }

    public BlockPos stablePos() {
        return stablePos;
    }
}
