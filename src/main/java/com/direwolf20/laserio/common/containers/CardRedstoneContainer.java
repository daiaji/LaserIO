package com.direwolf20.laserio.common.containers;

import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.blocks.LaserNode;
import com.direwolf20.laserio.common.items.CardHolder;
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

public class CardRedstoneContainer extends AbstractContainerMenu {
    public static final int SLOTS = 0; // 红石卡没有内部槽位

    public Player playerEntity;
    private IItemHandler playerInventory;
    public ItemStack cardItem;
    // 卡包相关
    public ItemStack cardHolder;
    public IItemHandler cardHolderHandler;
    public UUID cardHolderUUID;
    
    public BlockPos sourceContainer = BlockPos.ZERO;
    public byte direction = -1;

    protected CardRedstoneContainer(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }

    public CardRedstoneContainer(int windowId, Inventory playerInventory, Player player, RegistryFriendlyByteBuf extraData) {
        this(windowId, playerInventory, player, ItemStack.OPTIONAL_STREAM_CODEC.decode(extraData));
        this.direction = extraData.readByte();
    }

    public CardRedstoneContainer(int windowId, Inventory playerInventory, Player player, ItemStack cardItem) {
        super(Registration.CardRedstone_Container.get(), windowId);
        playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);
        this.cardItem = cardItem;
        
        // 初始化卡包
        this.cardHolder = LaserNode.findFirstCardHolder(player);
        if (!cardHolder.isEmpty()) {
            this.cardHolderHandler = new CardHolderItemStackHandler(CardHolderContainer.SLOTS, cardHolder);
            // 绘制卡包槽位 (侧边栏)
            addSlotBox(cardHolderHandler, 0, -92, 32, 5, 18, 3, 18);
            cardHolderUUID = CardHolder.getUUID(cardHolder);
        }

        layoutPlayerInventorySlots(8, 84);
    }

    public CardRedstoneContainer(int windowId, Inventory playerInventory, Player player, BlockPos sourcePos, ItemStack cardItem, byte direction) {
        this(windowId, playerInventory, player, cardItem);
        this.sourceContainer = sourcePos;
        this.direction = direction;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        // 安全检查：如果卡包被扔掉了，关闭界面
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
    public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
        // 简单的防呆检查，防止操作异常槽位
        if (slotId >= 0 && slotId < this.slots.size()) {
             // 可以在这里添加针对 CardHolderSlot 的额外检查
        }
        super.clicked(slotId, dragType, clickTypeIn, player);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            
            // 红石卡没有内部槽位，所以只有 卡包 <-> 玩家背包 之间的移动
            
            int cardHolderSlots = (!cardHolder.isEmpty()) ? CardHolderContainer.SLOTS : 0;

            if (index < cardHolderSlots) {
                // 1. 从 卡包 -> 玩家背包
                if (!this.moveItemStackTo(stack, cardHolderSlots, 36 + cardHolderSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 2. 从 玩家背包 -> 卡包 (如果存在)
                if (cardHolderSlots > 0) {
                    if (!this.moveItemStackTo(stack, 0, cardHolderSlots, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stack);
        }
        return itemstack;
    }

    // 辅助方法：添加槽位
    protected int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
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