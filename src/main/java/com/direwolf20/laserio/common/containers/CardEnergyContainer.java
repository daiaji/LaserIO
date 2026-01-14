package com.direwolf20.laserio.common.containers;

import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.blocks.LaserNode;
import com.direwolf20.laserio.common.containers.customhandler.CardItemHandler;
import com.direwolf20.laserio.common.containers.customslot.CardHolderSlot;
import com.direwolf20.laserio.common.containers.customslot.CardOverclockSlot;
import com.direwolf20.laserio.common.items.CardHolder;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.upgrades.OverclockerCard;
import com.direwolf20.laserio.setup.Config;
import com.direwolf20.laserio.setup.Registration;
import com.direwolf20.laserio.util.CardHolderItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.UUID;

public class CardEnergyContainer extends AbstractContainerMenu {
    public static final int SLOTS = (Config.DEFAULT_TIER_VALUES.isEmpty()) ? 0 : 1;

    public CardItemHandler handler;
    public ItemStack cardItem;
    public Player playerEntity;
    protected IItemHandler playerInventory;
    public BlockPos sourceContainer = BlockPos.ZERO;
    public byte direction = -1;
    public ItemStack cardHolder;
    public IItemHandler cardHolderHandler;
    public UUID cardHolderUUID;

    protected CardEnergyContainer(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }

    public CardEnergyContainer(int windowId, Inventory playerInventory, Player player, RegistryFriendlyByteBuf extraData) {
        this(windowId, playerInventory, player, ItemStack.OPTIONAL_STREAM_CODEC.decode(extraData));
        this.direction = extraData.readByte();
    }

    public CardEnergyContainer(int windowId, Inventory playerInventory, Player player, ItemStack cardItem) {
        super(Registration.CardEnergy_Container.get(), windowId);
        playerEntity = player;

        if (SLOTS == 1) {
            this.handler = new CardItemHandler(SLOTS, cardItem);
        }

        this.playerInventory = new InvWrapper(playerInventory);
        this.cardItem = cardItem;
        this.cardHolder = LaserNode.findFirstCardHolder(player);

        if (handler != null) {
            addSlotRange(handler, 0, 153, 5, 1, 18);
        }

        if (!cardHolder.isEmpty()) {
            this.cardHolderHandler = new CardHolderItemStackHandler(CardHolderContainer.SLOTS, cardHolder);
            addSlotBox(cardHolderHandler, 0, -92, 32, 5, 18, 3, 18);
            cardHolderUUID = CardHolder.getUUID(cardHolder);
        }
        layoutPlayerInventorySlots(8, 84);
    }

    public CardEnergyContainer(int windowId, Inventory playerInventory, Player player, BlockPos sourcePos, ItemStack cardItem, byte direction) {
        this(windowId, playerInventory, player, cardItem);
        this.sourceContainer = sourcePos;
        this.direction = direction;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            ItemStack stackInSlot = slot.getItem();
            if (slot instanceof CardHolderSlot) {
                if (stackInSlot.getItem() instanceof BaseCard) return;
            } else if (stackInSlot.equals(cardItem)) {
                return;
            }
        }
        super.clicked(slotId, dragType, clickTypeIn, player);
    }

    @Override
    public boolean stillValid(Player playerIn) {
        if (cardHolderUUID != null) {
            if (cardHolder.isEmpty() || !CardHolder.getUUID(cardHolder).equals(cardHolderUUID)) {
                return false;
            }
        }
        if (sourceContainer.equals(BlockPos.ZERO))
            return playerIn.getMainHandItem().equals(cardItem) || playerIn.getOffhandItem().equals(cardItem);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        if (SLOTS == 0 || cardItem.getCount() > 1) return ItemStack.EMPTY;

        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemStack = stack.copy();
            if (ItemStack.isSameItemSameComponents(itemStack, cardItem)) return ItemStack.EMPTY;

            int cardHolderSlots = (!cardHolder.isEmpty()) ? CardHolderContainer.SLOTS : 0;

            int overclockSlotEnd = SLOTS;
            int cardHolderStart = overclockSlotEnd;
            int cardHolderEnd = cardHolderStart + cardHolderSlots;
            int playerInvStart = cardHolderEnd;
            int playerInvEnd = playerInvStart + 36;

            if (index < overclockSlotEnd) { // 从超频槽取出
                if (cardHolderSlots > 0 && !this.moveItemStackTo(stack, cardHolderStart, cardHolderEnd, false)) {
                    if (!this.moveItemStackTo(stack, playerInvStart, playerInvEnd, true)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(stack, playerInvStart, playerInvEnd, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, itemStack);
            } else { // 放入超频槽
                if (stack.getItem() instanceof OverclockerCard) {
                    if (!this.moveItemStackTo(stack, 0, overclockSlotEnd, false)) {
                        if (index < playerInvStart) {
                            if (!this.moveItemStackTo(stack, playerInvStart, playerInvEnd, true)) return ItemStack.EMPTY;
                        } else if (cardHolderSlots > 0) {
                            if (!this.moveItemStackTo(stack, cardHolderStart, cardHolderEnd, false)) return ItemStack.EMPTY;
                        }
                    }
                } else {
                    if (index < playerInvStart) {
                        if (!this.moveItemStackTo(stack, playerInvStart, playerInvEnd, true)) return ItemStack.EMPTY;
                    } else if (cardHolderSlots > 0) {
                        if (!this.moveItemStackTo(stack, cardHolderStart, cardHolderEnd, false)) return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stack);
        }
        return itemStack;
    }

    protected int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
            if (handler instanceof CardItemHandler && index == 0)
                addSlot(new CardOverclockSlot(handler, index, x, y));
            else if (handler instanceof CardHolderItemStackHandler)
                addSlot(new CardHolderSlot(handler, index, x, y));
            else
                addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    protected int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0; j < verAmount; j++) {
            index = addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }

    protected void layoutPlayerInventorySlots(int leftCol, int topRow) {
        addSlotBox(playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);
        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }

    @Override
    public void removed(Player playerIn) {
        Level world = playerIn.level();
        if (!world.isClientSide) {
            if (!sourceContainer.equals(BlockPos.ZERO)) {
                BlockEntity blockEntity = world.getBlockEntity(sourceContainer);
                if (blockEntity instanceof LaserNodeBE)
                    ((LaserNodeBE) blockEntity).updateThisNode();
            }
        }
        super.removed(playerIn);
    }
}