package ca.bradj.horsehotel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Supplier;

public class HHNBT {

    public static final Logger LOGGER = LogManager.getLogger();
    private final Supplier<CompoundTag> delegate;

    public HHNBT(Supplier<CompoundTag> delegate) {
        this.delegate = delegate;
    }

    private static boolean contains(
            CompoundTag pd,
            Key key
    ) {
        return pd.contains(buildKey(key));
    }

    private static ListTag getList(
            CompoundTag pd,
            Key key
    ) {
        return pd.getList(buildKey(key), Tag.TAG_COMPOUND);
    }

    private static UUID getUUID(
            CompoundTag tag,
            Key key
    ) {
        return tag.getUUID(buildKey(key));
    }

    private static void put(
            CompoundTag tag,
            Key key,
            UUID uuid
    ) {
        tag.putUUID(buildKey(key), uuid);
//        LOGGER.debug("Stored[{}]:{}", buildKey(key), uuid);
    }

    private static void put(
            CompoundTag tag,
            Key key,
            ListTag l
    ) {
        tag.put(buildKey(key), l);
//        LOGGER.debug("Stored[{}]:{}", buildKey(key), l);
    }

    private static @NotNull String buildKey(Key key) {
        return HorseHotel.MODID + "_" + key.value;
    }

    private static CompoundTag getOrDefault(
            CompoundTag persistentData,
            Key key,
            Supplier<CompoundTag> o
    ) {
        if (!persistentData.contains(buildKey(key))) {
            return o.get();
        }
        return persistentData.getCompound(buildKey(key));
    }

    public static HHNBT getPersistentData(Entity player) {
        return new HHNBT(player::getPersistentData);
    }

    public static HHNBT getPersistentData(BlockEntity player) {
        return new HHNBT(player::getPersistentData);
    }

    public UUID getUUID(Key key) {
        return getUUID(delegate.get(), key);
    }

    public boolean contains(Key key) {
        return contains(delegate.get(), key);
    }

    public void put(
            Key key,
            UUID uuid
    ) {
        put(delegate.get(), key, uuid);
    }

    public CompoundTag getOrDefault(
            Key key,
            Supplier<CompoundTag> o
    ) {
        return getOrDefault(delegate.get(), key, o);
    }

    public void put(
            Key key,
            ListTag l
    ) {
        put(delegate.get(), key, l);
    }

    public void remove(Key key) {
        delegate.get().remove(buildKey(key));
    }

    public int getInt(Key key) {
        return delegate.get().getInt(buildKey(key));
    }

    public void put(
            Key key,
            int i
    ) {
        delegate.get().putInt(buildKey(key), i);
    }

    public void put(
            Key key,
            BlockPos above
    ) {
        CompoundTag t = new CompoundTag();
        t.putInt("x", above.getX());
        t.putInt("y", above.getY());
        t.putInt("z", above.getZ());
        delegate.get().put(buildKey(key), t);
    }

    public BlockPos getBlockPos(Key key) {
        CompoundTag d = delegate.get();
        CompoundTag c = d.getCompound(buildKey(key));
        return new BlockPos(
                c.getInt("x"),
                c.getInt("y"),
                c.getInt("z")
        );
    }

    public void put(
            Key key,
            CompoundTag compoundTag
    ) {
        delegate.get().put(buildKey(key), compoundTag);
    }

    public CompoundTag getCompound(Key key) {
        return delegate.get().getCompound(buildKey(key));
    }

    public enum Key {
        REGISTERED_HORSES("registered_horses"),
        REAL_HORSE_UUID("real_horse_uuid"),
        REGISTERED_HORSE_INDEX("registered_horse_index"),
        ANCHOR_POS("anchor_pos"),
        ARMOR_ITEM("armor_item");

        private final String value;

        Key(String value) {
            this.value = value;
        }
    }

    @Override
    public String toString() {
        return delegate.get().toString();
    }
}
