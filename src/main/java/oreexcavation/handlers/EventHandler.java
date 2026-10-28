package oreexcavation.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.groups.BlockBlacklist;
import oreexcavation.groups.ItemBlacklist;
import oreexcavation.network.PacketExcavation;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.utils.ToolEffectiveCheck;

/** Server side events (block break detection, drop capture, ticking the miners). */
public class EventHandler {
    public static MiningAgent captureAgent;
    public static boolean skipNext = false;
    public static boolean isExcavating = false;

    public static void register() {
        EntityJoinLevelEvent.BUS.addListener(Priority.LOWEST, EventHandler::onEntitySpawn);
        BlockEvent.BreakEvent.BUS.addListener(Priority.LOWEST, EventHandler::onBlockBreak);
        TickEvent.ServerTickEvent.Post.BUS.addListener(EventHandler::onTick);
        LevelEvent.Unload.BUS.addListener(EventHandler::onWorldUnload);
    }

    /** Captures the drops of excavated blocks so they can be merged. Returns true to cancel the spawn. */
    public static boolean onEntitySpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !event.getEntity().isAlive() || captureAgent == null) return false;
        if (event.getEntity() instanceof ItemEntity item) {
            captureAgent.addItemDrop(item.getItem());
            return true;
        } else if (event.getEntity() instanceof ExperienceOrb orb) {
            captureAgent.addExperience(orb.getValue());
            return true;
        }
        return false;
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (captureAgent != null && !captureAgent.hasMinedPosition(pos)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!ExcavationSettings.openHand && player.getMainHandItem().isEmpty()) return;
        if (isToolBlacklisted(player.getMainHandItem()) || isBlockBlacklisted(state)) return;
        if (state.isAir()) return;
        if (!(event.getLevel() instanceof Level level)) return;
        if (!ExcavationSettings.ignoreTools && !ToolEffectiveCheck.canHarvestBlock(level, state, pos, player)) return;
        if (MiningScheduler.INSTANCE.getActiveAgent(player.getUUID()) != null) return;

        CompoundTag tag = new CompoundTag();
        tag.putLong("pos", pos.asLong());
        tag.putInt("stateId", Block.getId(state));
        ClipContext clip = new ClipContext(player.getEyePosition(1.0F),
                player.getEyePosition(1.0F).add(player.getLookAngle().scale(5.0)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = level.clip(clip);
        tag.putInt("side", hit.getType() == HitResult.Type.BLOCK
                ? hit.getDirection().getOpposite().get3DDataValue()
                : ExcavateShape.getFacing(player, state, pos).get3DDataValue());
        OreExcavation.instance.network.send(new PacketExcavation(tag), PacketDistributor.PLAYER.with(player));
    }

    public static void onTick(TickEvent.ServerTickEvent.Post event) {
        if (skipNext) {
            skipNext = false;
        } else {
            MiningScheduler.INSTANCE.tickAgents(event.server());
            captureAgent = null;
        }
    }

    public static void onWorldUnload(LevelEvent.Unload event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (!event.getLevel().isClientSide() && server != null && !server.isRunning()) {
            MiningScheduler.INSTANCE.resetAll();
            captureAgent = null;
        }
    }

    public static boolean isBlockBlacklisted(BlockState state) {
        if (state == null || state.isAir()) return true;
        if (state.is(BlockTags.FIRE) || state.liquid()) return true;
        return BlockBlacklist.INSTANCE.isBanned(state) != ExcavationSettings.invertBBlacklist;
    }

    public static boolean isToolBlacklisted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return ItemBlacklist.INSTANCE.isBanned(stack) != ExcavationSettings.invertTBlacklist;
    }
}
