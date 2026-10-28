package oreexcavation.undo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraftforge.common.util.BlockSnapshot;
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

    public BlockHistory(BlockSnapshot snapshot) {
        this.pos = snapshot.getPos();
        this.state = snapshot.getReplacedBlock();
        BlockEntity be = snapshot.getBlockEntity();
        if (be == null) {
            this.tileData = null;
        } else {
            CompoundTag tag;
            try {
                tag = saveBlockEntity(be);
            } catch (Exception e) {
                OreExcavation.logger.error("Unable to save undo state for tile entity: " + be, e);
                tag = new CompoundTag();
            }
            this.tileData = tag;
        }
    }

    public BlockHistory(BlockSnapshot snapshot, CompoundTag tileData) {
        this.pos = snapshot.getPos();
        this.state = snapshot.getReplacedBlock();
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
