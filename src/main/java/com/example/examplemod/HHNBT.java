package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
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
        LOGGER.debug("Stored[{}]:{}", buildKey(key), uuid);
    }

    private static void put(
            CompoundTag tag,
            Key key,
            ListTag l
    ) {
        tag.put(buildKey(key), l);
        LOGGER.debug("Stored[{}]:{}", buildKey(key), l);
    }

    private static @NotNull String buildKey(Key key) {
        return "horsehotel_" + key.value;
    }

    private static ListTag getOrDefault(
            CompoundTag persistentData,
            Key key,
            Supplier<ListTag> o
    ) {
        if (!persistentData.contains(buildKey(key))) {
            return o.get();
        }
        return persistentData.getList(buildKey(key), Tag.TAG_COMPOUND);
    }

    public static HHNBT getPersistentData(Entity player) {
        return new HHNBT(player::getPersistentData);
    }

    public UUID getUUID(Key key) {
        return getUUID(delegate.get(), key);
    }

    public boolean contains(Key key) {
        return contains(delegate.get(), key);
    }

    public ListTag getList(Key key) {
        return getList(delegate.get(), key);
    }

    public void put(
            Key key,
            UUID uuid
    ) {
        put(delegate.get(), key, uuid);
    }

    public ListTag getOrDefault(
            Key key,
            Supplier<ListTag> o
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

    public enum Key {
        REGISTERED_HORSES("registered_horses"), REAL_HORSE_UUID("real_horse_uuid");

        private final String value;

        Key(String value) {
            this.value = value;
        }
    }
}
