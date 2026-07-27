package dev.z33.factoryhud.client;

import dev.z33.factoryhud.network.OpenEditorPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientEditorHandler {
    private ClientEditorHandler() {
    }

    public static void open(OpenEditorPayload payload, IPayloadContext context) {
        Minecraft.getInstance().setScreen(new HudEditorScreen());
    }
}
