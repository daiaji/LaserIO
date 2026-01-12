package com.direwolf20.laserio.common.network.handler;

import com.direwolf20.laserio.common.blocks.LaserNode;
import com.direwolf20.laserio.common.containers.CardEnergyContainer;
import com.direwolf20.laserio.common.containers.CardEnergyContainer;
import com.direwolf20.laserio.common.containers.CardHolderContainer;
import com.direwolf20.laserio.common.containers.CardItemContainer;
import com.direwolf20.laserio.common.containers.LaserNodeContainer;
import com.direwolf20.laserio.common.containers.customhandler.CardItemHandler;
import com.direwolf20.laserio.common.containers.customslot.CardHolderSlot;
import com.direwolf20.laserio.common.items.CardCloner;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardEnergy;
import com.direwolf20.laserio.common.network.data.CopyPasteCardPayload;
import com.direwolf20.laserio.util.CardHolderItemStackHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacketCopyPasteCard {
    public static final PacketCopyPasteCard INSTANCE = new PacketCopyPasteCard();

    public static PacketCopyPasteCard get() {
        return INSTANCE;
    }

    public void handle(final CopyPasteCardPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            AbstractContainerMenu container = player.containerMenu;
            if (container == null) return;

            if (payload.slot() < 0 || payload.slot() >= container.slots.size()) return;

            ItemStack slotStack = container.getSlot(payload.slot()).getItem();
            ItemStack clonerStack = container.getCarried();

            if (clonerStack.isEmpty() || !(clonerStack.getItem() instanceof CardCloner)) return;

            if (payload.copy()) { // ============================ 复制模式 ============================
                Item slotItem = slotStack.getItem();
                if (slotItem instanceof BaseCard) {
                    DataComponentPatch dataComponentPatch = slotStack.getComponentsPatch();
                    CardCloner.saveSettings(clonerStack, dataComponentPatch);
                    CardCloner.setItemType(clonerStack, slotStack.getItem().toString());
                    
                    playSound((ServerPlayer) player, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT);
                    player.displayClientMessage(Component.translatable("message.laserio.card_cloner.card_copied"), true);
                }
            } else { // ============================ 粘贴模式 ============================
                String savedType = CardCloner.getItemType(clonerStack);
                if (savedType.isEmpty() || !slotStack.getItem().toString().equals(savedType)) {
                    player.displayClientMessage(Component.literal("Paste Failed: Card type mismatch").withStyle(ChatFormatting.RED), true);
                    playSound((ServerPlayer) player, SoundEvents.WAXED_SIGN_INTERACT_FAIL);
                    return;
                }

                int stackSize = slotStack.getCount();
                
                // 1. 需求计算
                ItemStack baseFilterNeeded = CardCloner.getFilter(clonerStack);
                ItemStack baseOverclockersNeeded = CardCloner.getOverclocker(clonerStack);

                // 2. 现有检查
                CardItemHandler existingHandler = getCardHandler(slotStack);
                ItemStack baseExistingFilter = ItemStack.EMPTY;
                ItemStack baseExistingOverclockers = ItemStack.EMPTY;

                int filterSlotIndex = 0;
                int overclockSlotIndex = 1;
                boolean isEnergyCard = (slotStack.getItem() instanceof CardEnergy);
                if (isEnergyCard) {
                    overclockSlotIndex = 0;
                    filterSlotIndex = -1; 
                }

                if (!isEnergyCard && existingHandler.getSlots() > 0) {
                    baseExistingFilter = existingHandler.getStackInSlot(filterSlotIndex);
                }
                if (existingHandler.getSlots() > overclockSlotIndex) {
                    baseExistingOverclockers = existingHandler.getStackInSlot(overclockSlotIndex);
                }

                // 3. 资源池构建 (虚拟卡包 + 物理背包)
                boolean hasGuiCardHolderSlots = false;
                List<Integer> physicalSourceSlots = new ArrayList<>();
                for (Slot s : container.slots) {
                    if (s.index == payload.slot()) continue; 
                    
                    if (s instanceof CardHolderSlot) {
                        hasGuiCardHolderSlots = true;
                        if (s.hasItem()) physicalSourceSlots.add(s.index);
                    } else if (s.container == player.getInventory() && s.hasItem()) {
                        physicalSourceSlots.add(s.index);
                    }
                }

                // 如果 GUI 里没显示卡包槽位（比如按E打开的背包），则尝试寻找玩家身上的卡包（Curios/Inventory）
                // 并将其封装为虚拟 ItemHandler
                IItemHandlerModifiable virtualHandler = null;
                if (!hasGuiCardHolderSlots) {
                    ItemStack holderStack = LaserNode.findFirstCardHolder(player);
                    if (!holderStack.isEmpty()) {
                        virtualHandler = new CardHolderItemStackHandler(CardHolderContainer.SLOTS, holderStack);
                    }
                }

                // 4. 计算与模拟
                boolean filterSatisfied = false;
                boolean filterNeedsReturn = false;
                boolean overclockSatisfied = false;
                boolean overclockNeedsReturn = false;

                int totalFilterNeeded = baseFilterNeeded.getCount() * stackSize;
                int totalExistingFilter = baseExistingFilter.getCount() * stackSize;

                // --- 过滤器 ---
                if (filterSlotIndex == -1) {
                    filterSatisfied = true;
                } else if (baseExistingFilter.is(baseFilterNeeded.getItem())) {
                    filterSatisfied = true;
                } else {
                    if (!baseExistingFilter.isEmpty()) {
                        filterNeedsReturn = !returnResources(container, physicalSourceSlots, virtualHandler, baseExistingFilter.getItem(), totalExistingFilter, true);
                    }
                    if (!filterNeedsReturn) {
                        if (totalFilterNeeded > 0) {
                            filterSatisfied = getResources(container, physicalSourceSlots, virtualHandler, baseFilterNeeded.getItem(), totalFilterNeeded, true);
                        } else {
                            filterSatisfied = true;
                        }
                    }
                }

                // --- 超频 ---
                int totalOverclockNeeded = baseOverclockersNeeded.getCount() * stackSize;
                int totalExistingOverclock = baseExistingOverclockers.getCount() * stackSize;

                if (baseExistingOverclockers.getCount() == baseOverclockersNeeded.getCount()) {
                    overclockSatisfied = true;
                } else {
                    if (baseExistingOverclockers.getCount() > baseOverclockersNeeded.getCount()) {
                        int totalReturn = (baseExistingOverclockers.getCount() - baseOverclockersNeeded.getCount()) * stackSize;
                        overclockNeedsReturn = !returnResources(container, physicalSourceSlots, virtualHandler, baseExistingOverclockers.getItem(), totalReturn, true);
                        overclockSatisfied = true;
                    } else {
                        int totalNeed = (baseOverclockersNeeded.getCount() - baseExistingOverclockers.getCount()) * stackSize;
                        overclockSatisfied = getResources(container, physicalSourceSlots, virtualHandler, baseOverclockersNeeded.getItem(), totalNeed, true);
                    }
                }

                // 5. 执行
                if (filterSatisfied && !filterNeedsReturn && overclockSatisfied && !overclockNeedsReturn) {
                    // 过滤器交换
                    if (filterSlotIndex != -1 && !baseExistingFilter.is(baseFilterNeeded.getItem())) {
                        if (!baseExistingFilter.isEmpty()) {
                            if (!returnResources(container, physicalSourceSlots, virtualHandler, baseExistingFilter.getItem(), totalExistingFilter, false))
                                dropItem(player, new ItemStack(baseExistingFilter.getItem(), totalExistingFilter));
                        }
                        if (totalFilterNeeded > 0) {
                            getResources(container, physicalSourceSlots, virtualHandler, baseFilterNeeded.getItem(), totalFilterNeeded, false);
                        }
                    }

                    // 超频交换
                    if (baseExistingOverclockers.getCount() != baseOverclockersNeeded.getCount()) {
                        if (baseExistingOverclockers.getCount() > baseOverclockersNeeded.getCount()) {
                            int totalReturn = (baseExistingOverclockers.getCount() - baseOverclockersNeeded.getCount()) * stackSize;
                            if (!returnResources(container, physicalSourceSlots, virtualHandler, baseExistingOverclockers.getItem(), totalReturn, false))
                                dropItem(player, new ItemStack(baseExistingOverclockers.getItem(), totalReturn));
                        } else {
                            int totalNeed = (baseOverclockersNeeded.getCount() - baseExistingOverclockers.getCount()) * stackSize;
                            getResources(container, physicalSourceSlots, virtualHandler, baseOverclockersNeeded.getItem(), totalNeed, false);
                        }
                    }

                    // 应用新数据
                    ItemStack tempStack = new ItemStack(slotStack.getItem(), stackSize);
                    DataComponentPatch storedPatch = CardCloner.getSettings(clonerStack);
                    if (storedPatch != null) {
                        tempStack.applyComponents(storedPatch);
                    }

                    CardItemHandler newHandler = getCardHandler(tempStack);
                    if (filterSlotIndex != -1 && !baseFilterNeeded.isEmpty()) {
                        ItemStack filterToSet = baseFilterNeeded.copy();
                        filterToSet.setCount(1);
                        newHandler.setStackInSlot(filterSlotIndex, filterToSet);
                    }
                    if (!baseOverclockersNeeded.isEmpty()) {
                        ItemStack ocToSet = baseOverclockersNeeded.copy();
                        newHandler.setStackInSlot(overclockSlotIndex, ocToSet);
                    }

                    container.getSlot(payload.slot()).set(tempStack);

                    playSound((ServerPlayer) player, SoundEvents.ENCHANTMENT_TABLE_USE);
                    
                    if (container instanceof LaserNodeContainer laserNodeContainer) {
                        laserNodeContainer.tile.updateThisNode();
                    }
                } else {
                    playSound((ServerPlayer) player, SoundEvents.WAXED_SIGN_INTERACT_FAIL);
                    MutableComponent msg = Component.literal("Paste Failed: ");
                    if (!filterSatisfied) msg.append("Missing Filters. ");
                    if (filterNeedsReturn) msg.append("Full (Filter). ");
                    if (!overclockSatisfied) msg.append("Missing OCs. ");
                    if (overclockNeedsReturn) msg.append("Full (OC). ");
                    player.displayClientMessage(msg.withStyle(ChatFormatting.RED), true);
                }
            }
        });
    }

    // ========================================================================================
    // 新的资源管理逻辑：支持 虚拟Handler(卡包) + 物理Slots(背包)
    // ========================================================================================

    private static boolean returnResources(AbstractContainerMenu container, List<Integer> physicalSlots, IItemHandlerModifiable virtualHandler, Item item, int amount, boolean simulate) {
        if (amount <= 0) return true;
        int remaining = amount;
        Map<Integer, Integer> physicalPlan = new HashMap<>();
        Map<Integer, Integer> virtualPlan = new HashMap<>();

        // 1. 优先放入虚拟卡包 (Virtual Handler)
        if (virtualHandler != null) {
            for (int i = 0; i < virtualHandler.getSlots(); i++) {
                ItemStack inSlot = virtualHandler.getStackInSlot(i);
                if (inSlot.isEmpty() || (inSlot.is(item) && inSlot.getCount() < inSlot.getMaxStackSize())) {
                    int space = inSlot.getMaxStackSize() - inSlot.getCount();
                    int toAdd = Math.min(remaining, space);
                    virtualPlan.put(i, toAdd);
                    remaining -= toAdd;
                    if (remaining == 0) break;
                }
            }
        }

        // 2. 如果还有剩余，放入物理背包 (Physical Slots)
        if (remaining > 0) {
            for (int slotIndex : physicalSlots) {
                ItemStack inSlot = container.getSlot(slotIndex).getItem();
                if (inSlot.isEmpty() || (inSlot.is(item) && inSlot.getCount() < inSlot.getMaxStackSize())) {
                    int space = inSlot.getMaxStackSize() - inSlot.getCount();
                    int toAdd = Math.min(remaining, space);
                    physicalPlan.put(slotIndex, toAdd);
                    remaining -= toAdd;
                    if (remaining == 0) break;
                }
            }
        }

        if (remaining > 0) return false;
        if (simulate) return true;

        // 执行写入
        if (virtualHandler != null) {
            for (Map.Entry<Integer, Integer> entry : virtualPlan.entrySet()) {
                ItemStack inSlot = virtualHandler.getStackInSlot(entry.getKey());
                if (inSlot.isEmpty()) {
                    virtualHandler.setStackInSlot(entry.getKey(), new ItemStack(item, entry.getValue()));
                } else {
                    inSlot.grow(entry.getValue());
                    virtualHandler.setStackInSlot(entry.getKey(), inSlot); // 触发更新
                }
            }
        }

        for (Map.Entry<Integer, Integer> entry : physicalPlan.entrySet()) {
            ItemStack inSlot = container.getSlot(entry.getKey()).getItem();
            if (inSlot.isEmpty()) {
                container.getSlot(entry.getKey()).set(new ItemStack(item, entry.getValue()));
            } else {
                inSlot.grow(entry.getValue());
                container.getSlot(entry.getKey()).set(inSlot);
            }
        }
        return true;
    }

    private static boolean getResources(AbstractContainerMenu container, List<Integer> physicalSlots, IItemHandlerModifiable virtualHandler, Item item, int amount, boolean simulate) {
        if (amount <= 0) return true;
        int remaining = amount;
        Map<Integer, Integer> physicalPlan = new HashMap<>();
        Map<Integer, Integer> virtualPlan = new HashMap<>();

        // 1. 优先从虚拟卡包提取
        if (virtualHandler != null) {
            for (int i = 0; i < virtualHandler.getSlots(); i++) {
                ItemStack inSlot = virtualHandler.getStackInSlot(i);
                if (inSlot.is(item)) {
                    int available = inSlot.getCount();
                    int toTake = Math.min(remaining, available);
                    virtualPlan.put(i, toTake);
                    remaining -= toTake;
                    if (remaining == 0) break;
                }
            }
        }

        // 2. 从物理背包提取
        if (remaining > 0) {
            for (int slotIndex : physicalSlots) {
                ItemStack inSlot = container.getSlot(slotIndex).getItem();
                if (inSlot.is(item)) {
                    int available = inSlot.getCount();
                    int toTake = Math.min(remaining, available);
                    physicalPlan.put(slotIndex, toTake);
                    remaining -= toTake;
                    if (remaining == 0) break;
                }
            }
        }

        if (remaining > 0) return false;
        if (simulate) return true;

        // 执行提取
        if (virtualHandler != null) {
            for (Map.Entry<Integer, Integer> entry : virtualPlan.entrySet()) {
                ItemStack inSlot = virtualHandler.getStackInSlot(entry.getKey());
                inSlot.shrink(entry.getValue());
                virtualHandler.setStackInSlot(entry.getKey(), inSlot); // 触发更新
            }
        }

        for (Map.Entry<Integer, Integer> entry : physicalPlan.entrySet()) {
            ItemStack inSlot = container.getSlot(entry.getKey()).getItem();
            inSlot.shrink(entry.getValue());
            container.getSlot(entry.getKey()).set(inSlot);
        }
        return true;
    }

    private CardItemHandler getCardHandler(ItemStack stack) {
        if (stack.getItem() instanceof CardEnergy) {
            return new CardItemHandler(CardEnergyContainer.SLOTS, stack);
        } else {
            return BaseCard.getInventory(stack);
        }
    }

    public static void playSound(ServerPlayer player, SoundEvent soundEvent) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        ClientboundSoundPacket packet = new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent), 
                SoundSource.MASTER, x, y, z, 1, 1, 1
        );
        player.connection.send(packet);
    }

    private static void dropItem(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        while (stack.getCount() > 0) {
            int dropCount = Math.min(stack.getCount(), stack.getMaxStackSize());
            ItemStack drop = stack.split(dropCount);
            ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), drop);
            player.level().addFreshEntity(itemEntity);
        }
    }
}