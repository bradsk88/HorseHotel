package ca.bradj.horsehotel.network;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

public final class ShowHorseSummonScreenMessage extends ClientAccessMessage {
    private final ImmutableList<UIHorse> horses;

    public ShowHorseSummonScreenMessage(ImmutableList<UIHorse> horses) {
        this.horses = horses;
    }

    public static void encode(
            ShowHorseSummonScreenMessage msg,
            FriendlyByteBuf buffer
    ) {
        buffer.writeCollection(msg.horses, UIHorse::toNetwork);
    }

    public static ShowHorseSummonScreenMessage decode(FriendlyByteBuf buffer) {
        return new ShowHorseSummonScreenMessage(ImmutableList.copyOf(buffer.readList(UIHorse::fromNetwork)));
    }

    public ImmutableList<UIHorse> horses() {
        return horses;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ShowHorseSummonScreenMessage) obj;
        return Objects.equals(this.horses, that.horses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(horses);
    }

    @Override
    public String toString() {
        return "ShowHorseSummonScreenMessage[" +
                "horses=" + horses + ']';
    }

}
