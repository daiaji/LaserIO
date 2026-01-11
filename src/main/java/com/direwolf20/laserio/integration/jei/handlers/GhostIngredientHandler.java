package com.direwolf20.laserio.integration.jei.handlers;

import com.direwolf20.laserio.client.screens.CardChemicalScreen; // [修改] 修正导入路径
import com.direwolf20.laserio.client.screens.CardItemScreen;
import com.direwolf20.laserio.client.screens.FilterCountScreen;
import com.direwolf20.laserio.common.containers.customslot.FilterBasicSlot;
import com.direwolf20.laserio.common.network.data.GhostSlotPayload;
import com.direwolf20.laserio.integration.ModIntegration;
import com.direwolf20.laserio.integration.mekanism.MekanismStatics;
import mekanism.api.IMekanismAccess;
import mekanism.api.chemical.ChemicalStack;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class GhostIngredientHandler<T extends AbstractContainerScreen<?>> implements IGhostIngredientHandler<T> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(T gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();

        for (Slot slot : gui.getMenu().slots) {
            if (!slot.isActive() || !(slot instanceof FilterBasicSlot)) {
                continue;
            }

            Rect2i bounds = new Rect2i(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, 16, 16);

            // 处理物品拖拽
            if (ingredient.getIngredient() instanceof ItemStack) {
                // 如果是化学卡界面，且物品不包含化学品，则忽略
                if (ModIntegration.MEKANISM.isLoaded() && gui instanceof CardChemicalScreen) {
                    if (!MekanismStatics.doesItemStackHoldChemicals((ItemStack) ingredient.getIngredient())) {
                        continue;
                    }
                }

                targets.add(new Target<I>() {
                    @Override
                    public Rect2i getArea() { return bounds; }

                    @Override
                    public void accept(I ingredient) {
                        ItemStack itemStack = (ItemStack) ingredient;
                        slot.set((gui instanceof CardItemScreen) ? itemStack.copy() : itemStack);
                        // FilterCountScreen 需要特殊处理以保持客户端同步
                        if (gui instanceof FilterCountScreen filterCountGui) {
                            filterCountGui.getMenu().handler.setStackInSlot(slot.index, itemStack);
                        }
                        PacketDistributor.sendToServer(new GhostSlotPayload(slot.index, itemStack, itemStack.getCount(), -1));
                    }
                });
            } 
            // 处理流体拖拽
            else if (ingredient.getIngredient() instanceof FluidStack) {
                // 如果是化学卡界面，禁止拖拽流体
                if (ModIntegration.MEKANISM.isLoaded() && gui instanceof CardChemicalScreen) {
                    continue;
                }

                targets.add(new Target<I>() {
                    @Override
                    public Rect2i getArea() { return bounds; }

                    @Override
                    public void accept(I ingredient) {
                        // 将流体转换为桶物品放入过滤器
                        ItemStack itemStack = new ItemStack(((FluidStack) ingredient).getFluid().getBucket(), 1);
                        slot.set(itemStack);
                        if (gui instanceof FilterCountScreen filterCountGui) {
                            filterCountGui.getMenu().handler.setStackInSlot(slot.index, itemStack);
                        }
                        PacketDistributor.sendToServer(new GhostSlotPayload(slot.index, itemStack, itemStack.getCount(), -1));
                    }
                });
            } 
            // 处理 Mekanism 化学品拖拽 (如果加载了模组)
            // [Fix] 移除 <?> 泛型通配符，适配 Mekanism 1.21 API
            else if (ModIntegration.MEKANISM.isLoaded() && ingredient.getIngredient() instanceof ChemicalStack chemicalStack) {
                targets.add(new Target<I>() {
                    @Override
                    public Rect2i getArea() { return bounds; }

                    @Override
                    public void accept(I ingredient) {
                        // 获取化学品的代表物品
                        ItemStack itemStack = IMekanismAccess.INSTANCE.jeiHelper().getChemicalStackHelper().getCheatItemStack(chemicalStack);
                        if (itemStack.isEmpty()) return;
                        
                        slot.set(itemStack);
                        if (gui instanceof FilterCountScreen filterCountGui) {
                            filterCountGui.getMenu().handler.setStackInSlot(slot.index, itemStack);
                        }
                        PacketDistributor.sendToServer(new GhostSlotPayload(slot.index, itemStack, itemStack.getCount(), -1));
                    }
                });
            }
        }

        return targets;
    }

    @Override
    public void onComplete() { }
}