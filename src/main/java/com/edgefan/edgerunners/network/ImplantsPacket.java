package com.edgefan.edgerunners.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Tells clients which cyberware a player has, so it can be drawn on their model. */
public record ImplantsPacket(int entityId, int implants, boolean sandevistanActive, int hero) {
    public ImplantsPacket(FriendlyByteBuf b) { this(b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readVarInt()); }

    public void encode(FriendlyByteBuf b) { b.writeVarInt(entityId).writeVarInt(implants); b.writeBoolean(sandevistanActive); b.writeVarInt(hero); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ClientCyberData.VISIBLE.put(entityId, implants | (sandevistanActive ? ClientCyberData.SANDE_ACTIVE_BIT : 0));
        if (hero > 0) ClientCyberData.HEROES.put(entityId, hero); else ClientCyberData.HEROES.remove(entityId);
    }
}
