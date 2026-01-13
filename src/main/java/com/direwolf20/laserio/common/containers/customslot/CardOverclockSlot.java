package com.direwolf20.laserio.common.containers.customslot;

import com.direwolf20.laserio.common.items.upgrades.OverclockerCard;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class CardOverclockSlot extends SlotItemHandler {
    private boolean enabled = true;

    public CardOverclockSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        // 必须启用，且物品必须是有效的 Tier > 0 的超频卡
        if (!enabled) return false;
        return stack.getItem() instanceof OverclockerCard card && card.getEnergyTier() > 0;
    }

    @Override
    public boolean isActive() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(@Nonnull ItemStack stack) {
        return 1;
    }
}