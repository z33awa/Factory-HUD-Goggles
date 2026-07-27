package dev.z33.factoryhud.network;

import dev.z33.factoryhud.client.ClientHudState;
import dev.z33.factoryhud.client.ClientEditorHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                HudSnapshotPayload.TYPE,
                HudSnapshotPayload.STREAM_CODEC,
                ClientHudState::handle
        );
        registrar.playToClient(
                OpenEditorPayload.TYPE,
                OpenEditorPayload.STREAM_CODEC,
                ClientEditorHandler::open
        );
        registrar.playToServer(
                UpdateCardPositionPayload.TYPE,
                UpdateCardPositionPayload.STREAM_CODEC,
                UpdateCardPositionPayload::handle
        );
        registrar.playToServer(
                UpdateCardNotePayload.TYPE,
                UpdateCardNotePayload.STREAM_CODEC,
                UpdateCardNotePayload::handle
        );
        registrar.playToServer(
                OpenGogglesEditorRequestPayload.TYPE,
                OpenGogglesEditorRequestPayload.STREAM_CODEC,
                OpenGogglesEditorRequestPayload::handle
        );
        registrar.playToServer(
                DeleteCardPayload.TYPE,
                DeleteCardPayload.STREAM_CODEC,
                DeleteCardPayload::handle
        );
        registrar.playToServer(
                UpdateCardAppearancePayload.TYPE,
                UpdateCardAppearancePayload.STREAM_CODEC,
                UpdateCardAppearancePayload::handle
        );
        registrar.playToServer(
                UpdateGogglesSettingsPayload.TYPE,
                UpdateGogglesSettingsPayload.STREAM_CODEC,
                UpdateGogglesSettingsPayload::handle
        );
    }
}
