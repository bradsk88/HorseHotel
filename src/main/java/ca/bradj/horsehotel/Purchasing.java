package ca.bradj.horsehotel;

import ca.bradj.horsehotel.compat.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Buying a horse out of a Stable's Stock. The counterpart to {@link Registration} (which stores
 * and summons a player's own horses); Purchasing takes a horse the Stable owns into the world.
 * <p>
 * Horses are free in this slice. When currency lands, the cost check belongs here, before
 * {@link Stock#purchase} removes the horse.
 */
public class Purchasing {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Buys the Stock horse with the given shop identity from the Stable nearest {@code stablePos},
     * spawns it as a real horse, and mounts the player. If the player is already riding a
     * registerable horse it is stored into their Registry first (a mount-swap); if they are riding
     * something else the purchase is left unmounted beside them. A failed lookup — no Attendant, no
     * such horse, or the player out of range — shows the buyer an action-bar message rather than
     * failing silently.
     */
    public static void purchase(
            ServerPlayer sp,
            UUID stockUuid,
            BlockPos stablePos
    ) {
        ServerLevel sl = sp.getLevel();
        double radius = Config.horseEvalRadius;

        StableAttendant att = StableAttendant.findNearest(sl, stablePos, radius);
        if (att == null || sp.distanceToSqr(Vec3.atCenterOf(stablePos)) > radius * radius) {
            fail(sp);
            return;
        }

        CompoundTag data = Stock.purchase(HHNBT.getPersistentData(att), stockUuid);
        if (data == null) {
            fail(sp);
            return;
        }

        spawnAndMount(sp, sl, data);
    }

    private static void spawnAndMount(
            ServerPlayer sp,
            ServerLevel sl,
            CompoundTag data
    ) {
        Horse horse = EntityType.HORSE.create(sl);
        if (horse == null) {
            HorseHotel.logBug("Could not create a purchased horse; the buyer paid nothing, so nothing is lost.");
            return;
        }
        horse.deserializeNBT(Registration.updateDataToFullHealth(sl, data));
        horse.setUUID(UUID.randomUUID());
        // Stock horses are rolled wild by finalizeSpawn, so a fresh purchase would buck the buyer
        // off. Buying one tames it to the purchaser and saddles it, ready to ride.
        horse.setTamed(true);
        horse.setOwnerUUID(sp.getUUID());
        horse.equipSaddle(null);
        horse.setPos(sp.getX(), sp.getY(), sp.getZ());
        sl.addFreshEntity(horse);

        // The bought horse joins the buyer's Registry immediately — a safety-net snapshot, keyed by
        // this horse's fresh UUID — so it lives in "Your Horses" from the moment of purchase while
        // the buyer rides the world copy. The world horse is not removed; both coexist, exactly as a
        // BOTW-summoned horse does.
        Registration.storeHorseOnPlayer(sp, horse);

        // Mount-swap: riding a registerable horse stores it into the Registry and remounts onto the
        // purchase; on foot, just mount; riding something non-registerable, leave them on it and the
        // purchase stands beside them (it spawned at the player's feet).
        Entity vehicle = sp.getVehicle();
        boolean mount;
        if (vehicle instanceof Horse oldHorse) {
            Registration.storeHorseOnPlayer(sp, oldHorse);
            oldHorse.remove(Entity.RemovalReason.DISCARDED);
            mount = true;
        } else {
            mount = vehicle == null;
        }
        if (mount) {
            sp.startRiding(horse);
        }
        LOGGER.debug("Purchased horse spawned for {}", sp.getGameProfile().getName());
    }

    private static void fail(ServerPlayer sp) {
        sp.displayClientMessage(Compat.translatable("horsehotel.menus.purchase.already_sold"), true);
    }
}
