package dev.z33.factoryhud.network;

import dev.z33.factoryhud.FactoryHud;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenEditorPayload(String ignored) implements CustomPacketPayload {
    public static final OpenEditorPayload INSTANCE = new OpenEditorPayload("");
    public static final Type<OpenEditorPayload> TYPE =
            new Type<>(FactoryHud.id("open_editor"));
    public static final StreamCodec<ByteBuf, OpenEditorPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    OpenEditorPayload::ignored,
                    OpenEditorPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
