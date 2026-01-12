package com.direwolf20.laserio.client.events;

import com.direwolf20.laserio.client.renderer.BlockOverlayRender;
import com.direwolf20.laserio.client.renderer.DelayedRenderer;
import com.direwolf20.laserio.common.blockentities.LaserConnectorAdvBE;
import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.blockentities.basebe.BaseLaserBE;
import com.direwolf20.laserio.common.blocks.LaserConnectorAdv;
import com.direwolf20.laserio.common.items.CardCloner;
import com.direwolf20.laserio.common.items.LaserWrench;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.network.data.CopyPasteCardPayload;
import com.direwolf20.laserio.setup.Config;
import com.direwolf20.laserio.setup.LaserIODataComponents;
import com.direwolf20.laserio.util.VectorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext; // 确保导入
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.awt.Color;

public class ClientEvents {

    @SubscribeEvent
    public static void onScreenMouseClickPre(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        boolean isCopy = (event.getButton() == 0);
        boolean isPaste = (event.getButton() == 1);
        if (!isCopy && !isPaste) return;

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null) return;

        ItemStack carriedItem = mc.player.containerMenu.getCarried();

        if (!carriedItem.isEmpty() && carriedItem.getItem() instanceof CardCloner) {
            // 基础检查：必须是 BaseCard
            if (!hoveredSlot.hasItem() || !(hoveredSlot.getItem().getItem() instanceof BaseCard)) {
                return;
            }

            // 发送数据包
            PacketDistributor.sendToServer(new CopyPasteCardPayload(hoveredSlot.index, isCopy));
            // 取消原版事件
            event.setCanceled(true); 
        }
    }

    @SubscribeEvent
    public static void renderWorldLastEvent(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack wrench = getWrench(player);
        if (!wrench.isEmpty()) {
            GlobalPos selectedPos = LaserWrench.getConnectionPos(wrench, player.level());
            if (selectedPos != null && player.level().dimension().equals(selectedPos.dimension()) && !selectedPos.pos().equals(BlockPos.ZERO)) {
                BlockEntity be = player.level().getBlockEntity(selectedPos.pos());
                if (be instanceof BaseLaserBE baseLaserBE) {
                    BlockOverlayRender.renderSelectedBlock(evt, selectedPos.pos(), baseLaserBE, Color.GREEN);
                }
            }
        }

        ItemStack cardCloner = findCardCloner(player);
        if (!cardCloner.isEmpty()) {
            CompoundTag nodeData = cardCloner.getOrDefault(LaserIODataComponents.CLONER_NODE_DATA, new CompoundTag());
            if (!nodeData.isEmpty()) {
                String dimKey = nodeData.getString("dimension");
                if (player.level().dimension().location().toShortLanguageKey().equals(dimKey)) {
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

        DelayedRenderer.render(evt.getPoseStack());
        DelayedRenderer.renderConnections(evt.getPoseStack());
    }

    public static ItemStack getWrench(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof LaserWrench)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof LaserWrench)) return ItemStack.EMPTY;
        }
        return heldItem;
    }

    public static ItemStack findCardCloner(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof CardCloner)) {
            heldItem = player.getOffhandItem();
            if (!(heldItem.getItem() instanceof CardCloner)) return ItemStack.EMPTY;
        }
        return heldItem;
    }

    @SubscribeEvent
    static void renderGUIOverlay(CustomizeGuiOverlayEvent.DebugText evt) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        ItemStack wrench = getWrench(player);
        if (wrench.isEmpty()) return;

        // [修复] 使用 ClipContext 的全名，防止导入丢失
        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, net.minecraft.world.level.ClipContext.Fluid.NONE, Config.MAX_INTERACTION_RANGE.get());
        if (lookingAt == null || !(player.level().getBlockState(lookingAt.getBlockPos()).getBlock() instanceof LaserConnectorAdv)) return;

        BlockEntity blockEntity = player.level().getBlockEntity(lookingAt.getBlockPos());
        if (blockEntity instanceof LaserConnectorAdvBE laserConnectorAdvBE) {
            GuiGraphics guiGraphics = evt.getGuiGraphics();
            Font font = Minecraft.getInstance().font;
            RenderGUIOverlay.renderLocation(font, guiGraphics, laserConnectorAdvBE);
        }
    }
}