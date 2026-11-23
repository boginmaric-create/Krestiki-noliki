package com.edgefan.edgerunners.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Tells clients which cyberware a player has, so it can be drawn on their model. */
public record ImplantsPacket(int entityId, int implants, boolean sandevistanActive) {
    public ImplantsPacket(FriendlyByteBuf b) { this(b.readVarInt(), b.readVarInt(), b.readBoolean()); }

    public void encode(FriendlyByteBuf b) { b.writeVarInt(entityId).writeVarInt(implants).writeBoolean(sandevistanActive); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ClientCyberData.VISIBLE.put(entityId, implants | (sandevistanActive ? ClientCyberData.SANDE_ACTIVE_BIT : 0));
    }
}
