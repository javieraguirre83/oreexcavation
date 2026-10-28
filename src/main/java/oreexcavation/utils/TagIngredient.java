package oreexcavation.utils;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Matches items or blocks by tag name (e.g. "minecraft:logs"). */
public class TagIngredient {
    private final TagKey<Item> itemTag;
    private final TagKey<Block> blockTag;

    public TagIngredient(String tagName) {
        Identifier id = tagName == null || tagName.isBlank() ? null : Identifier.tryParse(tagName);
        this.itemTag = id == null ? null : TagKey.create(Registries.ITEM, id);
        this.blockTag = id == null ? null : TagKey.create(Registries.BLOCK, id);
    }

    public boolean apply(BlockState state) {
        if (state == null || state.isAir()) return true;
        return blockTag != null && state.is(blockTag);
    }

    public boolean apply(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        return itemTag != null && stack.is(itemTag);
    }
}
