package oreexcavation.shapes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import oreexcavation.core.OreExcavation;
import oreexcavation.utils.JsonHelper;

import javax.annotation.Nonnull;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ExcavateShape {
    private String name = "New Shape";
    private int shapeMask = 0;
    private int maxDepth = -1;
    private int revDepth = 0;
    private byte reticle = 12;
    private int stride = 0;
    private int elev = 0;
    private byte[] outline = new byte[0];

    public byte[] getOutline() {
        return outline;
    }

    public void rebuildOutline() {
        if (shapeMask == 0) {
            outline = new byte[0];
            return;
        }
        List<Integer> lines = new ArrayList<>();
        int ox = -(reticle % 5);
        int oy = -(reticle / 5);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                if ((1 << y * 5 + x & shapeMask) == 0) continue;
                byte adj = adjacentBits(x, y);
                if ((adj & 1) != 0) { lines.add(ox + x); lines.add(oy + y + 1); lines.add(ox + x + 1); lines.add(oy + y + 1); }
                if ((adj & 2) != 0) { lines.add(ox + x + 1); lines.add(oy + y + 1); lines.add(ox + x + 1); lines.add(oy + y); }
                if ((adj & 4) != 0) { lines.add(ox + x); lines.add(oy + y); lines.add(ox + x + 1); lines.add(oy + y); }
                if ((adj & 8) != 0) { lines.add(ox + x); lines.add(oy + y + 1); lines.add(ox + x); lines.add(oy + y); }
            }
        }
        byte[] out = new byte[lines.size()];
        for (int i = 0; i < out.length; i++) out[i] = lines.get(i).byteValue();
        outline = out;
    }

    private byte adjacentBits(int x, int y) {
        if (x < 0 || x >= 5 || y < 0 || y >= 5) return 0;
        byte b = 0;
        if (x == 0 || (shapeMask & 1 << y * 5 + x - 1) == 0) b |= 8;
        if (x == 4 || (shapeMask & 1 << y * 5 + x + 1) == 0) b |= 2;
        if (y == 0 || (shapeMask & 1 << (y - 1) * 5 + x) == 0) b |= 4;
        if (y == 4 || (shapeMask & 1 << (y + 1) * 5 + x) == 0) b |= 1;
        return b;
    }

    public ExcavateShape setName(String name) { this.name = name; return this; }
    public String getName() { return name; }
    public ExcavateShape setStairStride(int stride) { this.stride = stride; return this; }
    public ExcavateShape setStairElev(int elev) { this.elev = elev; return this; }
    public int getStairStride() { return stride; }
    public int getStairElevation() { return elev; }

    public ExcavateShape setReticle(int x, int y) {
        setReticle(y * 5 + x);
        return this;
    }

    public ExcavateShape setReticle(int idx) {
        reticle = (byte) Mth.clamp(idx, 0, 24);
        rebuildOutline();
        return this;
    }

    public int getReticle() { return reticle; }
    public ExcavateShape setMaxDepth(int depth) { this.maxDepth = depth; return this; }
    public int getMaxDepth() { return maxDepth; }
    public ExcavateShape setRevDepth(int depth) { this.revDepth = depth; return this; }
    public int getRevDepth() { return revDepth; }
    public int getShapeMask() { return shapeMask; }

    public ExcavateShape setMask(int mask) {
        shapeMask = mask;
        rebuildOutline();
        return this;
    }

    public ExcavateShape setMask(int x, int y, boolean state) {
        if (x < 0 || x >= 5 || y < 0 || y >= 5) return this;
        shapeMask = state ? shapeMask | 1 << y * 5 + x : shapeMask & ~(1 << y * 5 + x);
        rebuildOutline();
        return this;
    }

    public void readFromJson(JsonObject json) {
        name = JsonHelper.GetString(json, "name", "New Shape");
        maxDepth = JsonHelper.GetNumber(json, "depth", -1).intValue();
        revDepth = JsonHelper.GetNumber(json, "reverse", 0).intValue();
        reticle = (byte) Mth.clamp(JsonHelper.GetNumber(json, "reticle", 12).byteValue(), 0, 24);
        stride = JsonHelper.GetNumber(json, "stride", 0).intValue();
        elev = JsonHelper.GetNumber(json, "elevation", 0).intValue();
        shapeMask = 0;
        JsonArray mask = JsonHelper.GetArray(json, "mask");
        int last = Math.min(mask.size(), 5) - 1;
        for (int row = last; row >= 0; row--) {
            JsonElement element = mask.get(last - row);
            if (element == null || !element.isJsonPrimitive()) continue;
            String line = element.getAsString();
            for (int i = 0; i < Math.min(5, line.length()); i++) {
                char c = line.charAt(i);
                if (c == 'X' || c == 'x') shapeMask |= 1 << row * 5 + i;
            }
        }
        rebuildOutline();
    }

    public JsonObject writeToJson(JsonObject json) {
        json.addProperty("name", name);
        json.addProperty("depth", maxDepth);
        json.addProperty("reverse", revDepth);
        json.addProperty("reticle", reticle);
        json.addProperty("stride", stride);
        json.addProperty("elevation", elev);
        JsonArray mask = new JsonArray();
        StringBuilder sb = new StringBuilder(5);
        for (int i = 24; i >= 0; i--) {
            sb.append((shapeMask & 1 << i) != 0 ? "X" : "O");
            if (i % 5 == 0) {
                mask.add(new JsonPrimitive(sb.toString()));
                sb.setLength(0);
            }
        }
        json.add("mask", mask);
        return json;
    }

    public void readFromNBT(CompoundTag tag) {
        name = tag.getStringOr("name", "New Shape");
        shapeMask = tag.getIntOr("mask", 0);
        maxDepth = tag.getIntOr("depth", -1);
        revDepth = tag.getIntOr("reverse", 0);
        reticle = tag.getByteOr("reticle", (byte) 12);
        stride = tag.getIntOr("stride", 0);
        elev = tag.getIntOr("elev", 0);
        rebuildOutline();
    }

    public CompoundTag writeToNBT(CompoundTag tag) {
        tag.putString("name", name);
        tag.putInt("mask", shapeMask);
        tag.putInt("depth", maxDepth);
        tag.putInt("reverse", revDepth);
        tag.putByte("reticle", reticle);
        tag.putInt("stride", stride);
        tag.putInt("elev", elev);
        return tag;
    }

    public boolean isValid(BlockPos origin, BlockPos pos, Direction side, Direction facing) {
        int[] off = new int[]{pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ()};
        if (side.getAxis().isVertical()) rotateFacing(off, facing);
        rotateSide(off, side);
        off[0] += reticle % 5;
        off[1] += reticle / 5;
        int lift = stride > 0 && elev != 0 ? off[2] / stride * elev : 0;
        if (off[0] >= 0 && off[0] < 5 && off[1] - lift >= 0 && off[1] - lift < 5
                && (revDepth < 0 || off[2] >= -revDepth)
                && (maxDepth < 0 || off[2] < maxDepth)) {
            return (shapeMask & 1 << (off[1] - lift) * 5 + off[0]) != 0;
        }
        return false;
    }

    private static void rotateFacing(int[] v, Direction facing) {
        int x = v[0];
        int z = v[2];
        switch (facing) {
            case EAST -> { v[0] = z; v[2] = -x; }
            case SOUTH -> { v[0] = -x; v[2] = -z; }
            case WEST -> { v[0] = -z; v[2] = x; }
            default -> { }
        }
    }

    private static void rotateSide(int[] v, Direction side) {
        int x = v[0];
        int y = v[1];
        int z = v[2];
        switch (side) {
            case EAST -> { v[0] = -z; v[2] = x; }
            case WEST -> { v[0] = z; v[2] = -x; }
            case NORTH -> { v[0] = -x; v[2] = -z; }
            case DOWN -> { v[0] = -x; v[1] = -z; v[2] = -y; }
            case UP -> { v[0] = -x; v[1] = z; v[2] = y; }
            default -> { }
        }
    }

    public static int posToMask(int x, int y) {
        return 1 << y * 5 + x;
    }

    public static Direction getFacing(Player player, BlockState state, BlockPos pos) {
        BlockHitResult hit;
        try {
            double dist = Math.sqrt(player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            VoxelShape shape = state.getShape(player.level(), pos);
            Vec3 start = player.getEyePosition(1.0F);
            Vec3 end = start.add(player.getLookAngle().scale(dist + 1.0));
            shape = shape.move(pos.getX(), pos.getY(), pos.getZ());
            hit = shape.clip(start, end, pos);
        } catch (Exception e) {
            OreExcavation.logger.info("Unable to get excavation direction for player " + player.getName().getString(), e);
            hit = null;
        }
        return hit == null ? getFacing(player) : hit.getDirection().getOpposite();
    }

    public static Direction getFacing(Player player) {
        Vec3 look = player.getLookAngle();
        double y = Math.abs(look.y);
        if (y > Math.abs(look.x) && y > Math.abs(look.z)) {
            return look.y > 0.0 ? Direction.UP : Direction.DOWN;
        }
        return player.getDirection();
    }

    public String writeShareCode() {
        StringBuilder sb = new StringBuilder();
        writeVarInt(shapeMask, sb);
        writeVarInt(reticle, sb);
        writeVarInt(maxDepth, sb);
        writeVarInt(revDepth, sb);
        writeVarInt(stride, sb);
        writeVarInt(elev, sb);
        return sb.toString();
    }

    public void readShareCode(@Nonnull String code) {
        CharIterator it = new CharIterator(code);
        try {
            int[] values = new int[6];
            for (int i = 0; i < 6; i++) values[i] = readVarInt(it);
            setMask(values[0]);
            setReticle(values[1]);
            setMaxDepth(values[2]);
            setRevDepth(values[3]);
            setStairStride(values[4]);
            setStairElev(values[5]);
        } catch (Exception e) {
            OreExcavation.logger.error("Unable to read share code!", e);
        }
    }

    public static boolean validateShareCode(@Nonnull String code) {
        CharIterator it = new CharIterator(code);
        try {
            for (int i = 0; i < 6; i++) readVarInt(it);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void writeVarInt(int value, StringBuilder sb) {
        int v = value >> 31 ^ value << 1;
        do {
            byte b = (byte) (v & 127);
            v >>>= 7;
            if (v != 0) b = (byte) (b | 128);
            sb.append(String.format("%02X", b));
        } while (v != 0);
    }

    private static int readVarInt(CharIterator it) throws EOFException {
        int value = 0;
        int shift = 0;
        int b;
        do {
            b = read(it);
            if (b < 0) throw new EOFException();
            value |= (b & 127) << shift;
            shift += 7;
        } while ((b & 128) != 0);
        return value >>> 1 ^ -(value & 1);
    }

    private static int read(CharIterator it) {
        try {
            int hi = hexChar(it.next());
            int lo = hexChar(it.next());
            return (hi | lo) < 0 ? -1 : (hi << 4 | lo) & 255;
        } catch (NoSuchElementException e) {
            return -1;
        }
    }

    private static int hexChar(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'Z') return c - 'A' + 10;
        return c >= 'a' && c < 'z' ? c - 'a' + 10 : -1;
    }

    private static class CharIterator {
        private final String str;
        private final int last;
        private int idx = 0;

        private CharIterator(String str) {
            this.str = str;
            this.last = str.length() - 1;
        }

        public boolean hasNext() {
            return idx <= last;
        }

        public char next() throws NoSuchElementException {
            if (!hasNext()) throw new NoSuchElementException();
            return str.charAt(idx++);
        }
    }
}
