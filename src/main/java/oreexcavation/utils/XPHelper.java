package oreexcavation.utils;

import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.stream.IntStream;

public class XPHelper {
    private static final long[] QUICK_XP = new long[2147];

    static {
        IntStream.range(0, QUICK_XP.length).forEach(i -> QUICK_XP[i] = getLevelXP(i * 1000000));
    }

    public static void addXP(Player player, long xp) {
        addXP(player, xp, true);
    }

    public static void addXP(Player player, long xp, boolean sync) {
        long total = getPlayerXP(player) + xp;
        player.totalExperience = total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        player.experienceLevel = getXPLevel(total);
        long levelXp = getLevelXP(player.experienceLevel);
        player.experienceProgress = (float) ((double) (total - levelXp) / xpBarCap(player));
        if (player.experienceProgress < 0.0F) player.experienceProgress = 0.0F;
        if (sync && player instanceof ServerPlayer sp) syncXP(sp);
    }

    public static void syncXP(ServerPlayer player) {
        player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
    }

    public static long getPlayerXP(Player player) {
        return getLevelXP(player.experienceLevel) + (long) (xpBarCap(player) * Math.max(0.0, player.experienceProgress));
    }

    public static long xpBarCap(Player player) {
        int lvl = player.experienceLevel;
        if (lvl < 16) return (long) (2.0 * lvl + 7.0);
        return lvl < 31 ? (long) (5.0 * lvl - 38.0) : (long) (9.0 * lvl - 158.0);
    }

    public static int getXPLevel(long xp) {
        if (xp <= 0L) return 0;
        int i = 0;
        while (i < QUICK_XP.length && QUICK_XP[i] <= xp) i++;
        if (i > 0) i = (i - 1) * 1000000;
        while (i < Integer.MAX_VALUE && getLevelXP(i) <= xp) i++;
        return i - 1;
    }

    public static long getLevelXP(int level) {
        if (level <= 0) return 0L;
        if (level < 17) return (long) (Math.pow(level, 2.0) + level * 6.0);
        return level < 32 ? (long) (2.5 * Math.pow(level, 2.0) - level * 40.5 + 360.0) : (long) (4.5 * Math.pow(level, 2.0) - level * 162.5 + 2220.0);
    }
}
