package com.edgefan.edgerunners.network;

import com.edgefan.edgerunners.EdgerunnersMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    private Network() {}

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(EdgerunnersMod.MODID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        CHANNEL.messageBuilder(AbilityPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AbilityPacket::encode).decoder(AbilityPacket::new)
                .consumerMainThread(AbilityPacket::handle).add();
        CHANNEL.messageBuilder(SyncPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPacket::encode).decoder(SyncPacket::new)
                .consumerMainThread(SyncPacket::handle).add();
    }

    public static void toServer(Object msg) { CHANNEL.sendToServer(msg); }

    public static void toPlayer(ServerPlayer p, Object msg) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg); }
}
