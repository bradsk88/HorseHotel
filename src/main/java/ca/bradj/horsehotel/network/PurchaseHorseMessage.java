package ca.bradj.horsehotel.network;

import ca.bradj.horsehotel.Purchasing;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * A player confirming a purchase. Carries the Stock horse's shop UUID, not a slot index: a Restock
 * can replace the whole Stock between the screen opening and this click, and the UUID either still
 * identifies a horse or does not. The stable position lets the server find the right Attendant.
 */
public record PurchaseHorseMessage(
        UUID stockUuid,
        BlockPos stablePos
) {

    public static void encode(
            PurchaseHorseMessage msg,
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(msg.stockUuid());
        buffer.writeBlockPos(msg.stablePos());
    }

    public static PurchaseHorseMessage decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();
        BlockPos pos = buffer.readBlockPos();
        return new PurchaseHorseMessage(uuid, pos);
    }

    public void handle(
            Supplier<NetworkEvent.Context> ctx
    ) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Purchasing.purchase(sender, stockUuid(), stablePos());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
