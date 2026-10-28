package oreexcavation.handlers;

import com.google.common.base.Stopwatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.undo.ExcavateHistory;
import oreexcavation.undo.RestoreResult;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MiningScheduler {
    public static final MiningScheduler INSTANCE = new MiningScheduler();

    private final ArrayDeque<MiningAgent> agents = new ArrayDeque<>();
    private final Map<UUID, ExcavateHistory> undoing = new HashMap<>();
    private final Map<UUID, ArrayDeque<ExcavateHistory>> undoHistory = new HashMap<>();
    private final Stopwatch timer = Stopwatch.createStarted();

    private MiningScheduler() {
    }

    public MiningAgent getActiveAgent(UUID uuid) {
        for (MiningAgent agent : agents) {
            if (agent.getPlayerID().equals(uuid)) return agent;
        }
        return null;
    }

    public void stopMining(ServerPlayer player) {
        stopMining(getActiveAgent(player.getUUID()));
    }

    public void stopMining(MiningAgent agent) {
        if (agent == null) return;
        agent.dropEverything();
        appendHistory(agent.getPlayerID(), agent.getHistory());
        agents.remove(agent);
    }

    public MiningAgent startMining(ServerPlayer player, BlockPos pos, BlockState state, ExcavateShape shape, Direction side, Direction facing) {
        MiningAgent existing = getActiveAgent(player.getUUID());
        if (existing != null) {
            existing.appendBlock(pos);
            return existing;
        }
        MiningAgent agent = new MiningAgent(player, pos, state);
        if (shape != null) agent.setShape(shape, side, facing);
        agents.add(agent);
        agent.init();
        return agent;
    }

    public RestoreResult attemptUndo(Player player) {
        if (player == null) return RestoreResult.NO_UNDO_HISTORY;
        ArrayDeque<ExcavateHistory> list = undoHistory.get(player.getUUID());
        if (list == null || list.isEmpty()) return RestoreResult.NO_UNDO_HISTORY;
        RestoreResult result = list.peek().canRestore(player.level().getServer(), player);
        if (result == RestoreResult.SUCCESS) {
            undoing.put(player.getUUID(), list.poll());
        }
        return result;
    }

    public void appendHistory(UUID uuid, ExcavateHistory history) {
        if (uuid == null || history == null) return;
        ArrayDeque<ExcavateHistory> list = undoHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        list.push(history);
        while (list.size() > ExcavationSettings.maxUndos) list.removeLast();
    }

    public void tickAgents(MinecraftServer server) {
        timer.reset();
        timer.start();

        for (int i = 0, n = agents.size(); i < n && !agents.isEmpty(); i++) {
            if (ExcavationSettings.tpsGuard && timer.elapsed(TimeUnit.MILLISECONDS) > 40L) {
                EventHandler.skipNext = true;
                break;
            }
            MiningAgent agent = agents.poll();
            if (agent == null) continue;
            EventHandler.captureAgent = agent;
            boolean done = agent.tickMiner(timer);
            EventHandler.captureAgent = null;
            if (!done) {
                agents.addLast(agent);
            } else {
                agent.dropEverything();
                appendHistory(agent.getPlayerID(), agent.getHistory());
            }
        }

        Iterator<Map.Entry<UUID, ExcavateHistory>> it = undoing.entrySet().iterator();
        while (it.hasNext()) {
            if (ExcavationSettings.tpsGuard && timer.elapsed(TimeUnit.MILLISECONDS) > 40L) {
                EventHandler.skipNext = true;
                break;
            }
            Map.Entry<UUID, ExcavateHistory> entry = it.next();
            if (entry.getValue().tickRestore(server, server.getPlayerList().getPlayer(entry.getKey()))) {
                it.remove();
            }
        }
        timer.stop();
    }

    public void resetAll() {
        agents.clear();
        undoing.clear();
        undoHistory.clear();
        timer.reset();
    }
}
