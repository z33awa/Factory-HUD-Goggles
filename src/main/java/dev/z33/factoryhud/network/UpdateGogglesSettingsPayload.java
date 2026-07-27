package dev.z33.factoryhud.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.z33.factoryhud.FactoryHud;
import dev.z33.factoryhud.data.HudBindingStore;
import dev.z33.factoryhud.data.HudEditorTargetStore;
import dev.z33.factoryhud.server.HudSnapshotService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateGogglesSettingsPayload(String json) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final Type<UpdateGogglesSettingsPayload> TYPE =
            new Type<>(FactoryHud.id("update_goggles_settings"));
    public static final StreamCodec<ByteBuf, UpdateGogglesSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    UpdateGogglesSettingsPayload::json,
                    UpdateGogglesSettingsPayload::new
            );

    public static UpdateGogglesSettingsPayload of(
            boolean autoDimUnavailable,
            float unavailableOpacity
    ) {
        return new UpdateGogglesSettingsPayload(
                GSON.toJson(new Update(autoDimUnavailable, unavailableOpacity))
        );
    }

    public static void handle(UpdateGogglesSettingsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        try {
            Update update = GSON.fromJson(payload.json(), Update.class);
            if (update == null) {
                return;
            }
            if (HudBindingStore.updateSettings(
                    player,
                    update.autoDimUnavailable(),
                    update.unavailableOpacity()
            )) {
                ItemStack goggles = HudEditorTargetStore.selected(player);
                if (!goggles.isEmpty()) {
                    HudSnapshotService.send(player, goggles);
                }
            }
        } catch (JsonSyntaxException ignored) {
            // Invalid client data is ignored; the server remains authoritative.
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private record Update(boolean autoDimUnavailable, float unavailableOpacity) {
    }
}
