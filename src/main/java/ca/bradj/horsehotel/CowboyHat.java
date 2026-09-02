package ca.bradj.horsehotel;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CowboyHat extends ArmorItem {

    public static final String ITEM_ID = "cowboy_hat";
    private static final Properties PROPS = new Properties();

    public CowboyHat() {
        super(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, PROPS);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<?> original
            ) {
                if (EquipmentSlot.HEAD == equipmentSlot) {
                    return new CowboyHatModel();
                }
                return null;
            }
        });
    }
}
