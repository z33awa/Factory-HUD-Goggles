package dev.z33.factoryhud.network;

import dev.z33.factoryhud.FactoryHud;
import dev.z33.factoryhud.item.ModItems;
import dev.z33.factoryhud.data.HudEditorTargetStore;
import dev.z33.factoryhud.data.HudBindingStore;
import dev.z33.factoryhud.server.HudSnapshotService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenGogglesEditorRequestPayload(
        int targetType,
        int containerId,
        int slot
) implements CustomPacketPayload {
    public static final int INVENTORY_TARGET = 0;
    public static final int MENU_TARGET = 1;

    public static final Type<OpenGogglesEditorRequestPayload> TYPE =
            new Type<>(FactoryHud.id("open_goggles_editor_request"));
    public static final StreamCodec<ByteBuf, OpenGogglesEditorRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    OpenGogglesEditorRequestPayload::targetType,
                    ByteBufCodecs.VAR_INT,
                    OpenGogglesEditorRequestPayload::containerId,
                    ByteBufCodecs.VAR_INT,
                    OpenGogglesEditorRequestPayload::slot,
                    OpenGogglesEditorRequestPayload::new
            );

    public static OpenGogglesEditorRequestPayload inventory(int slot) {
        return new OpenGogglesEditorRequestPayload(INVENTORY_TARGET, 0, slot);
    }

    public static OpenGogglesEditorRequestPayload menu(int containerId, int slot) {
        return new OpenGogglesEditorRequestPayload(MENU_TARGET, containerId, slot);
    }

    public static void handle(OpenGogglesEditorRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        boolean selected;
        if (payload.targetType() == INVENTORY_TARGET) {
            selected = HudEditorTargetStore.select(
                    player,
                    player.getInventory(),
                    payload.slot()
            );
        } else if (payload.targetType() == MENU_TARGET
                && player.containerMenu.containerId == payload.containerId()
                && payload.slot() >= 0
                && payload.slot() < player.containerMenu.slots.size()) {
            net.minecraft.world.inventory.Slot menuSlot =
                    player.containerMenu.getSlot(payload.slot());
            selected = HudEditorTargetStore.select(
                    player,
                    menuSlot.container,
                    menuSlot.getContainerSlot()
            );
        } else {
            selected = false;
        }
        if (!selected) {
            return;
        }
        ItemStack goggles = HudEditorTargetStore.selected(player);
        if (HudBindingStore.hasFutureData(goggles)) {
            player.displayClientMessage(
                    Component.translatable("message.factory_hud.incompatible_data")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }
        HudSnapshotService.send(player, goggles);
        PacketDistributor.sendToPlayer(player, OpenEditorPayload.INSTANCE);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
