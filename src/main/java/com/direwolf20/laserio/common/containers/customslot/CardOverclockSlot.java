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
        if (!enabled) return false;
        // 只要是 OverclockerCard 即可，允许堆叠
        return stack.getItem() instanceof OverclockerCard;
    }

    @Override
    public boolean isActive() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // 允许堆叠到 4 (LaserIO 逻辑上限)
    @Override
    public int getMaxStackSize() {
        return 4;
    }

    @Override
    public int getMaxStackSize(@Nonnull ItemStack stack) {
        return 4;
    }
}