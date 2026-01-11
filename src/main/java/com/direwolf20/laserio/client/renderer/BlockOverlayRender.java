package com.direwolf20.laserio.client.renderer;

import com.direwolf20.laserio.common.blockentities.LaserConnectorAdvBE;
import com.direwolf20.laserio.common.blockentities.LaserConnectorBE;
import com.direwolf20.laserio.common.blockentities.basebe.BaseLaserBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.awt.*;

public class BlockOverlayRender {
    
    public static void renderSelectedBlock(RenderLevelStageEvent event, BlockPos pos, BaseLaserBE be, Color color) {
        final Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        // 连接器稍微小一点，节点稍微大一点
        float scale = (be instanceof LaserConnectorBE || be instanceof LaserConnectorAdvBE) ? 0.375f : 0.625f;

        Vec3 view = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack matrix = event.getPoseStack();
        
        matrix.pushPose();
        
        // [修复] 使用相对坐标平移。将原点移动到方块的左下角 (相对于相机)
        // 这种写法比先 translate(-view) 再 translate(pos) 更稳定且符合现代渲染管线
        double renderX = pos.getX() - view.x();
        double renderY = pos.getY() - view.y();
        double renderZ = pos.getZ() - view.z();
        matrix.translate(renderX, renderY, renderZ);

        VertexConsumer builder = buffer.getBuffer(MyRenderType.BlockOverlay);

        matrix.pushPose();
        // [修复] 坐标已经平移过了，这里只做微调，不需要再 translate(pos)
        
        // 微调位置和大小，防止与方块纹理重叠 (Z-Fighting)
        matrix.translate(-0.005f, -0.005f, -0.005f);
        matrix.scale(1.01f, 1.01f, 1.01f);
        
        // [修复] 移除导致位置严重偏移的旋转代码
        // matrix.mulPose(Axis.YP.rotationDegrees(-90.0F)); 

        Matrix4f positionMatrix = matrix.last().pose();
        // 调用 RenderUtils 绘制线框 (它会在当前原点绘制 0~1 的框)
        RenderUtils.render(positionMatrix, builder, pos, color, scale);
        
        matrix.popPose(); // Pop 微调变换
        matrix.popPose(); // Pop 世界变换
        
        buffer.endBatch(MyRenderType.BlockOverlay);
    }
}