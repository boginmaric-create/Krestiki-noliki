package com.edgefan.edgerunners.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: play a visual effect. Purely cosmetic. */
public record FxPacket(int type, int entityId, float x, float y, float z, float yaw, float pitch) {
    public static final int SANDE_START = 0, MONOWIRE = 1, SLASH = 2, PUNCH = 3, SCAN = 4, DOUBLE_JUMP = 5, LUNGE = 6, BERSERK = 7;

    public FxPacket(FriendlyByteBuf b) {
        this(b.readByte(), b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeByte(type); b.writeVarInt(entityId);
        b.writeFloat(x); b.writeFloat(y); b.writeFloat(z); b.writeFloat(yaw); b.writeFloat(pitch);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.edgefan.edgerunners.client.fx.FxManager.spawn(this));
    }
}
