package com.direwolf20.laserio.common.network.handler;

import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.blocks.LaserNode;
import com.direwolf20.laserio.common.items.CardCloner;
import com.direwolf20.laserio.common.items.CardHolder;
import com.direwolf20.laserio.common.network.data.CopyPasteNodePayload;
import com.direwolf20.laserio.setup.LaserIODataComponents;
import com.direwolf20.laserio.util.CardHolderItemStackHandler;
import com.direwolf20.laserio.util.ItemHandlerUtil.InventoryCardCounts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.awt.Color;

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
                    // [修复] 使用静态方法 findFirstCardHolder，且参数正确
                    ItemStack cardHolder = LaserNode.findFirstCardHolder(player);
                    
                    InventoryCardCounts neededItems = CardCloner.getCopiedNodeContents(clonerStack, player.registryAccess());
                    
                    InventoryCardCounts existingItems = new InventoryCardCounts();
                    // [修复] 使用 NeoForge 1.21 新的 Capability API (通过 Level 获取)
                    for (Direction direction : Direction.values()) {
                        var handler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
                        if (handler != null) {
                            existingItems.addHandler(handler, true);
                        }
                    }
                    
                    existingItems.subtractInventoryCardCounts(neededItems);

                    boolean enoughItems = true;
                    if (existingItems.hasNegativeValues()) {
                        if (!cardHolder.isEmpty()) {
                            InventoryCardCounts totalExisting = existingItems.clone();
                            InventoryCardCounts holderCounts = new InventoryCardCounts(new CardHolderItemStackHandler(27, cardHolder), false);
                            totalExisting.addInventoryCardCounts(holderCounts);
                            enoughItems = !totalExisting.hasNegativeValues();
                        } else {
                            enoughItems = false;
                        }
                    }

                    if (enoughItems) {
                        transferItems(existingItems.getCardCounts(), existingItems, cardHolder, player);
                        transferItems(existingItems.getCardModifierCounts(), existingItems, cardHolder, player);

                        CompoundTag pasteTag = nodeData.copy();
                        pasteTag.remove("x");
                        pasteTag.remove("y");
                        pasteTag.remove("z");
                        pasteTag.remove("id");

                        laserNode.loadAdditional(pasteTag, player.registryAccess());
                        laserNode.updateThisNode();
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

    private void transferItems(it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<Item> itemCounts, InventoryCardCounts inventoryCardCounts, ItemStack cardHolder, ServerPlayer player) {
        itemCounts.object2IntEntrySet().forEach(entry -> {
            Item item = entry.getKey();
            int quantity = entry.getIntValue();

            if (quantity < 0) {
                int toTake = Math.abs(quantity);
                ItemStack neededStack = new ItemStack(item, toTake);
                CardHolder.extractCard(cardHolder, neededStack, false);
            } else if (quantity > 0) {
                while (quantity > 0) {
                    int stackSize = Math.min(quantity, 64);
                    ItemStack toAdd = new ItemStack(item, stackSize);
                    quantity -= stackSize;

                    if (!cardHolder.isEmpty()) {
                        toAdd = CardHolder.addCardToInventory(cardHolder, toAdd);
                    }

                    if (!toAdd.isEmpty()) {
                        ItemEntity itementity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), toAdd);
                        player.level().addFreshEntity(itementity);
                    }
                }
            }
        });
    }
}