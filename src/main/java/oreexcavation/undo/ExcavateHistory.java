package oreexcavation.undo;

import com.google.common.base.Stopwatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.utils.BigItemStack;
import oreexcavation.utils.XPHelper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class ExcavateHistory {
    private final Deque<BlockHistory> history = new ArrayDeque<>();
    private final NonNullList<BigItemStack> drops = NonNullList.create();
    private long experience = 0L;
    private final Stopwatch timer = Stopwatch.createUnstarted();
    private final ResourceKey<Level> dimension;

    public ExcavateHistory(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public BlockHistory recordPosition(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        CompoundTag tag = be == null ? null : BlockHistory.saveBlockEntity(be);
        return new BlockHistory(pos, state, tag);
    }

    public void addRecordedBlock(BlockHistory block) {
        if (!block.state.isAir() && !history.contains(block)) {
            history.push(block);
        }
    }

    public void setRecievedStacks(List<BigItemStack> stacks) {
        drops.clear();
        stacks.forEach(s -> drops.add(s.copy()));
    }

    public void setRecievedXP(long xp) {
        experience = xp;
    }

    public RestoreResult canRestore(MinecraftServer server, Player player) {
        if (server == null || player == null) return RestoreResult.NO_UNDO_HISTORY;
        Level level = server.getLevel(dimension);
        if (level == null) return RestoreResult.NO_UNDO_HISTORY;

        if (!player.getAbilities().instabuild) {
            if (XPHelper.getPlayerXP(player) < experience) return RestoreResult.INVALID_XP;

            int[] counts = new int[drops.size()];
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                for (int j = 0; j < counts.length; j++) {
                    if (drops.get(j).isItemEqual(stack)) {
                        counts[j] += stack.getCount();
                        break;
                    }
                }
            }
            if (IntStream.range(0, counts.length).anyMatch(j -> counts[j] < drops.get(j).stackSize)) {
                return RestoreResult.INVALID_ITEMS;
            }
        }

        boolean blocked = history.stream().anyMatch(b -> {
            BlockState current = level.getBlockState(b.pos);
            boolean free = current.isAir() || current.canBeReplaced() && ExcavationSettings.undoReplace;
            return !(free && level.getEntities(null, new AABB(b.pos)).isEmpty());
        });
        return blocked ? RestoreResult.OBSTRUCTED : RestoreResult.SUCCESS;
    }

    public boolean tickRestore(MinecraftServer server, Player player) {
        Level level = server.getLevel(dimension);
        if (level == null) return true;
        timer.reset();
        timer.start();

        if (player != null && !player.getAbilities().instabuild) {
            XPHelper.addXP(player, -experience, true);
            if (!player.getInventory().isEmpty()) {
                for (BigItemStack big : drops) {
                    for (int i = 0; i < player.getInventory().getContainerSize() && big.stackSize > 0; i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty() && big.isItemEqual(stack)) {
                            int n = Math.min(big.stackSize, stack.getCount());
                            player.getInventory().removeItem(i, n);
                            big.stackSize -= n;
                        }
                    }
                }
            }
            drops.clear();
        }

        while (!history.isEmpty() && (!ExcavationSettings.tpsGuard || timer.elapsed(TimeUnit.MILLISECONDS) <= 40L)) {
            history.pop().restoreBlock(level);
        }
        timer.stop();
        return history.isEmpty();
    }
}
