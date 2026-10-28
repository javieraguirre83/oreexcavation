package oreexcavation.utils;

import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/** An item stack that can hold more than 64 items (used to merge excavation drops). */
public class BigItemStack {
    public int stackSize;
    private final ItemStack baseStack;

    public BigItemStack(ItemStack stack) {
        baseStack = stack.copy();
        stackSize = baseStack.getCount();
        baseStack.setCount(1);
    }

    public ItemStack getBaseStack() {
        return baseStack;
    }

    public Set<ItemStack> getCombinedStacks() {
        int remaining = Math.max(1, stackSize);
        int max = baseStack.getMaxStackSize();
        Set<ItemStack> out = new HashSet<>(remaining / max + (remaining % max > 0 ? 1 : 0));
        while (remaining > 0) {
            ItemStack copy = baseStack.copy();
            copy.setCount(Math.min(remaining, max));
            out.add(copy);
            remaining -= max;
        }
        return out;
    }

    public BigItemStack copy() {
        BigItemStack copy = new BigItemStack(baseStack.copy());
        copy.stackSize = stackSize;
        return copy;
    }

    public boolean isItemEqual(ItemStack stack) {
        if (stack == baseStack) return true;
        if (baseStack == null || stack == null) return false;
        return ItemStack.isSameItemSameComponents(baseStack, stack);
    }
}
