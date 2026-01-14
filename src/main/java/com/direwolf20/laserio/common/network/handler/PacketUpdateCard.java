package com.direwolf20.laserio.common.network.handler;

import com.direwolf20.laserio.common.containers.CardEnergyContainer;
import com.direwolf20.laserio.common.containers.CardItemContainer;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardEnergy;
import com.direwolf20.laserio.common.items.cards.CardFluid;
import com.direwolf20.laserio.common.items.cards.CardItem;
import com.direwolf20.laserio.common.items.upgrades.OverclockerCard;
import com.direwolf20.laserio.common.network.data.UpdateCardPayload;
import com.direwolf20.laserio.integration.mekanism.CardChemical;
import com.direwolf20.laserio.setup.Config;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class PacketUpdateCard {
    public static final PacketUpdateCard INSTANCE = new PacketUpdateCard();

    public static PacketUpdateCard get() {
        return INSTANCE;
    }

    public void handle(final UpdateCardPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player sender = context.player();

            AbstractContainerMenu container = sender.containerMenu;
            if (container == null)
                return;

            if (container instanceof CardItemContainer || container instanceof CardEnergyContainer) {
                ItemStack stack;
                if (container instanceof CardEnergyContainer)
                    stack = ((CardEnergyContainer) container).cardItem;
                else
                    stack = ((CardItemContainer) container).cardItem;
                BaseCard.setTransferMode(stack, payload.mode());
                BaseCard.setChannel(stack, payload.channel());
                BaseCard.setMaxBackoff(stack, payload.maxBackoff());
                int extractAmt = payload.extractAmt();
                int overClockerCount = 0;

                // [修改] 统一获取超频卡数量
                if (container instanceof CardEnergyContainer energyContainer) {
                    if (CardEnergyContainer.SLOTS > 0) {
                        ItemStack ocStack = energyContainer.getSlot(0).getItem();
                        if (ocStack.getItem() instanceof OverclockerCard)
                            overClockerCount = ocStack.getCount();
                    }
                } else {
                    // 对于其他卡片，超频卡通常在 Slot 1
                    overClockerCount = container.getSlot(1).getItem().getCount();
                }

                if (stack.getItem() instanceof CardItem) {
                    // 物品卡逻辑 (保持线性)
                    int maxAmt = Math.max(overClockerCount * 16, 8);
                    if (extractAmt > maxAmt) {
                        extractAmt = (byte) maxAmt;
                    }
                    CardItem.setItemExtractAmt(stack, (byte) extractAmt);

                    // 速度校验 (使用配置列表)
                    int minTicks = Config.MIN_TICKS_ITEM.get().get(Math.min(overClockerCount, Config.MIN_TICKS_ITEM.get().size() - 1));
                    short ticks = payload.ticks();
                    if (ticks < minTicks) ticks = (short) minTicks;
                    BaseCard.setExtractSpeed(stack, ticks);

                } else if (stack.getItem() instanceof CardFluid) {
                    // [修复] 流体卡逻辑 (支持层级模式)
                    int maxAmt;
                    if (Config.USE_FLUID_TIERS_MODE.get()) {
                        List<? extends Integer> tiers = Config.MAX_FLUID_TIERS.get();
                        if (overClockerCount == 0) maxAmt = Config.BASE_MILLI_BUCKETS_FLUID.get();
                        else if (overClockerCount <= tiers.size()) maxAmt = tiers.get(overClockerCount - 1);
                        else maxAmt = tiers.get(tiers.size() - 1);
                    } else {
                        maxAmt = Math.max(overClockerCount * Config.MULTIPLIER_MILLI_BUCKETS_FLUID.get(), Config.BASE_MILLI_BUCKETS_FLUID.get());
                    }

                    if (extractAmt > maxAmt) {
                        extractAmt = maxAmt;
                    }
                    CardFluid.setFluidExtractAmt(stack, extractAmt);

                    // 速度校验 (使用配置列表)
                    int minTicks = Config.MIN_TICKS_FLUID.get().get(Math.min(overClockerCount, Config.MIN_TICKS_FLUID.get().size() - 1));
                    short ticks = payload.ticks();
                    if (ticks < minTicks) ticks = (short) minTicks;
                    BaseCard.setExtractSpeed(stack, ticks);

                } else if (stack.getItem() instanceof CardEnergy) {
                    // [修复] 能量卡逻辑 (支持层级模式)
                    int maxAmt = Config.MAX_FE_NO_TIERS.get();
                    List<? extends Integer> tiers = Config.MAX_FE_TIERS.get();
                    
                    if (overClockerCount > 0) {
                        if (overClockerCount <= tiers.size()) {
                            maxAmt = tiers.get(overClockerCount - 1);
                        } else {
                            maxAmt = tiers.get(tiers.size() - 1);
                        }
                    }
                    
                    // 还要受限于全局硬上限
                    int globalMax = Config.MAX_FE_TICK.get();
                    if (maxAmt > globalMax) maxAmt = globalMax;

                    if (extractAmt > maxAmt) {
                        extractAmt = maxAmt;
                    }
                    CardEnergy.setEnergyExtractAmt(stack, extractAmt);

                    short ticks = payload.ticks();
                    int minTicks = Config.MIN_TICKS_ENERGY.get();
                    if (ticks < minTicks) ticks = (short) minTicks;
                    
                    CardEnergy.setExtractSpeed(stack, ticks);
                    CardEnergy.setExtractLimitPercent(stack, payload.extractLimit());
                    CardEnergy.setInsertLimitPercent(stack, payload.insertLimit());

                } else if (stack.getItem() instanceof CardChemical) {
                    // 化学卡逻辑 (保持线性)
                    int maxAmt = Math.max(overClockerCount * Config.MULTIPLIER_MILLI_BUCKETS_CHEMICAL.get(), Config.BASE_MILLI_BUCKETS_CHEMICAL.get());
                    if (extractAmt > maxAmt) {
                        extractAmt = maxAmt;
                    }
                    CardChemical.setChemicalExtractAmt(stack, extractAmt);

                    // 速度校验 (使用配置列表)
                    int minTicks = Config.MIN_TICKS_CHEMICAL.get().get(Math.min(overClockerCount, Config.MIN_TICKS_CHEMICAL.get().size() - 1));
                    short ticks = payload.ticks();
                    if (ticks < minTicks) ticks = (short) minTicks;
                    BaseCard.setExtractSpeed(stack, ticks);
                }

                BaseCard.setPriority(stack, payload.priority());
                BaseCard.setSneaky(stack, payload.sneaky());
                BaseCard.setExact(stack, payload.exact());
                BaseCard.setRoundRobin(stack, payload.roundRobin());
                BaseCard.setRegulate(stack, payload.regulate());
                BaseCard.setRedstoneMode(stack, payload.redstoneMode());
                BaseCard.setRedstoneChannel(stack, payload.redstoneChannel());
                BaseCard.setAnd(stack, payload.andMode());
            }
        });
    }
}