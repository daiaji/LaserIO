package com.direwolf20.laserio.common.items.upgrades;

import com.direwolf20.laserio.setup.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class OverclockerCard extends Item {
    private final int energyTier;

    // 构造函数接收 tier。
    // tier = -1 代表原版逻辑超频卡 (Logistic)
    // tier > 0 代表能量超频卡 (Energy)
    public OverclockerCard(int tier) {
        super(new Item.Properties());
        this.energyTier = tier;
    }

    public int getEnergyTier() {
        return energyTier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        // 如果是能量超频卡，显示最大传输速率
        if (energyTier > 0) {
            List<? extends Integer> tiers = Config.MAX_FE_TIERS.get();
            if (tiers.size() >= energyTier) {
                tooltip.add(Component.translatable("laserio.tooltip.item.energy_overclocker.max_fe", tiers.get(energyTier - 1))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}