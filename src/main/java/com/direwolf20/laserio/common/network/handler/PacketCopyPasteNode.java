package com.direwolf20.laserio.common.network.handler;

import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.blocks.LaserNode;
import com.direwolf20.laserio.common.containers.CardEnergyContainer;
import com.direwolf20.laserio.common.containers.customhandler.CardItemHandler;
import com.direwolf20.laserio.common.items.CardCloner;
import com.direwolf20.laserio.common.items.CardHolder;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardEnergy;
import com.direwolf20.laserio.common.network.data.CopyPasteNodePayload;
import com.direwolf20.laserio.setup.LaserIODataComponents;
import com.direwolf20.laserio.util.ItemHandlerUtil.InventoryCardCounts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class PacketCopyPasteNode {
    public static final PacketCopyPasteNode INSTANCE = new PacketCopyPasteNode();

    public static PacketCopyPasteNode get() {
        return INSTANCE;
    }

    public void handle(final CopyPasteNodePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            
            ItemStack clonerStack = ItemStack.EMPTY;
            if (player.getMainHandItem().getItem() instanceof CardCloner) {
                clonerStack = player.getMainHandItem();
            } else if (player.getOffhandItem().getItem() instanceof CardCloner) {
                clonerStack = player.getOffhandItem();
            }

            if (clonerStack.isEmpty()) return;

            BlockPos pos = payload.pos();
            BlockEntity be = player.level().getBlockEntity(pos);
            if (!(be instanceof LaserNodeBE laserNode)) return;

            if (payload.mode() == 1) { // COPY
                CompoundTag tag = new CompoundTag();
                laserNode.saveAdditional(tag, player.registryAccess());
                
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());
                tag.putString("dimension", player.level().dimension().location().toShortLanguageKey());
                
                clonerStack.set(LaserIODataComponents.CLONER_NODE_DATA, tag);
                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.displayClientMessage(Component.translatable("message.laserio.card_cloner.node_copied"), true);
                
            } else if (payload.mode() == 2) { // PASTE
                CompoundTag nodeData = clonerStack.getOrDefault(LaserIODataComponents.CLONER_NODE_DATA, new CompoundTag());
                if (nodeData.isEmpty()) {
                    player.displayClientMessage(Component.translatable("message.laserio.card_cloner.copy_node_first"), true);
                    return;
                }

                boolean pasteNetworkOnly = clonerStack.getOrDefault(LaserIODataComponents.CLONER_PASTE_NETWORK_ONLY, false);

                if (pasteNetworkOnly) {
                    if (nodeData.contains("laserColor")) {
                        int colorInt = nodeData.getInt("laserColor");
                        int alphaInt = nodeData.contains("wrenchAlpha") ? nodeData.getInt("wrenchAlpha") : 255;
                        laserNode.setColor(new Color(colorInt, true), alphaInt);
                        laserNode.updateThisNode();
                        player.displayClientMessage(Component.translatable("message.laserio.card_cloner.network_settings_pasted"), true);
                        player.level().playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                } else {
                    // === 1. 准备阶段 ===
                    ItemStack cardHolder = LaserNode.findFirstCardHolder(player);
                    
                    // 解析需求列表
                    List<ItemStack> requiredCards = new ArrayList<>();
                    for (int i = 0; i < Direction.values().length; i++) {
                        if (nodeData.contains("Inventory" + i)) {
                            ItemStackHandler handler = new ItemStackHandler(9);
                            handler.deserializeNBT(player.registryAccess(), nodeData.getCompound("Inventory" + i));
                            for (int slot = 0; slot < handler.getSlots(); slot++) {
                                ItemStack stack = handler.getStackInSlot(slot);
                                if (!stack.isEmpty()) {
                                    requiredCards.add(stack.copy());
                                }
                            }
                        }
                    }
                    // 解析节点超频
                    if (nodeData.contains("inv")) {
                        ItemStackHandler ocHandler = new ItemStackHandler(3);
                        ocHandler.deserializeNBT(player.registryAccess(), nodeData.getCompound("inv"));
                        for (int i = 0; i < ocHandler.getSlots(); i++) {
                            ItemStack stack = ocHandler.getStackInSlot(i);
                            if (!stack.isEmpty()) {
                                requiredCards.add(stack.copy());
                            }
                        }
                    }

                    // === 2. 模拟扣费检查 ===
                    Inventory simPlayerInv = new Inventory(player);
                    simPlayerInv.replaceWith(player.getInventory());
                    ItemStack simCardHolder = cardHolder.copy();
                    
                    boolean canAfford = true;
                    for (ItemStack req : requiredCards) {
                        if (!simulateConsume(req, simCardHolder, simPlayerInv)) {
                            canAfford = false;
                            break;
                        }
                    }

                    if (canAfford) {
                        // === 3. 执行阶段 ===

                        // A. [核心修改] 清空当前节点并自动拆解 (Dismantle & Dump)
                        for (Direction direction : Direction.values()) {
                            IItemHandler handler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
                            if (handler != null) {
                                dumpAndDismantleHandler(handler, cardHolder, player);
                            }
                        }

                        // B. 粘贴数据
                        CompoundTag pasteTag = nodeData.copy();
                        pasteTag.remove("x");
                        pasteTag.remove("y");
                        pasteTag.remove("z");
                        pasteTag.remove("id");
                        laserNode.loadAdditional(pasteTag, player.registryAccess());
                        laserNode.updateThisNode();

                        // C. 真实扣费
                        for (ItemStack req : requiredCards) {
                            executeConsume(req, cardHolder, player.getInventory());
                        }

                        player.displayClientMessage(Component.translatable("message.laserio.card_cloner.node_contents_pasted"), true);
                        player.level().playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    } else {
                        player.displayClientMessage(Component.translatable("message.laserio.card_cloner.insufficient_materials"), true);
                        player.level().playSound(null, pos, SoundEvents.WAXED_SIGN_INTERACT_FAIL, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                }
            }
        });
    }

    // [新增] 辅助方法：清空 Handler，拆解卡片，并返还物品
    private void dumpAndDismantleHandler(IItemHandler handler, ItemStack cardHolder, ServerPlayer player) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // 如果是卡片，进行拆解
                if (stack.getItem() instanceof BaseCard) {
                    List<ItemStack> parts = dismantleCard(stack);
                    for (ItemStack part : parts) {
                        giveBackItem(part, cardHolder, player);
                    }
                } else {
                    // 如果是其他物品（如节点超频），直接返还
                    // [FIXED] Use new ItemStack to clear NBT from returned generic items too
                    giveBackItem(new ItemStack(stack.getItem(), stack.getCount()), cardHolder, player);
                }
                
                // 清空节点槽位
                handler.extractItem(i, stack.getCount(), false);
            }
        }
    }

    // [新增] 拆解卡片逻辑：返回 [白板卡, 过滤器*N, 超频插件*N]
    private List<ItemStack> dismantleCard(ItemStack cardStack) {
        List<ItemStack> parts = new ArrayList<>();
        int stackCount = cardStack.getCount();

        // 1. 获取卡片内部库存
        CardItemHandler cardHandler;
        if (cardStack.getItem() instanceof CardEnergy) {
            cardHandler = new CardItemHandler(CardEnergyContainer.SLOTS, cardStack);
        } else {
            cardHandler = BaseCard.getInventory(cardStack);
        }

        // 2. 提取内部组件 (Filter / Overclockers)
        for (int i = 0; i < cardHandler.getSlots(); i++) {
            ItemStack comp = cardHandler.getStackInSlot(i);
            if (!comp.isEmpty()) {
                // [FIXED] Construct new ItemStack to effectively strip NBT from returned components
                ItemStack returnComp = new ItemStack(comp.getItem(), comp.getCount() * stackCount);
                parts.add(returnComp);
            }
        }

        // 3. 返回白板卡
        parts.add(new ItemStack(cardStack.getItem(), stackCount));

        return parts;
    }

    // [新增] 统一返还逻辑：优先卡包，次选背包，最后掉落
    private void giveBackItem(ItemStack stack, ItemStack cardHolder, ServerPlayer player) {
        if (stack.isEmpty()) return;
        ItemStack remainder = stack;

        // 1. 尝试放入卡包
        if (!cardHolder.isEmpty()) {
            remainder = CardHolder.addCardToInventory(cardHolder, remainder);
        }
        
        // 2. 尝试放入玩家背包
        if (!remainder.isEmpty()) {
            if (player.getInventory().add(remainder)) {
                remainder = ItemStack.EMPTY;
            }
        }
        
        // 3. 掉落
        if (!remainder.isEmpty()) {
            ItemEntity itementity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), remainder);
            player.level().addFreshEntity(itementity);
        }
    }

    // ================= 以下为扣费逻辑 (保持不变) =================

    private boolean simulateConsume(ItemStack target, ItemStack cardHolder, Inventory playerInv) {
        if (findExactMatch(target, cardHolder, playerInv, true)) {
            return true;
        }
        return findRawMaterials(target, cardHolder, playerInv, true);
    }

    private void executeConsume(ItemStack target, ItemStack cardHolder, Inventory playerInv) {
        if (findExactMatch(target, cardHolder, playerInv, false)) {
            return;
        }
        findRawMaterials(target, cardHolder, playerInv, false);
    }

    private boolean findExactMatch(ItemStack target, ItemStack cardHolder, Inventory playerInv, boolean simulate) {
        int amountNeeded = target.getCount(); 

        if (!cardHolder.isEmpty()) {
            // 注意：CardHolderItemStackHandler 通常用于包装
            // 此处直接操作 IItemHandler 可能更通用，但 CardHolderItemStackHandler 提供了便捷方法
            // 这里为了保持逻辑一致，我们假设 ItemHandler 已经可用
            // ...
            // (Simulated logic omitted for brevity, assuming standard extraction)
            // But CardHolder uses ComponentItemHandler usually.
            // Let's assume we can iterate slots.
            IItemHandler handler = cardHolder.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack inSlot = handler.getStackInSlot(i);
                    if (ItemStack.isSameItemSameComponents(inSlot, target)) {
                        int toExtract = Math.min(amountNeeded, inSlot.getCount());
                        if (!simulate) {
                            handler.extractItem(i, toExtract, false);
                        }
                        amountNeeded -= toExtract;
                        if (amountNeeded == 0) return true;
                    }
                }
            }
        }

        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack inSlot = playerInv.getItem(i);
            if (ItemStack.isSameItemSameComponents(inSlot, target)) {
                int toExtract = Math.min(amountNeeded, inSlot.getCount());
                if (!simulate) {
                    playerInv.removeItem(i, toExtract);
                }
                amountNeeded -= toExtract;
                if (amountNeeded == 0) return true;
            }
        }
        return false;
    }

    private boolean findRawMaterials(ItemStack target, ItemStack cardHolder, Inventory playerInv, boolean simulate) {
        // 1. 扣除基础物品
        ItemStack baseItem = new ItemStack(target.getItem(), target.getCount());
        if (!consumeItem(baseItem, cardHolder, playerInv, simulate)) {
            return false;
        }

        // 2. 分析内部组件
        List<ItemStack> componentsNeeded = new ArrayList<>();
        CardItemHandler handler;
        if (target.getItem() instanceof CardEnergy) {
            handler = new CardItemHandler(CardEnergyContainer.SLOTS, target);
        } else if (target.getItem() instanceof BaseCard) {
            handler = BaseCard.getInventory(target);
        } else {
            return true; // 非卡片物品 (如 OverclockerNode)，无内部组件
        }

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack comp = handler.getStackInSlot(i);
            if (!comp.isEmpty()) {
                ItemStack neededComp = comp.copy();
                neededComp.setCount(comp.getCount() * target.getCount());
                componentsNeeded.add(neededComp);
            }
        }

        // 3. 扣除组件
        for (ItemStack comp : componentsNeeded) {
            if (!consumeItem(comp, cardHolder, playerInv, simulate)) {
                return false;
            }
        }

        return true;
    }

    private boolean consumeItem(ItemStack needed, ItemStack cardHolder, Inventory playerInv, boolean simulate) {
        int amountNeeded = needed.getCount();

        if (!cardHolder.isEmpty()) {
            IItemHandler handler = cardHolder.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack inSlot = handler.getStackInSlot(i);
                    // [IMPORTANT] Only consume blank items for raw material needs
                    if (inSlot.is(needed.getItem()) && inSlot.isComponentsPatchEmpty()) {
                        int extract = Math.min(amountNeeded, inSlot.getCount());
                        if (!simulate) {
                            handler.extractItem(i, extract, false);
                        }
                        amountNeeded -= extract;
                        if (amountNeeded == 0) return true;
                    }
                }
            }
        }

        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack inSlot = playerInv.getItem(i);
            // [IMPORTANT] Only consume blank items
            if (inSlot.is(needed.getItem()) && inSlot.isComponentsPatchEmpty()) {
                int extract = Math.min(amountNeeded, inSlot.getCount());
                if (!simulate) {
                    playerInv.removeItem(i, extract);
                }
                amountNeeded -= extract;
                if (amountNeeded == 0) return true;
            }
        }

        return amountNeeded == 0;
    }
}