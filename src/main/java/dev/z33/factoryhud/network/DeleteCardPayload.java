package dev.z33.factoryhud.network;

import dev.z33.factoryhud.FactoryHud;
import dev.z33.factoryhud.data.HudBindingStore;
import dev.z33.factoryhud.server.HudSnapshotService;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DeleteCardPayload(String id) implements CustomPacketPayload {
    public static final Type<DeleteCardPayload> TYPE =
            new Type<>(FactoryHud.id("delete_card"));
    public static final StreamCodec<ByteBuf, DeleteCardPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    DeleteCardPayload::id,
                    DeleteCardPayload::new
            );

    public static void handle(DeleteCardPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        try {
            UUID id = UUID.fromString(payload.id());
            ItemStack goggles = HudBindingStore.remove(player, id);
            if (!goggles.isEmpty()) {
                HudSnapshotService.send(player, goggles);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid client IDs are ignored; the server remains authoritative.
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
