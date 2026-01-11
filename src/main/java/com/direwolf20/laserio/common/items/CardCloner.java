package com.direwolf20.laserio.common.items;

import com.direwolf20.laserio.client.blockentityrenders.LaserNodeBERender;
import com.direwolf20.laserio.common.containers.CardEnergyContainer;
import com.direwolf20.laserio.common.containers.CardItemContainer;
import com.direwolf20.laserio.common.containers.customhandler.CardItemHandler;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardEnergy;
import com.direwolf20.laserio.common.items.cards.CardRedstone;
import com.direwolf20.laserio.common.network.data.CopyPasteNodePayload;
import com.direwolf20.laserio.setup.Config;
import com.direwolf20.laserio.setup.LaserIODataComponents;
import com.direwolf20.laserio.util.ItemHandlerUtil.InventoryCardCounts;
import com.direwolf20.laserio.util.VectorHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

import static com.direwolf20.laserio.util.MiscTools.tooltipMaker;
import static com.direwolf20.laserio.util.MiscTools.tooltipMakerLiteral;

public class CardCloner extends Item {

    public CardCloner() {
        super(new Properties()
                .stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 使用配置中的距离进行射线检测
        int range = Config.MAX_INTERACTION_RANGE.get();
        BlockHitResult result = VectorHelper.getLookingAt(player, ClipContext.Fluid.NONE, range);

        // 逻辑：如果没点到节点 (点击了空气或非LaserNode方块) -> 执行“清除设置”或“切换粘贴模式”
        if (result.getType() == HitResult.Type.MISS || !(level.getBlockState(result.getBlockPos()).getBlock() instanceof com.direwolf20.laserio.common.blocks.LaserNode)) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    // Shift+右键空气：清除所有设置
                    stack.remove(LaserIODataComponents.CLONER_NODE_DATA);
                    stack.remove(LaserIODataComponents.CARD_CLONER_ITEM_TYPE);
                    stack.remove(LaserIODataComponents.ITEMSTACK_HANDLER); // 清除可能存在的其他组件
                    // 实际上我们主要关心 CLONER_NODE_DATA
                    
                    player.displayClientMessage(Component.translatable("message.laserio.card_cloner.stored_settings_cleared"), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.0f);
                } else {
                    // 右键空气：切换粘贴模式 (仅网络设置 vs 完整内容)
                    boolean current = stack.getOrDefault(LaserIODataComponents.CLONER_PASTE_NETWORK_ONLY, false);
                    stack.set(LaserIODataComponents.CLONER_PASTE_NETWORK_ONLY, !current);
                    
                    String modeKey = !current ? "message.laserio.card_cloner.paste_mode.network_settings" : "message.laserio.card_cloner.paste_mode.node_contents";
                    player.displayClientMessage(Component.translatable("message.laserio.card_cloner.paste_mode").append(Component.translatable(modeKey)), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
            return InteractionResultHolder.success(stack);
        }

        // 逻辑：如果点到了 LaserNode -> 发送包到服务端进行“复制”或“粘贴”
        if (level.isClientSide) {
            BlockPos pos = result.getBlockPos();
            if (player.isShiftKeyDown()) {
                // Mode 1: Copy Node (复制节点)
                PacketDistributor.sendToServer(new CopyPasteNodePayload(pos, (byte) 1));
            } else {
                // Mode 2: Paste Node (粘贴节点)
                PacketDistributor.sendToServer(new CopyPasteNodePayload(pos, (byte) 2));
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        boolean sneakPressed = Screen.hasShiftDown();
        boolean ctrlPressed = Screen.hasControlDown();

        if (!sneakPressed) {
            tooltip.add(Component.translatable("laserio.tooltip.item.show_settings")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            // 操作指南 Tooltip
            tooltip.add(tooltipMaker("laserio.tooltip.item.card_cloner.in_node_ui", ChatFormatting.GRAY));
            
            MutableComponent toWrite = tooltipMaker("laserio.tooltip.item.card_cloner.in_node_ui.copy_card", " - ", ChatFormatting.GRAY);
            toWrite.append(tooltipMaker("laserio.tooltip.item.keys.left_click", ChatFormatting.WHITE));
            tooltip.add(toWrite);
            
            toWrite = tooltipMaker("laserio.tooltip.item.card_cloner.in_node_ui.paste_card", " - ", ChatFormatting.GRAY);
            toWrite.append(tooltipMaker("laserio.tooltip.item.keys.right_click", ChatFormatting.WHITE));
            tooltip.add(toWrite);

            tooltip.add(tooltipMaker("laserio.tooltip.item.card_cloner.in_world", ChatFormatting.GRAY));
            
            toWrite = tooltipMaker("laserio.tooltip.item.card_cloner.in_world.copy_node", " - ", ChatFormatting.GRAY);
            toWrite.append(tooltipMaker("laserio.tooltip.item.keys.shift_right_click", ChatFormatting.WHITE));
            tooltip.add(toWrite);
            
            toWrite = tooltipMaker("laserio.tooltip.item.card_cloner.in_world.paste_node", " - ", ChatFormatting.GRAY);
            toWrite.append(tooltipMaker("laserio.tooltip.item.keys.right_click", ChatFormatting.WHITE));
            tooltip.add(toWrite);
            
            toWrite = tooltipMaker("laserio.tooltip.item.card_cloner.in_world.clear", " - ", ChatFormatting.GRAY);
            toWrite.append(tooltipMaker("laserio.tooltip.item.keys.shift_right_click", ChatFormatting.WHITE));
            tooltip.add(toWrite);
            
            toWrite = tooltipMaker("laserio.tooltip.item.card_cloner.in_world.change_paste_mode", " - ", ChatFormatting.GRAY);
            toWrite.append(tooltipMaker("laserio.tooltip.item.keys.right_click", ChatFormatting.WHITE));
            tooltip.add(toWrite);

            // 显示已复制的卡片信息 (旧功能)
            String cardType = getItemType(stack);
            toWrite = tooltipMaker("laserio.tooltip.item.filter.type", ChatFormatting.GRAY);
            int cardColor = ChatFormatting.WHITE.getColor();
            if (cardType.equals("card_item"))
                cardColor = ChatFormatting.GREEN.getColor();
            else if (cardType.equals("card_fluid"))
                cardColor = ChatFormatting.BLUE.getColor();
            else if (cardType.equals("card_energy"))
                cardColor = ChatFormatting.YELLOW.getColor();
            else if (cardType.equals("card_redstone"))
                cardColor = ChatFormatting.RED.getColor();
            else if (cardType.equals("card_chemical"))
                cardColor = ChatFormatting.LIGHT_PURPLE.getColor();
            
            if (cardType.equals(""))
                toWrite.append(tooltipMaker("laserio.tooltip.item.card.None", cardColor));
            else
                toWrite.append(tooltipMaker("item.laserio." + cardType, cardColor));
            tooltip.add(toWrite);

            if (!cardType.equals("")) {
                // 显示卡片详情 (模式、频道等)
                int mode = stack.getOrDefault(LaserIODataComponents.CARD_TRANSFER_MODE, (byte)0).intValue();
                String currentMode = BaseCard.TransferMode.values()[mode].toString();
                toWrite = tooltipMaker("laserio.tooltip.item.card.mode", ChatFormatting.GRAY);
                int modeColor = ChatFormatting.GRAY.getColor();
                if (currentMode.equals("EXTRACT"))
                    modeColor = ChatFormatting.RED.getColor();
                else if (currentMode.equals("INSERT"))
                    modeColor = ChatFormatting.GREEN.getColor();
                else if (currentMode.equals("STOCK"))
                    modeColor = ChatFormatting.BLUE.getColor();
                else if (currentMode.equals("SENSOR"))
                    modeColor = ChatFormatting.YELLOW.getColor();
                toWrite.append(tooltipMaker("laserio.tooltip.item.card.mode." + currentMode, modeColor));
                tooltip.add(toWrite);

                toWrite = tooltipMaker("laserio.tooltip.item.card.channel", ChatFormatting.GRAY);
                int channel = stack.getOrDefault(LaserIODataComponents.CARD_CHANNEL, (byte)0).intValue();
                toWrite.append(tooltipMaker(String.valueOf(channel), LaserNodeBERender.colors[channel].getRGB()));
                tooltip.add(toWrite);

                // 只有非红石卡显示过滤器
                if (!cardType.equals("card_redstone")) {
                    toWrite = tooltipMaker("laserio.tooltip.item.card.Filter", ChatFormatting.GRAY);
                    ItemStack filterStack = getCopiedCardFilter(stack);
                    if (filterStack.isEmpty())
                        toWrite.append(tooltipMaker("laserio.tooltip.item.card.None", ChatFormatting.WHITE));
                    else
                        toWrite.append(tooltipMaker("item.laserio." + filterStack.getItem(), ChatFormatting.DARK_AQUA));
                    tooltip.add(toWrite);
                }

                // 显示超频插件数量 (红石卡除外，能量卡仅在 1 槽模式下显示)
                if (!cardType.equals("card_redstone")) {
                    toWrite = tooltipMaker("laserio.tooltip.item.card.Overclockers", ChatFormatting.GRAY);
                    ItemStack overclockStack = getCopiedCardOverclocker(stack);
                    if (overclockStack.isEmpty())
                        toWrite.append(tooltipMaker(String.valueOf(0), ChatFormatting.WHITE));
                    else
                        toWrite.append(tooltipMaker(String.valueOf(overclockStack.getCount()), ChatFormatting.DARK_AQUA));
                    tooltip.add(toWrite);
                }
            }
        }

        if (ctrlPressed) {
            // 显示已复制的节点信息 (新功能)
            tooltip.add(tooltipMaker("laserio.tooltip.item.card_cloner.copied_node", ChatFormatting.GRAY));
            CompoundTag nodeTag = stack.getOrDefault(LaserIODataComponents.CLONER_NODE_DATA, new CompoundTag());

            MutableComponent toWrite = tooltipMaker("laserio.tooltip.item.card_cloner.copied_node.position", " - ", ChatFormatting.GRAY);
            if (nodeTag.isEmpty()) {
                toWrite.append(tooltipMaker("laserio.tooltip.item.card.None", ChatFormatting.WHITE));
            } else {
                if (nodeTag.contains("x") && nodeTag.contains("y") && nodeTag.contains("z")) {
                    String posString = nodeTag.getInt("x") + ", " + nodeTag.getInt("y") + ", " + nodeTag.getInt("z");
                    toWrite.append(tooltipMakerLiteral(posString, ChatFormatting.AQUA));
                } else {
                    toWrite.append(tooltipMakerLiteral("Saved", ChatFormatting.AQUA));
                }
            }
            tooltip.add(toWrite);

            if (!nodeTag.isEmpty()) {
                toWrite = tooltipMaker("laserio.tooltip.item.card_cloner.copied_node.dimension", " - ", ChatFormatting.GRAY);
                toWrite.append(tooltipMakerLiteral(nodeTag.getString("dimension"), ChatFormatting.DARK_AQUA));
                tooltip.add(toWrite);
            }
            
            // 显示粘贴模式
            boolean pasteNetwork = getPasteNetworkSettings(stack);
            toWrite = tooltipMaker("message.laserio.card_cloner.paste_mode", ": ", ChatFormatting.GRAY);
            String modeKey = pasteNetwork ? "message.laserio.card_cloner.paste_mode.network_settings" : "message.laserio.card_cloner.paste_mode.node_contents";
            toWrite.append(Component.translatable(modeKey).withStyle(ChatFormatting.GOLD));
            tooltip.add(toWrite);
            
        } else {
            tooltip.add(tooltipMaker("laserio.tooltip.item.show_settings.ctrl_key", ChatFormatting.GRAY));
        }
    }

    /**
     * Retrieves the contents of the copied node from the Card Cloner.
     * @param stack The Card Cloner ItemStack.
     * @param provider The Registry Access Provider (required for NeoForge 1.21+ standard).
     * @return InventoryCardCounts containing the items.
     */
    public static InventoryCardCounts getCopiedNodeContents(ItemStack stack, HolderLookup.Provider provider) {
        InventoryCardCounts counts = new InventoryCardCounts();
        CompoundTag tag = stack.getOrDefault(LaserIODataComponents.CLONER_NODE_DATA, new CompoundTag());
        if (tag.isEmpty()) return counts;

        if (provider == null) {
            return counts;
        }

        for (int i = 0; i < Direction.values().length; i++) {
            if (tag.contains("Inventory" + i)) {
                ItemStackHandler handler = new ItemStackHandler(9); // 节点每面9个槽
                handler.deserializeNBT(provider, tag.getCompound("Inventory" + i));
                counts.addHandler(handler);
            }
        }
        return counts;
    }

    public static void setItemType(ItemStack stack, String itemType) {
        stack.set(LaserIODataComponents.CARD_CLONER_ITEM_TYPE, itemType);
    }

    public static String getItemType(ItemStack stack) {
        return stack.getOrDefault(LaserIODataComponents.CARD_CLONER_ITEM_TYPE, "");
    }

    // 辅助方法：兼容旧版代码，虽然现在使用 DataComponents
    public static void saveSettings(ItemStack stack, DataComponentPatch dataComponentPatch) {
        // 在 NeoForge 1.21 中，我们通常直接 set 组件，而不是批量 apply patch
        // 这里只是为了保持可能的兼容性，实际使用中应直接操作 stack
        stack.applyComponents(dataComponentPatch);
    }

    public static DataComponentPatch getSettings(ItemStack stack) {
        return stack.getComponentsPatch();
    }

    // 获取已复制卡片的过滤器 (用于Tooltip)
    public static ItemStack getCopiedCardFilter(ItemStack stack) {
        // 创建一个临时的 Handler 来模拟卡片容器
        CardItemHandler cardItemHandler = new CardItemHandler(CardItemContainer.SLOTS, stack);
        return cardItemHandler.getStackInSlot(0);
    }

    // 获取已复制卡片的超频插件 (用于Tooltip)
    public static ItemStack getCopiedCardOverclocker(ItemStack stack) {
        String cardType = getItemType(stack);
        CardItemHandler cardItemHandler = new CardItemHandler(CardItemContainer.SLOTS, stack);
        
        if (cardType.equals("card_energy")) {
            if (CardEnergyContainer.SLOTS == 1) {
                // 能量卡只有1个槽 (0)，没有超频槽
                // 除非 Config 改了，否则这里应该返回空或者根据具体逻辑
                return ItemStack.EMPTY; 
            }
        }
        
        // 物品卡、流体卡等，超频在槽位 1
        return cardItemHandler.getStackInSlot(1);
    }
    
    // --- 兼容旧方法的重定向 ---
    public static ItemStack getFilter(ItemStack stack) { return getCopiedCardFilter(stack); }
    public static ItemStack getOverclocker(ItemStack stack) { return getCopiedCardOverclocker(stack); }

    public static boolean getPasteNetworkSettings(ItemStack stack) {
        return stack.getOrDefault(LaserIODataComponents.CLONER_PASTE_NETWORK_ONLY, false);
    }
}