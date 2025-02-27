package ca.bradj.horsehotel.network;

import ca.bradj.horsehotel.NotHorseEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SummonHorseMessage(
        int horseIndex
) {

    public static void encode(
            SummonHorseMessage msg,
            FriendlyByteBuf buffer
    ) {
        buffer.writeInt(msg.horseIndex());
    }

    public static SummonHorseMessage decode(FriendlyByteBuf buffer) {
        int horseIndex = buffer.readInt();
        return new SummonHorseMessage(horseIndex);
    }


    public void handle(
            Supplier<NetworkEvent.Context> ctx
    ) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            NotHorseEntity.respawnRealHorse(horseIndex(), sender.getOnPos(), sender, sender.getLevel());
        });
        ctx.get().setPacketHandled(true);

    }
}
