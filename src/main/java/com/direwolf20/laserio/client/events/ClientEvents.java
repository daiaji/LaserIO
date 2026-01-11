package com.direwolf20.laserio.client.events;

import com.direwolf20.laserio.client.renderer.BlockOverlayRender;
import com.direwolf20.laserio.client.renderer.DelayedRenderer;
import com.direwolf20.laserio.common.blockentities.LaserConnectorAdvBE;
import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.blockentities.basebe.BaseLaserBE;
import com.direwolf20.laserio.common.blocks.LaserConnectorAdv;
import com.direwolf20.laserio.common.items.CardCloner;
import com.direwolf20.laserio.common.items.LaserWrench;
import com.direwolf20.laserio.integration.ModIntegration;
import com.direwolf20.laserio.setup.Config;
import com.direwolf20.laserio.setup.LaserIODataComponents;
import com.direwolf20.laserio.util.VectorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.awt.Color;

public class ClientEvents {
    
    @SubscribeEvent
    public static void renderWorldLastEvent(RenderLevelStageEvent evt) {
        // [修复] 光影兼容逻辑：Oculus 需要 AFTER_TRANSLUCENT_BLOCKS，原版通常使用 AFTER_PARTICLES
        RenderLevelStageEvent.Stage requiredStage = ModIntegration.OCULUS.isLoaded() 
                ? RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS 
                : RenderLevelStageEvent.Stage.AFTER_PARTICLES;

        if (evt.getStage() != requiredStage) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // 1. 扳手高亮渲染 (绿色)
        ItemStack wrench = getWrench(player);
        if (!wrench.isEmpty()) {
            GlobalPos selectedPos = LaserWrench.getConnectionPos(wrench, player.level());
            // 确保选中的节点在当前维度且不是零坐标
            if (selectedPos != null && player.level().dimension().equals(selectedPos.dimension()) && !selectedPos.pos().equals(BlockPos.ZERO)) {
                BlockEntity be = player.level().getBlockEntity(selectedPos.pos());
                if (be instanceof BaseLaserBE baseLaserBE) {
                    BlockOverlayRender.renderSelectedBlock(evt, selectedPos.pos(), baseLaserBE, Color.GREEN);
                }
            }
        }

        // 2. 卡片克隆器源节点预览 (青色)
        ItemStack cardCloner = findCardCloner(player);
        if (!cardCloner.isEmpty()) {
            CompoundTag nodeData = cardCloner.getOrDefault(LaserIODataComponents.CLONER_NODE_DATA, new CompoundTag());
            if (!nodeData.isEmpty()) {
                String dimKey = nodeData.getString("dimension");
                // 检查维度是否匹配
                if (player.level().dimension().location().toShortLanguageKey().equals(dimKey)) {
                    // 从 NBT 中读取坐标 (注意：服务端 CopyPayload 需要确保写入了 x,y,z)
                    int x = nodeData.getInt("x");
                    int y = nodeData.getInt("y");
                    int z = nodeData.getInt("z");
                    BlockPos targetPos = new BlockPos(x, y, z);
                    
                    BlockEntity be = player.level().getBlockEntity(targetPos);
                    if (be instanceof LaserNodeBE laserNodeBE) {
                        BlockOverlayRender.renderSelectedBlock(evt, targetPos, laserNodeBE, Color.CYAN);
                    }
                }
            }
        }

        // 3. 渲染激光连线 (延时渲染以处理透明度)
        DelayedRenderer.render(evt.getPoseStack());
        DelayedRenderer.renderConnections(evt.getPoseStack());
    }

    public static ItemStack getWrench(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof LaserWrench)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof LaserWrench)) {
                return ItemStack.EMPTY;
            }
        }
        return heldItem;
    }

    public static ItemStack findCardCloner(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof CardCloner)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof CardCloner)) {
                return ItemStack.EMPTY;
            }
        }
        return heldItem;
    }

    @SubscribeEvent
    static void renderGUIOverlay(CustomizeGuiOverlayEvent.DebugText evt) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        ItemStack wrench = getWrench(player);
        if (wrench.isEmpty()) {
            return;
        }

        // 射线检测，距离使用 Config 配置
        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, ClipContext.Fluid.NONE, Config.MAX_INTERACTION_RANGE.get());
        if (lookingAt == null || !(player.level().getBlockState(lookingAt.getBlockPos()).getBlock() instanceof LaserConnectorAdv)) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(lookingAt.getBlockPos());
        if (blockEntity instanceof LaserConnectorAdvBE laserConnectorAdvBE) {
            GuiGraphics guiGraphics = evt.getGuiGraphics();
            Font font = Minecraft.getInstance().font;
            RenderGUIOverlay.renderLocation(font, guiGraphics, laserConnectorAdvBE);
        }
    }
}