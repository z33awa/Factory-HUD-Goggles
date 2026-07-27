package dev.z33.factoryhud.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.z33.factoryhud.FactoryHud;
import dev.z33.factoryhud.data.HudBinding;
import dev.z33.factoryhud.data.HudBindingStore;
import dev.z33.factoryhud.server.HudSnapshotService;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateCardNotePayload(String json) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final Type<UpdateCardNotePayload> TYPE =
            new Type<>(FactoryHud.id("update_card_note"));
    public static final StreamCodec<ByteBuf, UpdateCardNotePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    UpdateCardNotePayload::json,
                    UpdateCardNotePayload::new
            );

    public static UpdateCardNotePayload of(UUID id, String note) {
        return new UpdateCardNotePayload(GSON.toJson(new Update(id.toString(), note)));
    }

    public static void handle(UpdateCardNotePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        try {
            Update update = GSON.fromJson(payload.json(), Update.class);
            if (update == null || update.note() == null
                    || update.note().length() > HudBinding.MAX_NOTE_LENGTH) {
                return;
            }
            UUID id = UUID.fromString(update.id());
            if (HudBindingStore.updateNote(player, id, update.note())) {
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

    private record Update(String id, String note) {
    }
}
