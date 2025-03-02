package ca.bradj.horsehotel.gui;

import ca.bradj.horsehotel.HorseHotel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

public class GuiUtil {

    private static boolean isCoordInBox(
            double mouseX,
            double mouseY,
            int leftX,
            int topY,
            int width,
            int height
    ) {
        return mouseX >= leftX && mouseY >= topY && mouseX < leftX + width && mouseY < topY + height;
    }


    public static boolean isCoordInBox(
            Coordinate coord,
            Coordinate boxTopLeft,
            Coordinate boxBottomRight
    ) {
        return isCoordInBox(
                coord.x(),
                coord.y(),
                boxTopLeft.x(),
                boxTopLeft.y(),
                boxBottomRight.x() - boxTopLeft.x(),
                boxBottomRight.y() - boxTopLeft.y()
        );
    }

    public static void renderEntity(
            Entity entity,
            PoseStack matrixStack,
            int xPos,
            int yPos,
            float scale
    ) {
        matrixStack.pushPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        matrixStack.translate(xPos + 8, yPos + 24, 50F);
        matrixStack.scale(-scale, scale, scale);
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(180));
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(0));

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        try {
            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
            dispatcher.setRenderShadow(false);
            dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrixStack, buffer, 15728880);
            buffer.endBatch();
        } catch (Exception e) {
            HorseHotel.LOGGER.error("Error rendering entity!", e);
        }
        dispatcher.setRenderShadow(true);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        matrixStack.popPose();
    }


}
