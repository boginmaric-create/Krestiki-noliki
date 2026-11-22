package com.edgefan.edgerunners.network;

import com.edgefan.edgerunners.cyber.Abilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AbilityPacket(int ability) {
    public AbilityPacket(FriendlyByteBuf buf) { this(buf.readByte()); }

    public void encode(FriendlyByteBuf buf) { buf.writeByte(ability); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var sender = ctx.get().getSender();
        if (sender != null && sender.isAlive() && !sender.isSpectator()) Abilities.handle(sender, ability);
    }
}
