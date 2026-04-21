package net.nextgen.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.nextgen.entity.custom.CompanionEntity;

public class SetCompanionStayC2SPacket {

    private final int entityId;
    private final boolean stay;

    public SetCompanionStayC2SPacket(int entityId, boolean stay) {
        this.entityId = entityId;
        this.stay = stay;
    }

    public SetCompanionStayC2SPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.stay = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeBoolean(this.stay);
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
                companion.setStay(this.stay);
            }
        });
        context.setPacketHandled(true);
    }
}