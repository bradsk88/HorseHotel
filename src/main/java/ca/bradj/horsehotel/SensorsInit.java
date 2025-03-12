package ca.bradj.horsehotel;

import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SensorsInit {

    public static final DeferredRegister<SensorType<?>> SENSORS = DeferredRegister.create(
            ForgeRegistries.SENSOR_TYPES,
            HorseHotel.MODID
    );

    public static final RegistryObject<SensorType<JobSiteFromHorseSensor>> JOBSITE_FROM_HORSES =
            SENSORS.register(
                    JobSiteFromHorseSensor.ID,
                    () -> new SensorType<>(JobSiteFromHorseSensor::new)
            );
}
