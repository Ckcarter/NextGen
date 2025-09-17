package net.nextgen.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.nextgen.entity.custom.CompanionEntity;

public class UnsummonCompanionC2SPacket {

    private final int entityId;

    public UnsummonCompanionC2SPacket(int entityId) {
        this.entityId = entityId;
    }

    public UnsummonCompanionC2SPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity entity = sender.level().getEntity(this.entityId);
            if (entity instanceof CompanionEntity companion && companion.isOwnedBy(sender)) {
                companion.unsummon(sender);
            }
        });
        context.setPacketHandled(true);
    }
}