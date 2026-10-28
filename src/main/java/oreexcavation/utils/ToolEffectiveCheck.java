package oreexcavation.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import oreexcavation.core.ExcavationSettings;

public class ToolEffectiveCheck {
    public static boolean canHarvestBlock(BlockGetter level, BlockState state, BlockPos pos, Player player) {
        if (level == null || state == null || pos == null || player == null) return false;
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (ExcavationSettings.openHand && held.isEmpty()) {
            return player.isCreative() || state.canHarvestBlock(level, pos, player);
        }

        boolean shears = !held.isEmpty() && held.getItem() instanceof ShearsItem && held.getDestroySpeed(state) > 1.0F;

        if (ExcavationSettings.toolClass) {
            if (held.isEmpty()) return false;
            if (shears) return true;
            boolean digger = held.is(ItemTags.PICKAXES) || held.is(ItemTags.AXES) || held.is(ItemTags.SHOVELS) || held.is(ItemTags.HOES);
            return digger && held.isCorrectToolForDrops(state);
        } else if (ExcavationSettings.altTools && !held.isEmpty() && held.getDestroySpeed(state) > 1.0F) {
            return true;
        }
        return shears || state.canHarvestBlock(level, pos, player);
    }
}
