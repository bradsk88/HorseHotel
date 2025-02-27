package ca.bradj.horsehotel.network;

import ca.bradj.horsehotel.gui.ClientAccess;
import com.google.common.collect.ImmutableList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ShowHorseSummonScreenMessage(
        ImmutableList<UIHorse> horses
) implements ClientRunnable {

    public static void encode(
            ShowHorseSummonScreenMessage msg,
            FriendlyByteBuf buffer
    ) {
        buffer.writeCollection(msg.horses, UIHorse::toNetwork);
    }

    public static ShowHorseSummonScreenMessage decode(FriendlyByteBuf buffer) {
        return new ShowHorseSummonScreenMessage(
                ImmutableList.copyOf(buffer.readList(UIHorse::fromNetwork))
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> ctx
    ) {
        ToClientMessage.handle(ctx, this);
    }

    @Override
    public void runOnClient() {
        ClientAccess.openHorseSummonScreen(horses);
    }
}
