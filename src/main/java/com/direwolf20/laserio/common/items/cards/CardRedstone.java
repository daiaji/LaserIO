package com.direwolf20.laserio.common.items.cards;

import com.direwolf20.laserio.common.containers.CardRedstoneContainer;
import com.direwolf20.laserio.setup.LaserIODataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;


public class CardRedstone extends BaseCard {

    public CardRedstone() {
        super();
        CARDTYPE = BaseCard.CardType.REDSTONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (level.isClientSide()) return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);

        player.openMenu(new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new CardRedstoneContainer(windowId, playerInventory, player, itemstack), Component.translatable("")), (buf -> {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, itemstack);
            buf.writeByte(-1);
        }));

        return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);
    }

    public static byte nextTransferMode(ItemStack card) {
        byte mode = getTransferMode(card);
        return setTransferMode(card, (byte) (mode == 1 ? 0 : mode + 1));
    }

    public static boolean getStrong(ItemStack stack) {
        return stack.getOrDefault(LaserIODataComponents.REDSTONE_CARD_STRONG, false);
    }

    public static boolean setStrong(ItemStack stack, boolean strong) {
        if (!strong)
            stack.remove(LaserIODataComponents.REDSTONE_CARD_STRONG);
        else
            stack.set(LaserIODataComponents.REDSTONE_CARD_STRONG, strong);
        return strong;
    }

    // [Fix Patch #52] Added getters and setters for new Redstone Card features
    // Using CustomData (NBT) for compatibility as LaserIODataComponents is not modified

    public static boolean getInterval(ItemStack card) {
        return card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("redstoneinterval");
    }

    public static boolean setInterval(ItemStack card, boolean interval) {
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> {
            if (!interval) tag.remove("redstoneinterval");
            else tag.putBoolean("redstoneinterval", interval);
        });
        return interval;
    }

    public static byte getIntervalLowerBound(ItemStack card) {
        return card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getByte("redstoneintervallowerbound");
    }

    public static byte setIntervalLowerBound(ItemStack card, byte intervalLowerBound) {
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> {
            if (intervalLowerBound == 0) tag.remove("redstoneintervallowerbound");
            else tag.putByte("redstoneintervallowerbound", intervalLowerBound);
        });
        return intervalLowerBound;
    }

    public static byte getIntervalUpperBound(ItemStack card) {
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("redstoneintervalupperbound") ? tag.getByte("redstoneintervalupperbound") : (byte) 15;
    }

    public static byte setIntervalUpperBound(ItemStack card, byte intervalUpperBound) {
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> {
            if (intervalUpperBound == 15) tag.remove("redstoneintervalupperbound");
            else tag.putByte("redstoneintervalupperbound", intervalUpperBound);
        });
        return intervalUpperBound;
    }

    public static byte getIntervalOutput(ItemStack card) {
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("redstoneintervaloutput") ? tag.getByte("redstoneintervaloutput") : (byte) 15;
    }

    public static byte setIntervalOutput(ItemStack card, byte intervalOutput) {
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> {
            if (intervalOutput == 15) tag.remove("redstoneintervaloutput");
            else tag.putByte("redstoneintervaloutput", intervalOutput);
        });
        return intervalOutput;
    }

    public static byte getOutputMode(ItemStack card) {
        return card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getByte("redstoneoutputmode");
    }

    public static byte setOutputMode(ItemStack card, byte outputMode) {
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> {
            if (outputMode == 0) tag.remove("redstoneoutputmode");
            else tag.putByte("redstoneoutputmode", outputMode);
        });
        return outputMode;
    }

    public static byte getLogicOperation(ItemStack card) {
        return card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getByte("redstonelogicoperation");
    }

    public static byte setLogicOperation(ItemStack card, byte logicOperation) {
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> {
            if (logicOperation == 0) tag.remove("redstonelogicoperation");
            else tag.putByte("redstonelogicoperation", logicOperation);
        });
        return logicOperation;
    }

    public static byte getRedstoneChannelOperation(ItemStack card) {
        return card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getByte("redstonechanneloperation");
    }

    public static byte setRedstoneChannelOperation(ItemStack card, byte logicOperationChannel) {
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> {
            if (logicOperationChannel == 0) tag.remove("redstonechanneloperation");
            else tag.putByte("redstonechanneloperation", logicOperationChannel);
        });
        return logicOperationChannel;
    }

    public static byte nextRedstoneChannelOperation(ItemStack card) {
        byte k = getRedstoneChannelOperation(card);
        return setRedstoneChannelOperation(card, (byte) (k == 15 ? 0 : k + 1));
    }

    public static byte previousRedstoneChannelOperation(ItemStack card) {
        byte k = getRedstoneChannelOperation(card);
        return setRedstoneChannelOperation(card, (byte) (k == 0 ? 15 : k - 1));
    }
}