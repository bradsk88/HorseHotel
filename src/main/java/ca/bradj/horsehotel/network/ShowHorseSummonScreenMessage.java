package ca.bradj.horsehotel.network;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

/**
 * Opens the "Your Horses" tab with the player's Registry. Also carries the stable position and
 * whether the "Horses for Sale" tab should be offered alongside it — the cheap enabled-tab flag
 * that lets tabs render without fetching the Stock.
 */
public final class ShowHorseSummonScreenMessage extends ClientAccessMessage {
    private final ImmutableList<UIHorse> horses;
    private final boolean forSaleAvailable;
    private final BlockPos stablePos;

    public ShowHorseSummonScreenMessage(
            ImmutableList<UIHorse> horses,
            boolean forSaleAvailable,
            BlockPos stablePos
    ) {
        this.horses = horses;
        this.forSaleAvailable = forSaleAvailable;
        this.stablePos = stablePos;
    }

    public static void encode(
            ShowHorseSummonScreenMessage msg,
            FriendlyByteBuf buffer
    ) {
        buffer.writeCollection(msg.horses, UIHorse::toNetwork);
        buffer.writeBoolean(msg.forSaleAvailable);
        buffer.writeBlockPos(msg.stablePos);
    }

    public static ShowHorseSummonScreenMessage decode(FriendlyByteBuf buffer) {
        ImmutableList<UIHorse> horses = ImmutableList.copyOf(buffer.readList(UIHorse::fromNetwork));
        boolean forSale = buffer.readBoolean();
        BlockPos pos = buffer.readBlockPos();
        return new ShowHorseSummonScreenMessage(horses, forSale, pos);
    }

    public ImmutableList<UIHorse> horses() {
        return horses;
    }

    public boolean forSaleAvailable() {
        return forSaleAvailable;
    }

    public BlockPos stablePos() {
        return stablePos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ShowHorseSummonScreenMessage) obj;
        return Objects.equals(this.horses, that.horses)
                && this.forSaleAvailable == that.forSaleAvailable
                && Objects.equals(this.stablePos, that.stablePos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(horses, forSaleAvailable, stablePos);
    }

    @Override
    public String toString() {
        return "ShowHorseSummonScreenMessage[horses=" + horses
                + ", forSaleAvailable=" + forSaleAvailable
                + ", stablePos=" + stablePos + ']';
    }
}
