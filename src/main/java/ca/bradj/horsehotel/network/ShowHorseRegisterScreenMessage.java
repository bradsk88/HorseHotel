package ca.bradj.horsehotel.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class ShowHorseRegisterScreenMessage extends ClientAccessMessage {
    public final CompoundTag horseData;

    public ShowHorseRegisterScreenMessage(CompoundTag horseData) {
        this.horseData = horseData;
    }

    public static void encode(
            ShowHorseRegisterScreenMessage msg,
            FriendlyByteBuf buffer
    ) {
        buffer.writeNbt(msg.horseData);
    }

    public static ShowHorseRegisterScreenMessage decode(FriendlyByteBuf buffer) {
        return new ShowHorseRegisterScreenMessage(buffer.readNbt());
    }

}
