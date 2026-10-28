package oreexcavation.handlers;

import com.google.common.base.Stopwatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraftforge.common.util.BlockSnapshot;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.groups.BlockEntry;
import oreexcavation.groups.BlockGroups;
import oreexcavation.overrides.ToolOverride;
import oreexcavation.overrides.ToolOverrideDefault;
import oreexcavation.overrides.ToolOverrideHandler;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.undo.BlockHistory;
import oreexcavation.undo.ExcavateHistory;
import oreexcavation.utils.BigItemStack;
import oreexcavation.utils.ToolEffectiveCheck;
import oreexcavation.utils.XPHelper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Mines a vein/shape for one player, a few blocks per tick. */
public class MiningAgent {
    private final Item origTool;
    public final Set<BlockPos> minedBlocks = new HashSet<>();
    private final Set<BlockPos> checkedBlocks = new HashSet<>();
    private final Deque<BlockPos> scheduled = new ArrayDeque<>();
    public final ServerPlayer player;
    public final BlockPos origin;
    public final UUID playerID;
    public Direction side = Direction.SOUTH;
    public Direction facing = Direction.SOUTH;
    public ExcavateShape shape = null;
    private final ExcavateHistory history;
    public final Set<BlockEntry> blockGroup = new HashSet<>();
    public final BlockState state;
    private final Block block;
    public ToolOverride toolProps = ToolOverrideDefault.DEFAULT;
    private final boolean strictSubs;
    public final NonNullList<BigItemStack> drops = NonNullList.create();
    public int experience = 0;
    public final CompoundTag auxNBT = new CompoundTag();

    public MiningAgent(ServerPlayer player, BlockPos origin, BlockState state) {
        this.player = player;
        this.origin = origin;
        this.playerID = player.getUUID();
        this.state = state;
        this.block = state.getBlock();
        this.history = new ExcavateHistory(player.level().dimension());
        this.blockGroup.addAll(BlockGroups.INSTANCE.getGroup(state));
        this.strictSubs = BlockGroups.INSTANCE.isStrict(state);
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        this.origTool = held.isEmpty() ? null : held.getItem();
        if (!held.isEmpty()) {
            ToolOverride override = ToolOverrideHandler.INSTANCE.getOverride(held);
            this.toolProps = override != null ? override : this.toolProps;
        }
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public void setOverride(ToolOverride override) {
        this.toolProps = override;
    }

    public void init() {
        appendSurrounding(origin);
    }

    public MiningAgent setShape(ExcavateShape shape, Direction side, Direction facing) {
        this.shape = shape;
        this.side = side;
        this.facing = facing;
        return this;
    }

    /** @return true when this excavation has finished */
    public boolean tickMiner(Stopwatch timer) {
        if (origin == null || player == null || !player.isAlive() || minedBlocks.size() >= toolProps.getLimit()) {
            return true;
        }
        ServerLevel level = player.level();

        int count = 0;
        while (!scheduled.isEmpty() && count++ < toolProps.getSpeed() && minedBlocks.size() < toolProps.getLimit()
                && (!ExcavationSettings.tpsGuard || timer.elapsed(TimeUnit.MILLISECONDS) <= 40L)) {
            BlockPos pos = scheduled.poll();
            if (pos == null) continue;

            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            Item heldItem = held.isEmpty() ? null : held.getItem();
            if (heldItem != origTool || !hasEnergy(player)) return true;

            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > toolProps.getRange() * toolProps.getRange()) {
                checkedBlocks.add(pos);
                continue;
            }

            BlockState current = level.getBlockState(pos);
            if (EventHandler.isBlockBlacklisted(current) || current.is(BlockTags.FIRE)) {
                checkedBlocks.add(pos);
                continue;
            }

            boolean match = strictSubs ? current == state : current.getBlock() == block;
            match = match || BlockGroups.INSTANCE.quickCheck(blockGroup, current);
            if (!match) {
                checkedBlocks.add(pos);
                continue;
            }

            CompoundTag tileData = null;
            if (ExcavationSettings.maxUndos > 0) {
                level.captureBlockSnapshots = true;
                level.capturedBlockSnapshots.clear();
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) tileData = BlockHistory.saveBlockEntity(be);
            }

            if (!ExcavationSettings.ignoreTools && !ToolEffectiveCheck.canHarvestBlock(level, current, pos, player)) {
                level.captureBlockSnapshots = false;
                checkedBlocks.add(pos);
                continue;
            }

            if (!player.gameMode.destroyBlock(pos) && !level.getBlockState(pos).isAir()) {
                OreExcavation.logger.warn("Block harvest failed unexpectedly.\nBlock: " + current + "\nTool: " + held + "\nPos: " + pos);
            } else {
                if (ExcavationSettings.maxUndos > 0) {
                    level.captureBlockSnapshots = false;
                    while (!level.capturedBlockSnapshots.isEmpty()) {
                        BlockSnapshot snap = level.capturedBlockSnapshots.get(0);
                        if (pos.equals(snap.getPos())) {
                            history.addRecordedBlock(new BlockHistory(snap, tileData));
                        } else {
                            history.addRecordedBlock(new BlockHistory(snap));
                        }
                        level.capturedBlockSnapshots.remove(0);
                        level.markAndNotifyBlock(snap.getPos(), level.getChunkAt(snap.getPos()),
                                snap.getReplacedBlock(), snap.getCurrentBlock(), snap.getFlag(), 512);
                    }
                }
                if (!player.isCreative()) {
                    player.getFoodData().addExhaustion(toolProps.getExhaustion());
                    if (toolProps.getExperience() > 0) {
                        XPHelper.addXP(player, -toolProps.getExperience(), false);
                    }
                }
                appendSurrounding(pos);
                minedBlocks.add(pos);
            }
            level.capturedBlockSnapshots.clear();
            level.captureBlockSnapshots = false;
            checkedBlocks.add(pos);
        }

        if (!player.isCreative()) XPHelper.syncXP(player);
        return scheduled.isEmpty() || minedBlocks.size() >= toolProps.getLimit();
    }

    private void appendSurrounding(BlockPos pos) {
        for (int i = 0; i < 27; i++) {
            if (i != 13) appendBlock(pos.offset(i % 3 - 1, i % 9 / 3 - 1, i / 9 - 1));
        }
    }

    public void appendBlock(BlockPos pos) {
        if (pos == null || checkedBlocks.contains(pos) || scheduled.contains(pos)) return;
        if (origin.distToCenterSqr(pos.getX(), pos.getY(), pos.getZ()) > toolProps.getRange() * toolProps.getRange()) return;
        if (!player.level().getWorldBorder().isWithinBounds(pos) || !canDestroy(player, pos)) return;
        if (shape == null || shape.isValid(origin, pos, side, facing)) {
            scheduled.add(pos);
        }
    }

    private boolean canDestroy(Player player, BlockPos pos) {
        if (player.getAbilities().mayBuild) return true;
        BlockState state = player.level().getBlockState(pos);
        ItemStack held = player.getMainHandItem();
        return !held.isEmpty() && !state.isAir() && held.canBreakBlockInAdventureMode(new BlockInWorld(player.level(), pos, false));
    }

    private boolean hasEnergy(ServerPlayer player) {
        if (toolProps.getExhaustion() > 0.0F && player.getFoodData().getFoodLevel() <= 0) return false;
        return !(toolProps.getExperience() > 0 && XPHelper.getPlayerXP(player) < toolProps.getExperience());
    }

    public void dropEverything() {
        MiningAgent prev = EventHandler.captureAgent;
        EventHandler.captureAgent = null;
        history.setRecievedStacks(drops);
        history.setRecievedXP(experience);
        BlockPos at = ExcavationSettings.autoPickup ? player.getOnPos() : origin;
        ServerLevel level = player.level();
        drops.forEach(big -> big.getCombinedStacks().forEach(stack ->
                level.addFreshEntity(new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, stack))));
        if (experience > 0) {
            level.addFreshEntity(new ExperienceOrb(level, at.getX(), at.getY(), at.getZ(), experience));
        }
        drops.clear();
        experience = 0;
        EventHandler.captureAgent = prev;
    }

    public void addItemDrop(ItemStack stack) {
        BigItemStack existing = drops.stream().filter(b -> b.isItemEqual(stack)).findFirst().orElse(null);
        if (existing != null) {
            existing.stackSize += stack.getCount();
        } else {
            drops.add(new BigItemStack(stack));
        }
    }

    public void addExperience(int xp) {
        experience += xp;
    }

    public boolean hasMinedPosition(BlockPos pos) {
        return checkedBlocks.contains(pos);
    }

    public ExcavateHistory getHistory() {
        return history;
    }
}
