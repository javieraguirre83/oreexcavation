package oreexcavation.groups;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import oreexcavation.core.OreExcavation;
import oreexcavation.utils.TagIngredient;

public class ItemEntry {
    public final boolean isWildcard;
    public final Identifier idName;
    public final TagIngredient tagIng;

    public ItemEntry() {
        idName = null;
        tagIng = null;
        isWildcard = true;
    }

    public ItemEntry(Identifier id) {
        idName = id;
        tagIng = null;
        isWildcard = false;
    }

    public ItemEntry(String tag) {
        tagIng = tag == null || tag.isEmpty() ? null : new TagIngredient(tag);
        idName = null;
        isWildcard = false;
    }

    public boolean checkMatch(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (isWildcard) return true;
        if (tagIng != null && tagIng.apply(stack)) return true;
        return idName != null && idName.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static ItemEntry readFromString(String text) {
        if (text == null || text.isEmpty()) return null;
        if (text.equalsIgnoreCase("*")) return new ItemEntry();
        String[] split = text.split(":");
        if (split.length <= 1 || split.length > 3) {
            OreExcavation.logger.warn("Invalid Item Entry format: " + text);
            return null;
        }
        if (split.length == 2) return new ItemEntry(Identifier.fromNamespaceAndPath(split[0], split[1]));
        if (text.startsWith("tag:")) return new ItemEntry(text.replaceFirst("tag:", ""));
        OreExcavation.logger.warn("Invalid Item Entry format: " + text);
        return null;
    }
}
