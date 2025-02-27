package ca.bradj.horsehotel.network;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ToClientMessage {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handle(
            Supplier<NetworkEvent.Context> ctx,
            ClientRunnable o
    ) {
        final AtomicBoolean success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> {
                        o.runOnClient();
                        success.set(true);
                    }
            );
        }).exceptionally(err -> logError(o.getClass(), err));
        ctx.get().setPacketHandled(true);
    }

    private static Void logError(
            Class<? extends ClientRunnable> aClass,
            Throwable ex
    ) {
        String name = aClass.getName();
        LOGGER.error("Failed to send {} data to player", name, ex);
        return null;
    }
}
