package net.nextgen.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.nextgen.entity.custom.CompanionEntity;

public class SetCompanionSkinC2SPacket {

    private final int entityId;
    private final String skinName;

    public SetCompanionSkinC2SPacket(int entityId, String skinName) {
        this.entityId = entityId;
        this.skinName = skinName == null ? "" : skinName;
    }

    public SetCompanionSkinC2SPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.skinName = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeUtf(this.skinName, 64);
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
                String trimmed = this.skinName.trim();
                companion.setSkinName(trimmed);
                if (trimmed.isEmpty()) {
                    companion.setCustomName(null);
                } else {
                    companion.setCustomName(Component.literal(trimmed));
                }
            }
        });
        context.setPacketHandled(true);
    }
}