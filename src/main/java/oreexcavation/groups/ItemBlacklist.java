package oreexcavation.groups;

import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class ItemBlacklist {
    public static final ItemBlacklist INSTANCE = new ItemBlacklist();
    private final Set<ItemEntry> banList = new HashSet<>();

    public boolean isBanned(ItemStack stack) {
        return banList.stream().anyMatch(e -> e.checkMatch(stack));
    }

    public void loadList(String[] list) {
        banList.clear();
        for (int i = list.length - 1; i >= 0; i--) {
            ItemEntry entry = ItemEntry.readFromString(list[i]);
            if (entry != null) banList.add(entry);
        }
    }
}
