package ca.bradj.horsehotel.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record UIHorse(
        int horseIndex,
        CompoundTag horseData
) {
    public static void toNetwork(
            FriendlyByteBuf friendlyByteBuf,
            UIHorse uiHorse
    ) {
        friendlyByteBuf.writeInt(uiHorse.horseIndex());
        friendlyByteBuf.writeNbt(uiHorse.horseData());
    }

    public static UIHorse fromNetwork(FriendlyByteBuf friendlyByteBuf) {
        int horseIndex = friendlyByteBuf.readInt();
        CompoundTag horseData = friendlyByteBuf.readNbt();
        return new UIHorse(horseIndex, horseData);
    }
}
