package dev.z33.factoryhud.server;

import dev.z33.factoryhud.data.HudBindingStore;
import dev.z33.factoryhud.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HudServerEvents {
    private HudServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        ItemStack goggles = player.getItemBySlot(EquipmentSlot.HEAD);
        if (goggles.is(ModItems.FACTORY_GOGGLES.get())) {
            if (HudBindingStore.migrateLegacyPlayerData(player, goggles)) {
                player.displayClientMessage(Component.translatable("message.factory_hud.migrated"), false);
            }
            HudSnapshotService.send(player, goggles);
        }
    }
}
