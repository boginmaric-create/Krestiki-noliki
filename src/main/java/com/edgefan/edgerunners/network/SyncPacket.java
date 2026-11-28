package com.edgefan.edgerunners.network;

import com.edgefan.edgerunners.cyber.CyberData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncPacket(int implants, float psychosis, int sande, int sandeCd, int monoCd, int scanCd, int berserk,
                         int plsCd, int hero, int heroCd, int barrage) {
    public static SyncPacket of(Player p) {
        return new SyncPacket(CyberData.implants(p), CyberData.psychosis(p), CyberData.get(p, CyberData.SANDE),
                CyberData.get(p, CyberData.SANDE_CD), CyberData.get(p, CyberData.MONO_CD),
                CyberData.get(p, CyberData.SCAN_CD), CyberData.get(p, CyberData.BERSERK),
                CyberData.get(p, CyberData.PLS_CD), CyberData.get(p, CyberData.HERO), CyberData.get(p, CyberData.HERO_CD), CyberData.get(p, CyberData.BARRAGE));
    }

    public SyncPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
                b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(implants).writeFloat(psychosis);
        b.writeVarInt(sande).writeVarInt(sandeCd).writeVarInt(monoCd).writeVarInt(scanCd).writeVarInt(berserk);
        b.writeVarInt(plsCd).writeVarInt(hero).writeVarInt(heroCd).writeVarInt(barrage);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) { ClientCyberData.apply(this); }
}
