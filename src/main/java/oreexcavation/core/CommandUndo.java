package oreexcavation.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import oreexcavation.handlers.MiningScheduler;
import oreexcavation.undo.RestoreResult;

public class CommandUndo {
    private static final SimpleCommandExceptionType ERR_FAIL_DISABLED = new SimpleCommandExceptionType(Component.translatable("oreexcavation.undo.failed.disabled"));
    private static final SimpleCommandExceptionType ERR_FAIL_XP = new SimpleCommandExceptionType(Component.translatable("oreexcavation.undo.failed.xp"));
    private static final SimpleCommandExceptionType ERR_FAIL_ITEMS = new SimpleCommandExceptionType(Component.translatable("oreexcavation.undo.failed.items"));
    private static final SimpleCommandExceptionType ERR_FAIL_HISTORY = new SimpleCommandExceptionType(Component.translatable("oreexcavation.undo.failed.no_history"));
    private static final SimpleCommandExceptionType ERR_FAIL_BLOCKED = new SimpleCommandExceptionType(Component.translatable("oreexcavation.undo.failed.obstructed"));
    private static final Component TXT_SUCCESS = Component.translatable("oreexcavation.undo.success");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("undo_excavation")
                .requires(Commands.hasPermission(Commands.LEVEL_ALL))
                .executes(CommandUndo::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (ExcavationSettings.maxUndos <= 0) throw ERR_FAIL_DISABLED.create();
        RestoreResult result = MiningScheduler.INSTANCE.attemptUndo(ctx.getSource().getPlayerOrException());
        switch (result) {
            case INVALID_XP -> throw ERR_FAIL_XP.create();
            case INVALID_ITEMS -> throw ERR_FAIL_ITEMS.create();
            case NO_UNDO_HISTORY -> throw ERR_FAIL_HISTORY.create();
            case OBSTRUCTED -> throw ERR_FAIL_BLOCKED.create();
            case SUCCESS -> ctx.getSource().sendSuccess(() -> TXT_SUCCESS, true);
        }
        return 1;
    }
}
