package com.direwolf20.laserio.integration.jei.handlers;

import com.direwolf20.laserio.client.screens.CardFluidScreen; // [新增]
import com.direwolf20.laserio.client.screens.CardItemScreen;
import com.direwolf20.laserio.client.screens.FilterCountScreen;
import com.direwolf20.laserio.common.containers.customslot.FilterBasicSlot;
import com.direwolf20.laserio.common.items.filters.FilterCount; // [新增]
import com.direwolf20.laserio.common.network.data.GhostSlotPayload;
import com.direwolf20.laserio.integration.ModIntegration;
import com.direwolf20.laserio.integration.mekanism.client.screens.CardChemicalScreen; // [修复] 修正导入路径
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

            // 处理物品拖拽 (ItemStack)
            if (ingredient.getIngredient() instanceof ItemStack itemStack) {
                // 1. 化学卡：只接受化学品容器
                if (ModIntegration.MEKANISM.isLoaded() && gui instanceof CardChemicalScreen) {
                    if (!MekanismStatics.doesItemStackHoldChemicals(itemStack)) {
                        continue;
                    }
                }
                // 2. 流体卡：只接受流体容器
                else if (gui instanceof CardFluidScreen) {
                    if (!FilterCount.doesItemStackHoldFluids(itemStack)) {
                        continue;
                    }
                }
                // 3. 物品卡：逻辑上接受所有物品，不做额外限制

                targets.add(new Target<I>() {
                    @Override
                    public Rect2i getArea() { return bounds; }

                    @Override
                    public void accept(I ingredient) {
                        ItemStack stack = (ItemStack) ingredient;
                        slot.set((gui instanceof CardItemScreen) ? stack.copy() : stack);
                        if (gui instanceof FilterCountScreen filterCountGui) {
                            filterCountGui.getMenu().handler.setStackInSlot(slot.index, stack);
                        }
                        PacketDistributor.sendToServer(new GhostSlotPayload(slot.index, stack, stack.getCount(), -1));
                    }
                });
            } 
            // 处理流体拖拽 (FluidStack)
            else if (ingredient.getIngredient() instanceof FluidStack) {
                // 1. 化学卡：不接受流体
                if (ModIntegration.MEKANISM.isLoaded() && gui instanceof CardChemicalScreen) {
                    continue;
                }
                // 2. 物品卡：不接受流体 (即使可以转为桶，通常也是误操作，且用户明确指出这是错误的)
                // 除非该界面是流体卡 (CardFluidScreen extends CardItemScreen)
                if (gui instanceof CardItemScreen && !(gui instanceof CardFluidScreen)) {
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
            // 处理 Mekanism 化学品拖拽 (ChemicalStack)
            else if (ModIntegration.MEKANISM.isLoaded() && ingredient.getIngredient() instanceof ChemicalStack chemicalStack) {
                // 仅化学卡接受化学品
                if (!(gui instanceof CardChemicalScreen)) {
                    continue;
                }

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