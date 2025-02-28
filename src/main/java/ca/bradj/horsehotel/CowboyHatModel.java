package ca.bradj.horsehotel;

import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;

public class CowboyHatModel extends HumanoidModel<LivingEntity> implements HeadedModel {
    public CowboyHatModel() {
        super(buildModel(), RenderType::armorCutoutNoCull);
    }

    public static ModelPart buildModel() {
        CubeListBuilder goggles = CubeListBuilder.create();

        addBox(goggles, 12, 0, -12.0F, -1.0F, 2.0F, 8.0F, 1.0F, 12.0F);
        addBox(goggles, 12, 0, -11.0F, -1.0F, 1.0F, 6.0F, 1.0F, 1.0F);
        addBox(goggles, 12, 0, -11.0F, -1.0F, 14.0F, 6.0F, 1.0F, 1.0F);
        addBox(goggles, 12, 21, -4.0F, -2.0F, 2.0F, 1.0F, 1.0F, 12.0F);
        addBox(goggles, 20, 21, -13.0F, -2.0F, 2.0F, 1.0F, 1.0F, 12.0F);
        addBox(goggles, 22, 24, -14.0F, -3.0F, 3.0F, 1.0F, 1.0F, 10.0F);
        addBox(goggles, 24, 24, -3.0F, -3.0F, 3.0F, 1.0F, 1.0F, 10.0F);
        addBox(goggles, 24, 24, -3.0F, -4.0F, 4.0F, 1.0F, 1.0F, 8.0F);
        addBox(goggles, 24, 24, -14.0F, -4.0F, 4.0F, 1.0F, 1.0F, 8.0F);
        addBox(goggles, 12, 14, -11.0F, -3.0F, 5.0F, 6.0F, 2.0F, 6.0F);
        addBox(goggles, 12, 14, -12.0F, -2.0F, 4.0F, 8.0F, 1.0F, 8.0F);
        addBox(goggles, 0, 2, -7.0F, -5.0F, 6.0F, 1.0F, 2.0F, 4.0F);
        addBox(goggles, 22, 22, -10.0F, -5.0F, 6.0F, 1.0F, 2.0F, 4.0F);
        addBox(goggles, 18, 0, -9.0F, -4.0F, 6.0F, 2.0F, 1.0F, 4.0F);

        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("head", goggles, PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 64).bakeRoot();
    }

    private static void addBox(
            CubeListBuilder goggles,
            int i,
            int i1,
            float v,
            float v1,
            float v2,
            float v3,
            float v4,
            float v5
    ) {
        int up = 6;
        int right = 8;
        int forward = 8;
        goggles.texOffs(i, i1).addBox(v + right, v1 - up, v2 - forward, v3, v4, v5, new CubeDeformation(0.0F));
    }
}