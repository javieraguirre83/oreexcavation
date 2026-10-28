package oreexcavation.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class PacketExcavation {
    private final CompoundTag tags;

    public PacketExcavation(CompoundTag tags) {
        this.tags = tags;
    }

    public PacketExcavation(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        this.tags = tag == null ? new CompoundTag() : tag;
    }

    public CompoundTag getTags() {
        return tags;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(tags);
    }
}
