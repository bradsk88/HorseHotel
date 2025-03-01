package ca.bradj.horsehotel;

import com.google.common.collect.Maps;
import net.minecraft.Util;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Variant;

import java.util.Map;

public class NotHorseEntityRenderer extends LivingEntityRenderer<NotHorseEntity, NotHorseModel> {

    private static ResourceLocation tex(String name) {
        String file = String.format("textures/entity/horse/%s.png", name);
        return ResourceLocation.tryBuild("minecraft", file);
    }

    // TODO: Access from Minecraft via Mixin
    @SuppressWarnings("rawtypes")
    private static final Map LOCATION_BY_VARIANT = (Map) Util.make(
            Maps.newEnumMap(Variant.class), (p_114874_) -> {
                p_114874_.put(Variant.WHITE, tex("horse_white"));
                p_114874_.put(Variant.CREAMY, tex("horse_creamy"));
                p_114874_.put(Variant.CHESTNUT, tex("horse_chestnut"));
                p_114874_.put(Variant.BROWN, tex("horse_brown"));
                p_114874_.put(Variant.BLACK, tex("horse_black"));
                p_114874_.put(Variant.GRAY, tex("horse_gray"));
                p_114874_.put(Variant.DARKBROWN, tex("horse_darkbrown"));
            }
    );

    public NotHorseEntityRenderer(EntityRendererProvider.Context p_174167_) {
        super(p_174167_, new NotHorseModel(p_174167_.bakeLayer(ModelLayers.HORSE)), 1.1F);
        // TODO: Implement markings
//        this.addLayer(new HorseMarkingLayer(this));
//        this.addLayer(new HorseArmorLayer(this, p_174167_.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(NotHorseEntity entity) {
        return (ResourceLocation) LOCATION_BY_VARIANT.get(entity.getVariant());
    }

    @Override
    protected boolean shouldShowName(NotHorseEntity p_115333_) {
        return p_115333_.hasCustomName() && !p_115333_.isInvisible();
    }
}
