package dev.z33.factoryhud.network;

import dev.z33.factoryhud.FactoryHud;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HudSnapshotPayload(String json) implements CustomPacketPayload {
    public static final Type<HudSnapshotPayload> TYPE =
            new Type<>(FactoryHud.id("hud_snapshot"));

    public static final StreamCodec<ByteBuf, HudSnapshotPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    HudSnapshotPayload::json,
                    HudSnapshotPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
