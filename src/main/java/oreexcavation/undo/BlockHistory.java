package oreexcavation.undo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import oreexcavation.core.OreExcavation;

public class BlockHistory {
    public final BlockPos pos;
    public final BlockState state;
    public final CompoundTag tileData;

    public BlockHistory(BlockPos pos, BlockState state, CompoundTag tileData) {
        this.pos = pos;
        this.state = state;
        this.tileData = tileData;
    }

    public static CompoundTag saveBlockEntity(BlockEntity be) {
        return be.saveWithoutMetadata(be.getLevel().registryAccess());
    }

    public void restoreBlock(Level level) {
        level.setBlock(pos, state, 2);
        if (tileData != null && !tileData.isEmpty()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tileData));
            }
        }
    }
}
