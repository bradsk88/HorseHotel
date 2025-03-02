package ca.bradj.horsehotel.network;

import ca.bradj.horsehotel.gui.ClientAccess;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class ClientAccessMessage implements ClientRunnable {
    @Override
    public void runOnClient() {
        ClientAccess.openScreenForMessage(this);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ToClientMessage.handle(ctx, this);
    }
}
