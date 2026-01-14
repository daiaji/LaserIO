package com.direwolf20.laserio.client.renderer;

import com.direwolf20.laserio.client.events.ClientEvents;
import com.direwolf20.laserio.common.blockentities.LaserConnectorAdvBE;
import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.blockentities.basebe.BaseLaserBE;
import com.direwolf20.laserio.common.items.LaserWrench;
import com.direwolf20.laserio.integration.ModIntegration;
import com.direwolf20.laserio.setup.Registration;
import com.direwolf20.laserio.util.CardRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.Queue;
import java.util.Set;

public class RenderUtils {

    // [注意] BlockOverlay 使用 POSITION_COLOR 格式，不需要 UV 和 Light
    public static void render(Matrix4f matrix, VertexConsumer builder, BlockPos pos, Color color, float scale) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = 0.5f;

        float startX = (1 - scale) / 2;
        float startY = (1 - scale) / 2;
        float startZ = (1 - scale) / 2;
        float endX = 1 - startX;
        float endY = 1 - startY;
        float endZ = 1 - startZ;

        // Down
        builder.addVertex(matrix, startX, startY, startZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, startY, startZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, startY, endZ).setColor(r, g, b, a);
        builder.addVertex(matrix, startX, startY, endZ).setColor(r, g, b, a);

        // Up
        builder.addVertex(matrix, startX, endY, startZ).setColor(r, g, b, a);
        builder.addVertex(matrix, startX, endY, endZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, endY, endZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, endY, startZ).setColor(r, g, b, a);

        // North
        builder.addVertex(matrix, startX, startY, startZ).setColor(r, g, b, a);
        builder.addVertex(matrix, startX, endY, startZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, endY, startZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, startY, startZ).setColor(r, g, b, a);

        // South
        builder.addVertex(matrix, startX, startY, endZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, startY, endZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, endY, endZ).setColor(r, g, b, a);
        builder.addVertex(matrix, startX, endY, endZ).setColor(r, g, b, a);

        // West
        builder.addVertex(matrix, startX, startY, startZ).setColor(r, g, b, a);
        builder.addVertex(matrix, startX, startY, endZ).setColor(r, g, b, a);
        builder.addVertex(matrix, startX, endY, endZ).setColor(r, g, b, a);
        builder.addVertex(matrix, startX, endY, startZ).setColor(r, g, b, a);

        // East
        builder.addVertex(matrix, endX, startY, startZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, endY, startZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, endY, endZ).setColor(r, g, b, a);
        builder.addVertex(matrix, endX, startY, endZ).setColor(r, g, b, a);
    }

    public static void drawLasers(Queue<BaseLaserBE> beRenders, PoseStack matrixStackIn) {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = buffer.getBuffer(MyRenderType.CONNECTING_LASER);
        Vec3 projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        while (!beRenders.isEmpty()) {
            BaseLaserBE be = beRenders.remove();
            Level level = be.getLevel();
            if (level == null) continue;
            
            long gameTime = level.getGameTime();
            double v = gameTime * 0.04;
            BlockPos startBlock = be.getBlockPos();

            matrixStackIn.pushPose();
            Matrix4f positionMatrix = matrixStackIn.last().pose();
            matrixStackIn.translate(startBlock.getX() - projectedView.x, startBlock.getY() - projectedView.y, startBlock.getZ() - projectedView.z);

            Vector3f startLaser = new Vector3f(0.5f, 0.5f, 0.5f);
            
            for (BlockPos target : be.getRenderedConnections()) {
                BlockPos endBlock = be.getWorldPos(target);
                Color color = be.getColor();
                Player player = Minecraft.getInstance().player;
                ItemStack wrench = ClientEvents.getWrench(player);
                
                int alpha = (wrench.getItem() instanceof LaserWrench) ? Math.min(color.getAlpha() + be.getWrenchAlpha(), 255) : color.getAlpha();
                
                float diffX = endBlock.getX() + 0.5f - startBlock.getX();
                float diffY = endBlock.getY() + 0.5f - startBlock.getY();
                float diffZ = endBlock.getZ() + 0.5f - startBlock.getZ();
                Vector3f endLaser = new Vector3f(diffX, diffY, diffZ);
                
                drawLaser(builder, positionMatrix, endLaser, startLaser, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha / 255f, 0.025f, v, v + diffY * 1.5, be);
            }

            if (be instanceof LaserConnectorAdvBE laserConnectorAdvBE && laserConnectorAdvBE.getPartnerGlobalPos() != null && level.getBlockState(be.getBlockPos()).getBlock().equals(Registration.LaserConnectorAdv.get())) {
                Direction facing = level.getBlockState(be.getBlockPos()).getValue(BlockStateProperties.FACING).getOpposite();
                BlockPos endBlock = laserConnectorAdvBE.getBlockPos().relative(facing);
                Color color = be.getColor();
                Player player = Minecraft.getInstance().player;
                ItemStack wrench = ClientEvents.getWrench(player);
                int alpha = (wrench.getItem() instanceof LaserWrench) ? Math.min(color.getAlpha() + be.getWrenchAlpha(), 255) : color.getAlpha();
                
                Vector3f endLaser = calculateEndAdvConnector(startBlock, endBlock, facing);
                drawLaser(builder, positionMatrix, endLaser, startLaser, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha / 255f, 0.025f, v, v + endLaser.y() * 1.5, be);
            }
            matrixStackIn.popPose();
        }
        buffer.endBatch(MyRenderType.CONNECTING_LASER);
    }

    public static void drawConnectingLasersMainBeam(Set<LaserNodeBE> beConnectingRenders, PoseStack matrixStackIn, MultiBufferSource.BufferSource buffer, Vec3 projectedView, float alpha, float thickness) {
        VertexConsumer builder = buffer.getBuffer(MyRenderType.LASER_MAIN_BEAM);
        for (LaserNodeBE be : beConnectingRenders) {
            Level level = be.getLevel();
            if (level == null) continue;
            long gameTime = level.getGameTime();
            double v = gameTime * 0.04;
            BlockPos startBlock = be.getBlockPos();

            matrixStackIn.pushPose();
            Matrix4f positionMatrix = matrixStackIn.last().pose();
            matrixStackIn.translate(startBlock.getX() - projectedView.x, startBlock.getY() - projectedView.y, startBlock.getZ() - projectedView.z);

            for (CardRender cardRender : be.cardRenders) {
                drawLaser(builder, positionMatrix, cardRender.endLaser, cardRender.startLaser, cardRender.r, cardRender.g, cardRender.b, alpha, thickness, v, v + cardRender.diffY * 4.5, be);
            }
            matrixStackIn.popPose();
        }
        buffer.endBatch(MyRenderType.LASER_MAIN_BEAM);
    }

    public static void drawConnectingLasersMainCore(Set<LaserNodeBE> beConnectingRenders, PoseStack matrixStackIn, MultiBufferSource.BufferSource buffer, Vec3 projectedView, float alpha, float thickness) {
        VertexConsumer builder = buffer.getBuffer(MyRenderType.LASER_MAIN_CORE);
        for (LaserNodeBE be : beConnectingRenders) {
            Level level = be.getLevel();
            if (level == null) continue;
            long gameTime = level.getGameTime();
            double v = gameTime * 0.04;
            BlockPos startBlock = be.getBlockPos();

            matrixStackIn.pushPose();
            Matrix4f positionMatrix = matrixStackIn.last().pose();
            matrixStackIn.translate(startBlock.getX() - projectedView.x, startBlock.getY() - projectedView.y, startBlock.getZ() - projectedView.z);

            for (CardRender cardRender : be.cardRenders) {
                drawLaser(builder, positionMatrix, cardRender.endLaser, cardRender.startLaser, cardRender.floatcolors[0], cardRender.floatcolors[1], cardRender.floatcolors[2], 1f, 0.0125f, v, v + cardRender.diffY * 1.5, be);
            }
            matrixStackIn.popPose();
        }
        buffer.endBatch(MyRenderType.LASER_MAIN_CORE);
    }

    // [完美融合] UEL 的 Oculus 兼容性逻辑：调整绘制顺序
    public static void drawConnectingLasers(Set<LaserNodeBE> beConnectingRenders, PoseStack matrixStackIn) {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        
        float alpha = 1f;
        float thickness = 0.0175f;

        if (ModIntegration.OCULUS.isLoaded()) {
            drawConnectingLasersMainCore(beConnectingRenders, matrixStackIn, buffer, projectedView, alpha, thickness);
            drawConnectingLasersMainBeam(beConnectingRenders, matrixStackIn, buffer, projectedView, alpha, thickness);
        } else {
            drawConnectingLasersMainBeam(beConnectingRenders, matrixStackIn, buffer, projectedView, alpha, thickness);
            drawConnectingLasersMainCore(beConnectingRenders, matrixStackIn, buffer, projectedView, alpha, thickness);
        }
    }

    public static Vector3f calculateEndAdvConnector(BlockPos startBlock, BlockPos endBlock, Direction facing) {
        float diffX = endBlock.getX() - startBlock.getX();
        float diffY = endBlock.getY() - startBlock.getY();
        float diffZ = endBlock.getZ() - startBlock.getZ();

        switch (facing) {
            case UP -> { diffX += 0.5f; diffY -= 0.25f; diffZ += 0.5f; }
            case DOWN -> { diffX += 0.5f; diffY += 1.25f; diffZ += 0.5f; }
            case NORTH -> { diffX += 0.5f; diffY += 0.5f; diffZ += 1.25f; }
            case SOUTH -> { diffX += 0.5f; diffY += 0.5f; diffZ -= 0.25f; }
            case EAST -> { diffX -= 0.25f; diffY += 0.5f; diffZ += 0.5f; }
            case WEST -> { diffX += 1.25f; diffY += 0.5f; diffZ += 0.5f; }
        }
        return new Vector3f(diffX, diffY, diffZ);
    }

    public static Vector3f adjustBeamToEyes(Vector3f from, Vector3f to, BlockEntity be) {
        Player player = Minecraft.getInstance().player;
        Vector3f P = new Vector3f(
            (float) (player.getX() - be.getBlockPos().getX()), 
            (float) (player.getEyeY() - be.getBlockPos().getY()), 
            (float) (player.getZ() - be.getBlockPos().getZ())
        );

        Vector3f PS = new Vector3f(from);
        PS.sub(P);
        
        Vector3f SE = new Vector3f(to);
        SE.sub(from);

        Vector3f adjustedVec = new Vector3f(PS);
        adjustedVec.cross(SE);
        adjustedVec.normalize();
        return adjustedVec;
    }

    // [关键修复] 严格对齐 VertexFormat (POSITION_COLOR_TEX_LIGHTMAP)
    // 移除了 .setOverlay() 以防止 1.21+ 崩溃
    public static void drawLaser(VertexConsumer builder, Matrix4f positionMatrix, Vector3f from, Vector3f to, float r, float g, float b, float alpha, float thickness, double v1, double v2, BlockEntity be) {
        Vector3f adjustedVec = adjustBeamToEyes(from, to, be);
        adjustedVec.mul(thickness);

        Vector3f p1 = new Vector3f(from).add(adjustedVec);
        Vector3f p2 = new Vector3f(from).sub(adjustedVec);
        Vector3f p3 = new Vector3f(to).add(adjustedVec);
        Vector3f p4 = new Vector3f(to).sub(adjustedVec);

        builder.addVertex(positionMatrix, p1.x(), p1.y(), p1.z())
                .setColor(r, g, b, alpha)
                .setUv(1, (float) v1)
                .setLight(LightTexture.FULL_BRIGHT);

        builder.addVertex(positionMatrix, p3.x(), p3.y(), p3.z())
                .setColor(r, g, b, alpha)
                .setUv(1, (float) v2)
                .setLight(LightTexture.FULL_BRIGHT);

        builder.addVertex(positionMatrix, p4.x(), p4.y(), p4.z())
                .setColor(r, g, b, alpha)
                .setUv(0, (float) v2)
                .setLight(LightTexture.FULL_BRIGHT);

        builder.addVertex(positionMatrix, p2.x(), p2.y(), p2.z())
                .setColor(r, g, b, alpha)
                .setUv(0, (float) v1)
                .setLight(LightTexture.FULL_BRIGHT);
    }
}