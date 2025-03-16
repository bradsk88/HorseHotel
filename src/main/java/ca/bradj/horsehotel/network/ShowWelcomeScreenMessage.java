package ca.bradj.horsehotel.network;

import net.minecraft.network.FriendlyByteBuf;

public final class ShowWelcomeScreenMessage extends ClientAccessMessage {

    public static void encode(
            ShowWelcomeScreenMessage msg,
            FriendlyByteBuf buffer
    ) {
    }

    public static ShowWelcomeScreenMessage decode(FriendlyByteBuf buffer) {
        return new ShowWelcomeScreenMessage();
    }

}
