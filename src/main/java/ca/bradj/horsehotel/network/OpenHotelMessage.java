package ca.bradj.horsehotel.network;

import ca.bradj.horsehotel.HotelUI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * A request to open the hotel UI on a particular tab — the round-trip that both the initial
 * interaction and a tab click send. The server gathers only that tab's data and replies with the
 * matching show-screen message. An empty {@link #tab} means "pick the default", resolved from what
 * the player's Registry and the Stable's Stock actually hold.
 */
public record OpenHotelMessage(
        BlockPos stablePos,
        String tab
) {
    public static final String DEFAULT = "";
    public static final String YOUR_HORSES = "your_horses";
    public static final String FOR_SALE = "for_sale";

    public static void encode(
            OpenHotelMessage msg,
            FriendlyByteBuf buffer
    ) {
        buffer.writeBlockPos(msg.stablePos());
        buffer.writeUtf(msg.tab());
    }

    public static OpenHotelMessage decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        String tab = buffer.readUtf();
        return new OpenHotelMessage(pos, tab);
    }

    public void handle(
            Supplier<NetworkEvent.Context> ctx
    ) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                HotelUI.open(sender, stablePos(), tab());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
