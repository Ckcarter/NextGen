package net.nextgen.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;

import net.minecraftforge.network.simple.SimpleChannel;
import net.nextgen.NextGen;

public final class ModMessages {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NextGen.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private ModMessages() {}

    public static void register() {
        int id = packetId++;
        CHANNEL.registerMessage(id, SetCompanionSkinC2SPacket.class, SetCompanionSkinC2SPacket::toBytes,
                SetCompanionSkinC2SPacket::new, SetCompanionSkinC2SPacket::handle);
        id = packetId++;
        CHANNEL.registerMessage(id, UnsummonCompanionC2SPacket.class, UnsummonCompanionC2SPacket::toBytes,
                UnsummonCompanionC2SPacket::new, UnsummonCompanionC2SPacket::handle);
        id = packetId++;
        CHANNEL.registerMessage(id, SetCompanionStayC2SPacket.class, SetCompanionStayC2SPacket::toBytes,
                SetCompanionStayC2SPacket::new, SetCompanionStayC2SPacket::handle);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static <MSG> void send(MSG message, PacketDistributor.PacketTarget target) {
        CHANNEL.send(target, message);
    }
}