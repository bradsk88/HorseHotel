package ca.bradj.horsehotel;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemRenderProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class CowboyHat extends ArmorItem {

    public static final String ITEM_ID = "cowboy_hat";
    private static final Properties PROPS = new Properties().tab(ca.bradj.horsehotel.ModItemGroup.HORSEHOTEL_GROUP);

    public CowboyHat() {
        super(ArmorMaterials.LEATHER, EquipmentSlot.HEAD, PROPS);
    }
    // TODO: Implement rendering


    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        consumer.accept(new IItemRenderProperties() {
            @Override
            public @Nullable HumanoidModel<?> getArmorModel(
                    LivingEntity entityLiving,
                    ItemStack itemStack,
                    EquipmentSlot armorSlot,
                    HumanoidModel<?> _default
            ) {
                if (EquipmentSlot.HEAD == armorSlot) {
                    return new CowboyHatModel();
                }
                return null;
            }
        });
    }

    @Nullable
    @Override
    public String getArmorTexture(
            ItemStack stack,
            Entity entity,
            EquipmentSlot slot,
            String type
    ) {
        return "horsehotel:textures/model/hat.png";
    }
}
