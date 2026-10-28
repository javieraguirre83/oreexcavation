package oreexcavation.groups;

import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class BlockBlacklist {
    public static final BlockBlacklist INSTANCE = new BlockBlacklist();
    private final Set<BlockEntry> banList = new HashSet<>();

    public boolean isBanned(BlockState state) {
        return banList.stream().anyMatch(e -> e.checkMatch(state));
    }

    public void loadList(String[] list) {
        banList.clear();
        for (int i = list.length - 1; i >= 0; i--) {
            BlockEntry entry = BlockEntry.readFromString(list[i]);
            if (entry != null) banList.add(entry);
        }
    }
}
