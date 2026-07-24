package ca.bradj.horsehotel.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * One horse in a Stable's Stock as shown in the For Sale tab. UUID-keyed rather than index-keyed
 * (unlike {@link UIHorse}) because a purchase quotes the shop UUID, which a Restock cannot silently
 * repoint the way it can an index.
 */
public record UIStockHorse(
        UUID stockUuid,
        CompoundTag horseData
) {
    public static void toNetwork(
            FriendlyByteBuf buffer,
            UIStockHorse horse
    ) {
        buffer.writeUUID(horse.stockUuid());
        buffer.writeNbt(horse.horseData());
    }

    public static UIStockHorse fromNetwork(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();
        CompoundTag data = buffer.readNbt();
        return new UIStockHorse(uuid, data);
    }
}
