package dev.z33.factoryhud.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.z33.factoryhud.FactoryHud;
import dev.z33.factoryhud.data.HudBindingStore;
import dev.z33.factoryhud.server.HudSnapshotService;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateCardPositionPayload(String json) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final Type<UpdateCardPositionPayload> TYPE =
            new Type<>(FactoryHud.id("update_card_position"));
    public static final StreamCodec<ByteBuf, UpdateCardPositionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    UpdateCardPositionPayload::json,
                    UpdateCardPositionPayload::new
            );

    public static UpdateCardPositionPayload of(UUID id, float x, float y) {
        return new UpdateCardPositionPayload(GSON.toJson(new Update(id.toString(), x, y)));
    }

    public static void handle(UpdateCardPositionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        try {
            Update update = GSON.fromJson(payload.json(), Update.class);
            if (update == null) {
                return;
            }
            UUID id = UUID.fromString(update.id());
            if (HudBindingStore.updatePosition(player, id, update.x(), update.y())) {
                HudSnapshotService.send(player, HudBindingStore.gogglesContaining(player, id));
            }
        } catch (JsonSyntaxException | IllegalArgumentException ignored) {
            // Invalid client data is ignored; the server remains authoritative.
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private record Update(String id, float x, float y) {
    }
}
