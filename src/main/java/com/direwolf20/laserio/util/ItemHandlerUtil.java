package com.direwolf20.laserio.util;

import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardEnergy;
import com.direwolf20.laserio.common.items.cards.CardRedstone;
import com.direwolf20.laserio.common.items.filters.BaseFilter;
import com.direwolf20.laserio.common.items.filters.FilterCount;
import com.direwolf20.laserio.common.items.upgrades.OverclockerCard;
import com.google.common.collect.ArrayListMultimap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ItemHandlerUtil {
    public record ExtractResult(ItemStack itemStack, int slot) {
    }

    @Nonnull
    public static ExtractResult extractItem(IItemHandler source, @Nonnull ItemStack incstack, boolean simulate, boolean isCompareNBT) {
        return extractItem(source, incstack, incstack.getCount(), simulate, isCompareNBT);
    }

    @Nonnull
    public static ExtractResult extractItemOnce(IItemHandler source, @Nonnull ItemStack incstack, int amount, boolean simulate, boolean isCompareNBT) {
        if (source == null || incstack.isEmpty())
            return new ExtractResult(incstack, -1);

        ItemStackKey key = new ItemStackKey(incstack, isCompareNBT);
        ItemStack tempStack = ItemStack.EMPTY;
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack stackInSlot = source.getStackInSlot(i);
            if (key.equals(new ItemStackKey(stackInSlot, isCompareNBT))) {
                int extractAmt = Math.min(amount, stackInSlot.getCount());
                tempStack = source.extractItem(i, extractAmt, simulate);
                return new ExtractResult(tempStack, i);
            }
        }
        return new ExtractResult(tempStack, -1);
    }

    @Nonnull
    public static ExtractResult extractItem(IItemHandler source, @Nonnull ItemStack incstack, int amount, boolean simulate, boolean isCompareNBT) {
        if (source == null || incstack.isEmpty())
            return new ExtractResult(incstack, -1);

        ItemStackKey key = new ItemStackKey(incstack, isCompareNBT);
        ItemStack tempStack = ItemStack.EMPTY;
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack stackInSlot = source.getStackInSlot(i);
            if (key.equals(new ItemStackKey(stackInSlot, isCompareNBT))) {
                int extractAmt = Math.min(amount, stackInSlot.getCount());
                if (tempStack.isEmpty())
                    tempStack = source.extractItem(i, extractAmt, simulate);
                else if (ItemStack.isSameItemSameComponents(tempStack, stackInSlot))
                    tempStack.grow(source.extractItem(i, extractAmt, simulate).getCount());
                else
                    return new ExtractResult(tempStack, i);
                if (tempStack.isEmpty()) continue;
                amount -= extractAmt;
                if (amount == 0)
                    return new ExtractResult(tempStack, i);
            }
        }
        return new ExtractResult(tempStack, -1);
    }

    @Nonnull
    public static ExtractResult extractItemBackwards(IItemHandler source, @Nonnull ItemStack incstack, int amount, boolean simulate, boolean isCompareNBT) {
        if (source == null || incstack.isEmpty())
            return new ExtractResult(incstack, -1);

        ItemStackKey key = new ItemStackKey(incstack, isCompareNBT);
        ItemStack tempStack = ItemStack.EMPTY;
        for (int i = source.getSlots() - 1; i >= 0; i--) {
            ItemStack stackInSlot = source.getStackInSlot(i);
            if (key.equals(new ItemStackKey(stackInSlot, isCompareNBT))) {
                int extractAmt = Math.min(amount, stackInSlot.getCount());
                if (tempStack.isEmpty())
                    tempStack = source.extractItem(i, extractAmt, simulate);
                else if (ItemStack.isSameItemSameComponents(tempStack, stackInSlot))
                    tempStack.grow(source.extractItem(i, extractAmt, simulate).getCount());
                else
                    return new ExtractResult(tempStack, i);
                if (tempStack.isEmpty()) continue;
                amount -= extractAmt;
                if (amount == 0)
                    return new ExtractResult(tempStack, i);
            }
        }
        return new ExtractResult(tempStack, -1);
    }

    @Nonnull
    public static TransferResult extractItemWithSlots(LaserNodeBE be, IItemHandler source, @Nonnull ItemStack incstack, int amount, boolean simulate, boolean isCompareNBT, BaseCardCache cardCache) {
        TransferResult extractResults = new TransferResult();
        if (source == null || incstack.isEmpty()) {
            return extractResults;
        }
        int amtRemaining = amount;
        ItemStack remainingStack = incstack.copy();
        ItemStackKey key = new ItemStackKey(incstack, isCompareNBT);
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack stackInSlot = source.getStackInSlot(i);
            if (key.equals(new ItemStackKey(stackInSlot, isCompareNBT))) {
                int extractAmt = Math.min(amtRemaining, stackInSlot.getCount());
                ItemStack extractStack = source.extractItem(i, extractAmt, simulate);
                if (extractStack.isEmpty())
                    continue;
                amtRemaining -= extractAmt;
                extractResults.addResult(new TransferResult.Result(source, i, cardCache, extractStack, be, true));
                remainingStack.setCount(amtRemaining);
                if (amtRemaining == 0)
                    return extractResults;
            }
        }
        extractResults.addRemainingStack(remainingStack);
        return extractResults;
    }

    @Nonnull
    public static TransferResult extractItemWithSlotsBackwards(LaserNodeBE be, IItemHandler source, @Nonnull ItemStack incstack, int amount, boolean simulate, boolean isCompareNBT, ExtractorCardCache extractorCardCache) {
        TransferResult extractResults = new TransferResult();
        if (source == null || incstack.isEmpty()) {
            return extractResults;
        }
        int amtRemaining = amount;
        ItemStack remainingStack = incstack.copy();
        ItemStackKey key = new ItemStackKey(incstack, isCompareNBT);
        for (int i = source.getSlots() - 1; i >= 0; i--) {
            ItemStack stackInSlot = source.getStackInSlot(i);
            if (key.equals(new ItemStackKey(stackInSlot, isCompareNBT))) {
                int extractAmt = Math.min(amtRemaining, stackInSlot.getCount());
                ItemStack extractStack = source.extractItem(i, extractAmt, simulate);
                amtRemaining -= extractAmt;
                extractResults.addResult(new TransferResult.Result(source, i, extractorCardCache, extractStack, be, true));
                remainingStack.setCount(amtRemaining);
                if (amtRemaining == 0)
                    return extractResults;
            }
        }
        extractResults.addRemainingStack(remainingStack);
        return extractResults;
    }

    @Nonnull
    public static TransferResult insertItemWithSlots(LaserNodeBE be, IItemHandler source, @Nonnull ItemStack incstack, int startAt, boolean simulate, boolean isCompareNBT, boolean stacksFirst, InserterCardCache inserterCardCache) {
        return insertItemWithSlots(be, source, incstack, incstack.getCount(), startAt, simulate, isCompareNBT, stacksFirst, inserterCardCache);
    }

    @Nonnull
    public static TransferResult insertItemWithSlots(LaserNodeBE be, IItemHandler source, @Nonnull ItemStack incstack, int amount, int startAt, boolean simulate, boolean isCompareNBT, boolean stacksFirst, InserterCardCache inserterCardCache) {
        TransferResult insertResults = new TransferResult();
        List<Integer> emptySlots = new ArrayList<>();
        if (source == null || incstack.isEmpty()) {
            return insertResults;
        }
        int amtRemaining = amount;
        ItemStack remainingStack = incstack.copy();
        remainingStack.setCount(amtRemaining);
        if (inserterCardCache.filterCard.getItem() instanceof FilterCount) {
            int filterCount = inserterCardCache.getFilterAmt(incstack);
            if (filterCount <= 0) return insertResults;
            ItemHandlerUtil.InventoryCounts inventoryCounts = new InventoryCounts(source, inserterCardCache.isCompareNBT);
            int amtInInv = inventoryCounts.getCount(remainingStack);
            int amtNeeded = filterCount - amtInInv;
            if (amtNeeded <= 0) return insertResults;
            amtRemaining = Math.min(remainingStack.getCount(), amtNeeded);
            remainingStack.setCount(amtRemaining);
        }

        ItemStackKey key = new ItemStackKey(incstack, isCompareNBT);
        if (stacksFirst) {
            for (int i = startAt; i < source.getSlots(); i++) {
                ItemStack stackInSlot = source.getStackInSlot(i);
                if (stackInSlot.isEmpty())
                    emptySlots.add(i);
                if (key.equals(new ItemStackKey(stackInSlot, isCompareNBT))) {
                    remainingStack = source.insertItem(i, remainingStack, simulate);
                    int amtInserted = amtRemaining - remainingStack.getCount();
                    if (amtInserted <= 0) continue;
                    insertResults.addResult(new TransferResult.Result(source, i, inserterCardCache, incstack.split(amtInserted), be, false));
                    amtRemaining = remainingStack.getCount();

                    if (amtRemaining == 0)
                        return insertResults;
                }
            }
            for (Integer i : emptySlots) {
                remainingStack = source.insertItem(i, remainingStack, simulate);
                if (remainingStack.getCount() == amtRemaining)
                    continue;
                insertResults.addResult(new TransferResult.Result(source, i, inserterCardCache, incstack.split(amtRemaining - remainingStack.getCount()), be, false));
                amtRemaining = remainingStack.getCount();

                if (amtRemaining == 0)
                    return insertResults;
            }
        } else {
            for (int i = 0; i < source.getSlots(); i++) {
                remainingStack = source.insertItem(i, remainingStack, simulate);
                insertResults.addResult(new TransferResult.Result(source, i, inserterCardCache, incstack.split(amtRemaining - remainingStack.getCount()), be, false));
                amtRemaining = remainingStack.getCount();

                if (amtRemaining == 0)
                    return insertResults;
            }
        }
        insertResults.addRemainingStack(remainingStack);
        return insertResults;
    }

    public static boolean doItemsMatch(ItemStack a, ItemStack b, boolean isCompareNBT) {
        return isCompareNBT ? ItemStack.isSameItemSameComponents(a, b) : ItemStack.isSameItem(a, b);
    }

    public static boolean areItemsStackable(ItemStack toInsert, ItemStack inSlot) {
        if (toInsert.isEmpty() || inSlot.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(inSlot, toInsert);
    }

    public static ItemStack size(ItemStack stack, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(size);
    }

    public static class InventoryInfo {
        private final NonNullList<ItemStack> inventory;
        private final IntList stackSizes = new IntArrayList();

        public InventoryInfo(IItemHandler handler) {
            inventory = NonNullList.withSize(handler.getSlots(), ItemStack.EMPTY);
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                inventory.set(i, stack);
                stackSizes.add(stack.getCount());
            }
        }
    }

    public static class InventoryCounts {
        private final ArrayListMultimap<Item, ItemStack> itemMap = ArrayListMultimap.create();
        private int totalCount = 0;
        private boolean isCompareNBT;

        public InventoryCounts() {
        }

        public InventoryCounts(IItemHandler handler, boolean compareNBT) {
            isCompareNBT = compareNBT;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    setCount(stack);
                }
            }
        }

        public void addHandler(IItemHandler handler) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    setCount(stack);
                }
            }
        }

        public void addHandlerWithFilter(IItemHandler handler, BaseCardCache filterCard) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty() && filterCard.isStackValidForCard(stack)) {
                    setCount(stack);
                }
            }
        }

        public ArrayListMultimap<Item, ItemStack> getItemCounts() {
            return itemMap;
        }

        public void setCount(ItemStack stack) {
            if (stack.isEmpty()) return;
            for (ItemStack cacheStack : itemMap.get(stack.getItem())) {
                boolean sameItems = isCompareNBT ? ItemStack.isSameItemSameComponents(cacheStack, stack) : ItemStack.isSameItem(cacheStack, stack);
                if (sameItems) {
                    cacheStack.grow(stack.getCount());
                    totalCount += stack.getCount();
                    return;
                }
            }
            itemMap.put(stack.getItem(), stack.copy());
            totalCount += stack.getCount();
        }

        public ItemStack removeStack(ItemStack stack, int count) {
            ItemStack returnStack = ItemStack.EMPTY;
            for (ItemStack cacheStack : itemMap.get(stack.getItem())) {
                if (ItemStack.isSameItemSameComponents(cacheStack, stack)) {
                    returnStack = cacheStack.split(count);
                    break;
                }
            }
            if (returnStack.isEmpty()) return returnStack;

            itemMap.get(returnStack.getItem()).removeIf(o -> o.isEmpty());
            totalCount -= returnStack.getCount();
            return returnStack;
        }

        public int getCount(ItemStack stack) {
            for (ItemStack cacheStack : itemMap.get(stack.getItem())) {
                boolean sameItems = isCompareNBT ? ItemStack.isSameItemSameComponents(cacheStack, stack) : ItemStack.isSameItem(cacheStack, stack);
                if (sameItems)
                    return cacheStack.getCount();
            }
            return 0;
        }

        public int getTotalCount() {
            return totalCount;
        }
    }

    public static class InventoryCardCounts {
        private final Object2IntOpenHashMap<Item> cardCounts;
        private final Object2IntOpenHashMap<Item> cardModifierCounts;

        public InventoryCardCounts() {
            cardCounts = new Object2IntOpenHashMap<>();
            cardModifierCounts = new Object2IntOpenHashMap<>();
        }

        public InventoryCardCounts(Object2IntOpenHashMap<Item> cardCounts, Object2IntOpenHashMap<Item> cardModifierCounts) {
            this.cardCounts = cardCounts;
            this.cardModifierCounts = cardModifierCounts;
        }

        public InventoryCardCounts(IItemHandler handler, boolean deepSearch) {
            this();
            addHandler(handler, deepSearch);
        }

        public void addCardModifiersFromCard(ItemStack cardStack) {
            Item cardItem = cardStack.getItem();
            if (cardItem instanceof BaseCard && !(cardItem instanceof CardRedstone)) {
                IItemHandler cardHandler;
                if (cardItem instanceof CardEnergy) {
                    cardHandler = new com.direwolf20.laserio.common.containers.customhandler.CardItemHandler(1, cardStack);
                } else {
                    cardHandler = BaseCard.getInventory(cardStack);
                }
                addHandler(cardHandler, false, cardStack.getCount());
            }
        }

        private void addCard(ItemStack cardStack, boolean deepSearch, int containerStackCount) {
            if (cardStack.isEmpty()) return;
            Item cardItem = cardStack.getItem();
            int cardStackCount = containerStackCount * cardStack.getCount();

            if (cardItem instanceof BaseFilter || cardItem instanceof OverclockerCard) {
                cardModifierCounts.addTo(cardItem, cardStackCount);
            } else {
                cardCounts.addTo(cardItem, cardStackCount);
                if (deepSearch) {
                    addCardModifiersFromCard(cardStack);
                }
            }
        }

        public void addHandler(IItemHandler handler) {
            addHandler(handler, true);
        }

        public void addHandler(IItemHandler handler, boolean deepSearch) {
            addHandler(handler, deepSearch, 1);
        }

        private void addHandler(IItemHandler handler, boolean deepSearch, int containerStackCount) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack cardStack = handler.getStackInSlot(i);
                addCard(cardStack, deepSearch, containerStackCount);
            }
        }

        public void subtractInventoryCardCounts(InventoryCardCounts other) {
            other.cardCounts.object2IntEntrySet().forEach(e -> this.cardCounts.addTo(e.getKey(), -e.getIntValue()));
            other.cardModifierCounts.object2IntEntrySet().forEach(e -> this.cardModifierCounts.addTo(e.getKey(), -e.getIntValue()));
        }

        public void addInventoryCardCounts(InventoryCardCounts other) {
            other.cardCounts.object2IntEntrySet().forEach(e -> this.cardCounts.addTo(e.getKey(), e.getIntValue()));
            other.cardModifierCounts.object2IntEntrySet().forEach(e -> this.cardModifierCounts.addTo(e.getKey(), e.getIntValue()));
        }

        public boolean hasNegativeValues() {
            return cardCounts.values().intStream().anyMatch(i -> i < 0) ||
                    cardModifierCounts.values().intStream().anyMatch(i -> i < 0);
        }

        public Object2IntOpenHashMap<Item> getCardCounts() {
            return cardCounts;
        }

        public Object2IntOpenHashMap<Item> getCardModifierCounts() {
            return cardModifierCounts;
        }

        @Override
        public InventoryCardCounts clone() {
            InventoryCardCounts clone = new InventoryCardCounts();
            clone.cardCounts.putAll(this.cardCounts);
            clone.cardModifierCounts.putAll(this.cardModifierCounts);
            return clone;
        }
    }
}