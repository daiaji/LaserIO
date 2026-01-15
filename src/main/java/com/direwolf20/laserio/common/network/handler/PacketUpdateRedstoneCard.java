package com.direwolf20.laserio.common.network.handler;

import com.direwolf20.laserio.common.containers.CardRedstoneContainer;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardRedstone;
import com.direwolf20.laserio.common.network.data.UpdateRedstoneCardPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketUpdateRedstoneCard {
    public static final PacketUpdateRedstoneCard INSTANCE = new PacketUpdateRedstoneCard();

    public static PacketUpdateRedstoneCard get() {
        return INSTANCE;
    }

    public void handle(final UpdateRedstoneCardPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player sender = context.player();

            AbstractContainerMenu container = sender.containerMenu;
            if (container == null)
                return;

            if (!(container instanceof CardRedstoneContainer))
                return;

            ItemStack stack;
            stack = ((CardRedstoneContainer) container).cardItem;

            CardRedstone.setTransferMode(stack, payload.mode());
            BaseCard.setChannel(stack, payload.channel());
            CardRedstone.setRedstoneChannel(stack, payload.redstoneChannel());
            
            // [Fix Patch #52] Apply new Redstone Card settings
            CardRedstone.setInterval(stack, payload.interval());
            CardRedstone.setIntervalLowerBound(stack, payload.intervalLowerBound());
            CardRedstone.setIntervalUpperBound(stack, payload.intervalUpperBound());
            CardRedstone.setIntervalOutput(stack, payload.intervalOutput());
            CardRedstone.setStrong(stack, payload.strong());
            CardRedstone.setOutputMode(stack, payload.outputMode());
            CardRedstone.setLogicOperation(stack, payload.logicOperation());
            CardRedstone.setRedstoneChannelOperation(stack, payload.logicOperationChannel());
        });
    }
}