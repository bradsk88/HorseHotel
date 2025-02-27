package ca.bradj.horsehotel.network;

import ca.bradj.horsehotel.HorseHotel;
import ca.bradj.horsehotel.compat.Compat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class HHNetwork {

    public static final String NETWORK_VERSION = "0.0.1";

    private static int messageIndex = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild(HorseHotel.MODID, "network"),
            () -> NETWORK_VERSION,
            version -> version.equals(NETWORK_VERSION),
            version -> version.equals(NETWORK_VERSION)
    );

    public static void init() {
        initMessagesToServer();
        initMessagesToClient();
    }

    private static void initMessagesToServer() {
        Compat.withConsumer(
                registerMessage(SummonHorseMessage.class, NetworkDirection.PLAY_TO_SERVER).
                        encoder(SummonHorseMessage::encode).
                        decoder(SummonHorseMessage::decode),
                SummonHorseMessage::handle
        ).add();
    }

    private static void initMessagesToClient() {
        Compat.withConsumer(
                registerMessage(ShowHorseSummonScreenMessage.class, NetworkDirection.PLAY_TO_CLIENT).
                        encoder(ShowHorseSummonScreenMessage::encode).
                        decoder(ShowHorseSummonScreenMessage::decode),
                ShowHorseSummonScreenMessage::handle
        ).add();
    }

    public static <T> SimpleChannel.MessageBuilder<T> registerMessage(
            Class<T> msgClass,
            NetworkDirection dir
    ) {
        return CHANNEL.messageBuilder(msgClass, messageIndex++, dir);
    }
}
