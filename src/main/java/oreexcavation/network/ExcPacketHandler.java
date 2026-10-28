package oreexcavation.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.network.CustomPayloadEvent;
import oreexcavation.client.ExcavationClient;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.handlers.MiningScheduler;
import oreexcavation.shapes.ExcavateShape;

public class ExcPacketHandler {

    public static void handle(PacketExcavation msg, CustomPayloadEvent.Context ctx) {
        if (ctx.isServerSide()) {
            acceptServer(msg, ctx);
        } else {
            ExcavationClient.acceptClient(msg);
        }
        ctx.setPacketHandled(true);
    }

    private static void acceptServer(PacketExcavation msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;
        CompoundTag tags = msg.getTags();

        if (tags.getBooleanOr("cancel", false)) {
            MiningScheduler.INSTANCE.stopMining(player);
            return;
        }

        BlockPos pos = BlockPos.of(tags.getLongOr("pos", 0L));
        BlockState state = Block.stateById(tags.getIntOr("stateId", 0));
        if (state.isAir()) {
            OreExcavation.logger.warn("Recieved invalid block ID");
            return;
        }
        Direction side = Direction.from3DDataValue(tags.getIntOr("side", 0));
        Direction facing = Direction.from3DDataValue(tags.getIntOr("facing", 0));

        ExcavateShape shape = null;
        if (tags.contains("shape")) {
            if (!ExcavationSettings.allowShapes) {
                player.sendSystemMessage(Component.translatable("oreexcavation.key.shape_disabled"), true);
                return;
            }
            shape = new ExcavateShape();
            shape.readFromNBT(tags.getCompoundOrEmpty("shape"));
        }
        MiningScheduler.INSTANCE.startMining(player, pos, state, shape, side, facing);
    }
}
