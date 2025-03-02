package ca.bradj.horsehotel.network;

import ca.bradj.horsehotel.Registration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record RegisterRiddenHorseMessage() {

    public static void encode(
            RegisterRiddenHorseMessage msg,
            FriendlyByteBuf buffer
    ) {
    }

    public static RegisterRiddenHorseMessage decode(FriendlyByteBuf buffer) {
        return new RegisterRiddenHorseMessage();
    }


    public void handle(
            Supplier<NetworkEvent.Context> ctx
    ) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = getSenderUnsafe(ctx);
            Registration.storeRiddenHorse(sender);
        });
        ctx.get().setPacketHandled(true);

    }

    @SuppressWarnings("DataFlowIssue")
    private static @NotNull ServerPlayer getSenderUnsafe(Supplier<NetworkEvent.Context> ctx) {
        return ctx.get().getSender();
    }
}
