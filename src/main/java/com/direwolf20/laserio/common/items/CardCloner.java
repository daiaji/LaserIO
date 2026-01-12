package com.direwolf20.laserio.common.items;

import com.direwolf20.laserio.client.blockentityrenders.LaserNodeBERender;
import com.direwolf20.laserio.common.containers.CardEnergyContainer;
import com.direwolf20.laserio.common.containers.CardItemContainer;
import com.direwolf20.laserio.common.containers.customhandler.CardItemHandler;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardEnergy;
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
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
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
        super(new Properties().stacksTo(1));
    }

    // ====================================================================================================
    // [修复] 重新加入物理拦截，防止物品交换
    // ====================================================================================================

    @Override
    public boolean overrideStackedOnOther(ItemStack clonerStack, Slot slot, ClickAction action, Player player) {
        // 如果目标是卡片，返回 true。
        // 这告诉游戏核心：“这个 Item 已经处理了这次点击，请不要执行默认的交换/合并逻辑”。
        // 注意：这里不需要发送网络包，因为 ClientEvents.java 已经负责发包了。
        // 这里纯粹是为了防止客户端预测导致的视觉交换。
        if (slot.getItem().getItem() instanceof BaseCard) {
            return true;
        }
        return false;
    }

    // ====================================================================================================
    // 下面的代码保持不变
    // ====================================================================================================

    public static void setItemType(ItemStack stack, String itemType) {
        stack.set(LaserIODataComponents.CARD_CLONER_ITEM_TYPE, itemType);
    }

    public static String getItemType(ItemStack stack) {
        return stack.getOrDefault(LaserIODataComponents.CARD_CLONER_ITEM_TYPE, "");
    }

    public static void saveSettings(ItemStack stack, DataComponentPatch dataComponentPatch) {
        cleanCardData(stack); 
        stack.applyComponents(dataComponentPatch);
    }

    public static DataComponentPatch getSettings(ItemStack stack) {
        return stack.getComponentsPatch();
    }

    public static ItemStack getCopiedCardFilter(ItemStack stack) {
        CardItemHandler cardItemHandler = new CardItemHandler(CardItemContainer.SLOTS, stack);
        return cardItemHandler.getStackInSlot(0);
    }

    public static ItemStack getCopiedCardOverclocker(ItemStack stack) {
        String cardType = getItemType(stack);
        CardItemHandler cardItemHandler = new CardItemHandler(CardItemContainer.SLOTS, stack);
        if (cardType.equals("card_energy") && CardEnergyContainer.SLOTS == 1) return ItemStack.EMPTY;
        return cardItemHandler.getStackInSlot(1);
    }

    public static ItemStack getFilter(ItemStack stack) { return getCopiedCardFilter(stack); }
    public static ItemStack getOverclocker(ItemStack stack) { return getCopiedCardOverclocker(stack); }
    public static boolean getPasteNetworkSettings(ItemStack stack) {
        return stack.getOrDefault(LaserIODataComponents.CLONER_PASTE_NETWORK_ONLY, false);
    }

    public static InventoryCardCounts getCopiedNodeContents(ItemStack stack, HolderLookup.Provider provider) {
        InventoryCardCounts counts = new InventoryCardCounts();
        CompoundTag tag = stack.getOrDefault(LaserIODataComponents.CLONER_NODE_DATA, new CompoundTag());
        if (tag.isEmpty() || provider == null) return counts;
        for (int i = 0; i < Direction.values().length; i++) {
            if (tag.contains("Inventory" + i)) {
                ItemStackHandler handler = new ItemStackHandler(9);
                handler.deserializeNBT(provider, tag.getCompound("Inventory" + i));
                counts.addHandler(handler);
            }
        }
        return counts;
    }

    public static void cleanCardData(ItemStack stack) {
        stack.remove(LaserIODataComponents.CARD_CLONER_ITEM_TYPE);
        stack.remove(LaserIODataComponents.ITEMSTACK_HANDLER);
        stack.remove(LaserIODataComponents.CLONER_NODE_DATA);
        stack.remove(LaserIODataComponents.CARD_TRANSFER_MODE);
        stack.remove(LaserIODataComponents.CARD_CHANNEL);
        stack.remove(LaserIODataComponents.CARD_EXTRACT_SPEED);
        stack.remove(LaserIODataComponents.CARD_PRIORITY);
        stack.remove(LaserIODataComponents.CARD_SNEAKY);
        stack.remove(LaserIODataComponents.CARD_REGULATE);
        stack.remove(LaserIODataComponents.CARD_ROUND_ROBIN);
        stack.remove(LaserIODataComponents.CARD_REDSTONE_MODE);
        stack.remove(LaserIODataComponents.CARD_EXACT);
        stack.remove(LaserIODataComponents.CARD_REDSTONE_CHANNEL);
        stack.remove(LaserIODataComponents.CARD_AND_MODE);
        stack.remove(LaserIODataComponents.CARD_MAX_BACKOFF);
        stack.remove(LaserIODataComponents.ENERGY_CARD_EXTRACT_AMT);
        stack.remove(LaserIODataComponents.ENERGY_CARD_EXTRACT_SPEED);
        stack.remove(LaserIODataComponents.ENERGY_CARD_INSERT_LIMIT);
        stack.remove(LaserIODataComponents.ENERGY_CARD_EXTRACT_LIMIT);
        stack.remove(LaserIODataComponents.FLUID_CARD_EXTRACT_AMT);
        stack.remove(LaserIODataComponents.ITEM_CARD_EXTRACT_AMT);
        stack.remove(LaserIODataComponents.REDSTONE_CARD_STRONG);
        stack.remove(LaserIODataComponents.CHEMICAL_CARD_EXTRACT_AMT);
        stack.remove(LaserIODataComponents.FILTER_ALLOW);
        stack.remove(LaserIODataComponents.FILTER_COMPARE);
        stack.remove(LaserIODataComponents.FILTER_COUNT_MBAMT);
        stack.remove(LaserIODataComponents.FILTER_COUNT_SLOT_COUNTS);
        stack.remove(LaserIODataComponents.FILTER_TAG_TAGS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int range = Config.MAX_INTERACTION_RANGE.get();
        BlockHitResult result = VectorHelper.getLookingAt(player, ClipContext.Fluid.NONE, range);

        if (result.getType() == HitResult.Type.MISS || !(level.getBlockState(result.getBlockPos()).getBlock() instanceof com.direwolf20.laserio.common.blocks.LaserNode)) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    cleanCardData(stack); 
                    player.displayClientMessage(Component.translatable("message.laserio.card_cloner.stored_settings_cleared"), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.0f);
                } else {
                    boolean current = stack.getOrDefault(LaserIODataComponents.CLONER_PASTE_NETWORK_ONLY, false);
                    stack.set(LaserIODataComponents.CLONER_PASTE_NETWORK_ONLY, !current);
                    String modeKey = !current ? "message.laserio.card_cloner.paste_mode.network_settings" : "message.laserio.card_cloner.paste_mode.node_contents";
                    player.displayClientMessage(Component.translatable("message.laserio.card_cloner.paste_mode").append(Component.translatable(modeKey)), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
            return InteractionResultHolder.success(stack);
        }

        if (level.isClientSide) {
            BlockPos pos = result.getBlockPos();
            if (player.isShiftKeyDown()) {
                PacketDistributor.sendToServer(new CopyPasteNodePayload(pos, (byte) 1));
            } else {
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
        if (mc.level == null || mc.player == null) return;

        boolean sneakPressed = Screen.hasShiftDown();
        boolean ctrlPressed = Screen.hasControlDown();

        if (!sneakPressed) {
            tooltip.add(Component.translatable("laserio.tooltip.item.show_settings").withStyle(ChatFormatting.GRAY));
        } else {
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

            String cardType = getItemType(stack);
            toWrite = tooltipMaker("laserio.tooltip.item.filter.type", ChatFormatting.GRAY);
            int cardColor = ChatFormatting.WHITE.getColor();
            if (cardType.equals("card_item")) cardColor = ChatFormatting.GREEN.getColor();
            else if (cardType.equals("card_fluid")) cardColor = ChatFormatting.BLUE.getColor();
            else if (cardType.equals("card_energy")) cardColor = ChatFormatting.YELLOW.getColor();
            else if (cardType.equals("card_redstone")) cardColor = ChatFormatting.RED.getColor();
            else if (cardType.equals("card_chemical")) cardColor = ChatFormatting.LIGHT_PURPLE.getColor();
            
            if (cardType.equals("")) toWrite.append(tooltipMaker("laserio.tooltip.item.card.None", cardColor));
            else toWrite.append(tooltipMaker("item.laserio." + cardType, cardColor));
            tooltip.add(toWrite);

            if (!cardType.equals("")) {
                int mode = stack.getOrDefault(LaserIODataComponents.CARD_TRANSFER_MODE, (byte)0).intValue();
                String currentMode = BaseCard.TransferMode.values()[mode].toString();
                toWrite = tooltipMaker("laserio.tooltip.item.card.mode", ChatFormatting.GRAY);
                int modeColor = ChatFormatting.GRAY.getColor();
                if (currentMode.equals("EXTRACT")) modeColor = ChatFormatting.RED.getColor();
                else if (currentMode.equals("INSERT")) modeColor = ChatFormatting.GREEN.getColor();
                else if (currentMode.equals("STOCK")) modeColor = ChatFormatting.BLUE.getColor();
                else if (currentMode.equals("SENSOR")) modeColor = ChatFormatting.YELLOW.getColor();
                toWrite.append(tooltipMaker("laserio.tooltip.item.card.mode." + currentMode, modeColor));
                tooltip.add(toWrite);

                toWrite = tooltipMaker("laserio.tooltip.item.card.channel", ChatFormatting.GRAY);
                int channel = stack.getOrDefault(LaserIODataComponents.CARD_CHANNEL, (byte)0).intValue();
                toWrite.append(tooltipMaker(String.valueOf(channel), LaserNodeBERender.colors[channel].getRGB()));
                tooltip.add(toWrite);

                if (!cardType.equals("card_redstone")) {
                    toWrite = tooltipMaker("laserio.tooltip.item.card.Filter", ChatFormatting.GRAY);
                    ItemStack filterStack = getCopiedCardFilter(stack);
                    if (filterStack.isEmpty()) toWrite.append(tooltipMaker("laserio.tooltip.item.card.None", ChatFormatting.WHITE));
                    else toWrite.append(tooltipMaker("item.laserio." + filterStack.getItem(), ChatFormatting.DARK_AQUA));
                    tooltip.add(toWrite);
                }

                if (!cardType.equals("card_redstone")) {
                    toWrite = tooltipMaker("laserio.tooltip.item.card.Overclockers", ChatFormatting.GRAY);
                    ItemStack overclockStack = getCopiedCardOverclocker(stack);
                    if (overclockStack.isEmpty()) toWrite.append(tooltipMaker(String.valueOf(0), ChatFormatting.WHITE));
                    else toWrite.append(tooltipMaker(String.valueOf(overclockStack.getCount()), ChatFormatting.DARK_AQUA));
                    tooltip.add(toWrite);
                }
            }
        }

        if (ctrlPressed) {
            tooltip.add(tooltipMaker("laserio.tooltip.item.card_cloner.copied_node", ChatFormatting.GRAY));
            CompoundTag nodeTag = stack.getOrDefault(LaserIODataComponents.CLONER_NODE_DATA, new CompoundTag());
            MutableComponent toWrite = tooltipMaker("laserio.tooltip.item.card_cloner.copied_node.position", " - ", ChatFormatting.GRAY);
            if (nodeTag.isEmpty()) {
                toWrite.append(tooltipMaker("laserio.tooltip.item.card.None", ChatFormatting.WHITE));
            } else {
                if (nodeTag.contains("x")) {
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
            boolean pasteNetwork = getPasteNetworkSettings(stack);
            toWrite = tooltipMaker("message.laserio.card_cloner.paste_mode", ": ", ChatFormatting.GRAY);
            String modeKey = pasteNetwork ? "message.laserio.card_cloner.paste_mode.network_settings" : "message.laserio.card_cloner.paste_mode.node_contents";
            toWrite.append(Component.translatable(modeKey).withStyle(ChatFormatting.GOLD));
            tooltip.add(toWrite);
        } else {
            tooltip.add(tooltipMaker("laserio.tooltip.item.show_settings.ctrl_key", ChatFormatting.GRAY));
        }
    }
}