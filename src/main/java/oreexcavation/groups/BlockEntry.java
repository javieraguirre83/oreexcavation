package oreexcavation.groups;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import oreexcavation.core.OreExcavation;
import oreexcavation.utils.TagIngredient;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

public class BlockEntry {
    public final Predicate<BlockState> validator;

    public BlockEntry() {
        validator = state -> true;
    }

    public BlockEntry(@Nonnull Identifier id) {
        validator = state -> id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public BlockEntry(@Nonnull BlockState ref, Collection<Property<?>> props, boolean matchBlock) {
        validator = state -> {
            if (matchBlock && state.getBlock() != ref.getBlock()) return false;
            for (Property<?> prop : props) {
                if (!state.hasProperty(prop) || !Objects.equals(state.getValue(prop), ref.getValue(prop))) return false;
            }
            return true;
        };
    }

    public BlockEntry(@Nonnull String tagName) {
        TagIngredient tag = new TagIngredient(tagName);
        validator = tag::apply;
    }

    public boolean checkMatch(BlockState state) {
        return state != null && !state.isAir() && validator.test(state);
    }

    public static BlockEntry readFromString(String text) {
        if (text == null || text.isBlank()) return null;
        if (text.equalsIgnoreCase("*")) return new BlockEntry();

        String[] split = text.split(":");
        if (split.length <= 1 || split.length > 3) {
            OreExcavation.logger.warn("Invalid Block Entry format: " + text);
            return null;
        }
        if (split.length == 2) {
            return new BlockEntry(Identifier.fromNamespaceAndPath(split[0], split[1]));
        }
        if (text.startsWith("tag:")) {
            String tag = text.substring(4);
            if (tag.isBlank()) {
                OreExcavation.logger.error("Unable to parse tag name for Block Entry: " + text);
                return null;
            }
            return new BlockEntry(tag);
        } else if (text.startsWith("state:")) {
            try {
                BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, new StringReader(text.substring(6)), false);
                return new BlockEntry(result.blockState(), result.properties().keySet(), true);
            } catch (Exception e) {
                OreExcavation.logger.error("Unable to parse block state for Block Entry: " + text, e);
                return null;
            }
        } else if (text.startsWith("property:")) {
            try {
                BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, new StringReader(text.substring(9)), false);
                return new BlockEntry(result.blockState(), result.properties().keySet(), false);
            } catch (Exception e) {
                OreExcavation.logger.error("Unable to parse block properties for Block Entry: " + text, e);
                return null;
            }
        }
        OreExcavation.logger.warn("Invalid Block Entry format: " + text);
        return null;
    }
}
