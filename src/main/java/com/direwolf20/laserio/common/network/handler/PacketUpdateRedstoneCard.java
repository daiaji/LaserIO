package com.direwolf20.laserio.common.network.handler;

import com.direwolf20.laserio.common.containers.CardRedstoneContainer;
import com.direwolf20.laserio.common.items.cards.BaseCard; // [新增] 引入 BaseCard
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
            
            // 应用所有设置
            CardRedstone.setTransferMode(stack, payload.mode());
            BaseCard.setChannel(stack, payload.channel());             // [新增] 设置基础网络频道
            CardRedstone.setRedstoneChannel(stack, payload.redstoneChannel()); // [修改] 设置红石逻辑频道
            CardRedstone.setStrong(stack, payload.strong());
        });
    }
}