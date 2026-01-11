package com.direwolf20.laserio.common.network.handler;

import com.direwolf20.laserio.common.containers.CardHolderContainer;
import com.direwolf20.laserio.common.items.CardHolder;
import com.direwolf20.laserio.common.network.data.KeybindPerformActionPayload;
import com.direwolf20.laserio.integration.ModIntegration;
import com.direwolf20.laserio.integration.curios.CuriosIntegration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketKeybindPerformAction {
    private static final PacketKeybindPerformAction INSTANCE = new PacketKeybindPerformAction();

    public static PacketKeybindPerformAction get() {
        return INSTANCE;
    }

    public void handle(final KeybindPerformActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            ItemStack cardHolder = findCardHolder(player);
            if (cardHolder.isEmpty()) return;

            switch (payload.action()) {
                case OPEN_CARD_HOLDER -> openCardHolder(player, cardHolder);
                case TOGGLE_CARD_HOLDER_PULLING -> togglePulling(player, cardHolder);
            }
        });
    }

    private void openCardHolder(ServerPlayer player, ItemStack cardHolder) {
        player.openMenu(new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new CardHolderContainer(windowId, playerInventory, player, cardHolder),
                Component.translatable("item.laserio.card_holder")
        ), (buf) -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, cardHolder));
    }

    private void togglePulling(ServerPlayer player, ItemStack cardHolder) {
        boolean isActive = CardHolder.getActive(cardHolder);
        CardHolder.setActive(cardHolder, !isActive);
        
        MutableComponent message = CardHolder.PULLING_MESSAGES[!isActive ? 1 : 0];
        player.displayClientMessage(message, true);
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    private ItemStack findCardHolder(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.getItem() instanceof CardHolder) return heldItem;
        
        heldItem = player.getOffhandItem();
        if (heldItem.getItem() instanceof CardHolder) return heldItem;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof CardHolder) return stack;
        }

        if (ModIntegration.CURIOS.isLoaded()) {
            return CuriosIntegration.findFirstCardHolder(player);
        }

        return ItemStack.EMPTY;
    }
}