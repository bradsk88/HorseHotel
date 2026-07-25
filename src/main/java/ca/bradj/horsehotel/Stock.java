package ca.bradj.horsehotel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A Stable's Stock — the horses it currently has for sale, held by its {@link StableAttendant}.
 * <p>
 * Stock is an <em>ordered, fixed-length slot list</em>, not a keyed map: barn Stall {@code i}
 * shows slot {@code i}, so order is load-bearing. The length is the count rolled at the last
 * {@link #restock}; a {@link #purchase} nulls its slot rather than compacting, so surviving
 * horses never change Stall. Serialized as a {@link ListTag} of slot compounds under
 * {@link HHNBT.Key#STOCK} in the Attendant's persistent data (empty slots included).
 * <p>
 * A filled slot carries {@link #KEY_UUID} (a shop identity, distinct from the horse's own UUID)
 * and {@link #KEY_DATA} (the horse NBT). An empty slot is an empty compound.
 */
public final class Stock {

    private static final String KEY_UUID = "u";
    private static final String KEY_DATA = "d";

    private Stock() {
    }

    /**
     * Discards any existing Stock and rolls a fresh assortment: a new count in
     * {@code 1..maxForSale} (never empty when {@code maxForSale >= 1}) and new
     * {@link #rollHorse plain horses}.
     */
    public static void restock(
            ServerLevel level,
            HHNBT attendantData,
            BlockPos near,
            int maxForSale
    ) {
        RandomSource rand = level.getRandom();
        // Always keep at least one horse for sale so a stable is never a dead end; roll 1..maxForSale.
        int n = maxForSale <= 0 ? 0 : 1 + rand.nextInt(maxForSale);
        setStock(level, attendantData, near, n);
    }

    /**
     * Rolls a Stock of exactly {@code n} horses, replacing any existing Stock. {@link #restock}
     * uses this with a random count; the autotest uses it to build a known Stock.
     */
    public static void setStock(
            ServerLevel level,
            HHNBT attendantData,
            BlockPos near,
            int n
    ) {
        ListTag slots = new ListTag();
        for (int i = 0; i < n; i++) {
            CompoundTag slot = new CompoundTag();
            slot.putUUID(KEY_UUID, UUID.randomUUID());
            slot.put(KEY_DATA, rollHorse(level, near));
            slots.add(slot);
        }
        attendantData.put(HHNBT.Key.STOCK, slots);
    }

    /**
     * A plain wild-caught horse: a real {@link Horse} finalized by vanilla so its variant and
     * stats land in vanilla-plausible ranges, then serialized. Never added to the world.
     */
    private static CompoundTag rollHorse(
            ServerLevel level,
            BlockPos near
    ) {
        Horse horse = EntityType.HORSE.create(level);
        if (horse == null) {
            return new CompoundTag();
        }
        horse.moveTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5, 0, 0);
        horse.finalizeSpawn(level, level.getCurrentDifficultyAt(near), MobSpawnType.SPAWNER, null, null);
        return horse.serializeNBT();
    }

    private static ListTag slots(HHNBT attendantData) {
        return attendantData.getList(HHNBT.Key.STOCK);
    }

    /** The number of Stalls this Stock occupies, empty slots included. */
    public static int size(HHNBT attendantData) {
        return slots(attendantData).size();
    }

    /** True when the Stable has no horses for sale at all (no slots, or every slot empty). */
    public static boolean isEmpty(HHNBT attendantData) {
        ListTag slots = slots(attendantData);
        for (int i = 0; i < slots.size(); i++) {
            if (isFilled(slots.getCompound(i))) {
                return false;
            }
        }
        return true;
    }

    /** The horse NBT in slot {@code index}, or null if the slot is empty or out of range. */
    public static @Nullable CompoundTag horseData(
            HHNBT attendantData,
            int index
    ) {
        ListTag slots = slots(attendantData);
        if (index < 0 || index >= slots.size()) {
            return null;
        }
        CompoundTag slot = slots.getCompound(index);
        return isFilled(slot) ? slot.getCompound(KEY_DATA) : null;
    }

    /** The shop identity of the horse in slot {@code index}, or null if empty/out of range. */
    public static @Nullable UUID uuid(
            HHNBT attendantData,
            int index
    ) {
        ListTag slots = slots(attendantData);
        if (index < 0 || index >= slots.size()) {
            return null;
        }
        CompoundTag slot = slots.getCompound(index);
        return isFilled(slot) ? slot.getUUID(KEY_UUID) : null;
    }

    /**
     * Removes the horse with the given shop identity, leaving its Stall empty (surviving horses
     * do not move).
     *
     * @return the purchased horse's NBT, or null if no slot held that UUID.
     */
    public static @Nullable CompoundTag purchase(
            HHNBT attendantData,
            UUID uuid
    ) {
        ListTag slots = slots(attendantData);
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag slot = slots.getCompound(i);
            if (isFilled(slot) && slot.getUUID(KEY_UUID).equals(uuid)) {
                CompoundTag data = slot.getCompound(KEY_DATA);
                slots.set(i, new CompoundTag());
                attendantData.put(HHNBT.Key.STOCK, slots);
                return data;
            }
        }
        return null;
    }

    private static boolean isFilled(CompoundTag slot) {
        return slot.contains(KEY_UUID);
    }
}
