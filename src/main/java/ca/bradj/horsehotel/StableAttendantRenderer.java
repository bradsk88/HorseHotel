package ca.bradj.horsehotel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StableAttendantRenderer extends MobRenderer<StableAttendant, VillagerModel<StableAttendant>> {
    private static final ResourceLocation VILLAGER_BASE_SKIN = ResourceLocation.tryBuild(
            "minecraft",
            "textures/entity/villager/villager.png"
    );
    private final CowboyHatModel hat;

    public StableAttendantRenderer(EntityRendererProvider.Context p_174437_) {
        super(p_174437_, new VillagerModel<>(p_174437_.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CustomHeadLayer<>(this, p_174437_.getModelSet(), p_174437_.getItemInHandRenderer()));
        this.hat = new CowboyHatModel();
    }

    @Override
    public void render(
            StableAttendant p_115455_,
            float p_115456_,
            float p_115457_,
            PoseStack stack,
            MultiBufferSource p_115459_,
            int p_115460_
    ) {
        super.render(p_115455_, p_115456_, p_115457_, stack, p_115459_, p_115460_);
        stack.pushPose();
        stack.mulPose(Vector3f.XP.rotationDegrees(180));
        stack.scale(1.675f, 1.675f, 1.675f);
        stack.translate(0, -1.6, 0);
        this.hat.renderToBuffer(
                stack,
                p_115459_.getBuffer(RenderType.entitySolid(ResourceLocation.tryBuild("horsehotel", "textures/model/hat.png"))),
                p_115460_,
                OverlayTexture.NO_OVERLAY,
                1.0f,
                1.0f,
                1.0f,
                1.0f
        );
        stack.popPose();
    }

    public ResourceLocation getTextureLocation(StableAttendant p_116312_) {
        return VILLAGER_BASE_SKIN;
    }

    protected void scale(
            StableAttendant p_116314_,
            PoseStack p_116315_,
            float p_116316_
    ) {
        float f = 0.9375F;
        if (p_116314_.isBaby()) {
            f *= 0.5F;
            this.shadowRadius = 0.25F;
        } else {
            this.shadowRadius = 0.5F;
        }

        p_116315_.scale(f, f, f);
    }
}