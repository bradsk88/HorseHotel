package ca.bradj.horsehotel.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * Opens the purchase-confirm screen for a single Stock horse — the buy counterpart of
 * {@link ShowHorseRegisterScreenMessage}. Carries the horse NBT to render, its shop UUID to quote
 * back on confirm, and the stable position so the confirm reaches the right Attendant.
 */
public final class ShowPurchaseScreenMessage extends ClientAccessMessage {
    public final UUID stockUuid;
    public final CompoundTag horseData;
    public final BlockPos stablePos;

    public ShowPurchaseScreenMessage(
            UUID stockUuid,
            CompoundTag horseData,
            BlockPos stablePos
    ) {
        this.stockUuid = stockUuid;
        this.horseData = horseData;
        this.stablePos = stablePos;
    }

    public static void encode(
            ShowPurchaseScreenMessage msg,
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(msg.stockUuid);
        buffer.writeNbt(msg.horseData);
        buffer.writeBlockPos(msg.stablePos);
    }

    public static ShowPurchaseScreenMessage decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();
        CompoundTag data = buffer.readNbt();
        BlockPos pos = buffer.readBlockPos();
        return new ShowPurchaseScreenMessage(uuid, data, pos);
    }
}
