package dev.z33.factoryhud.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class BindingConfirmationStore {
    private static final int CONFIRMATION_TICKS = 100;
    private static final Map<UUID, PendingBinding> PENDING = new ConcurrentHashMap<>();

    private BindingConfirmationStore() {
    }

    public static boolean confirm(
            ServerPlayer player,
            ItemStack goggles,
            ResourceLocation dimension,
            BlockPos pos,
            boolean removing
    ) {
        int now = player.tickCount;
        PendingBinding pending = PENDING.get(player.getUUID());
        if (pending != null
                && now <= pending.expiresAtTick()
                && pending.dimension().equals(dimension)
                && pending.pos().equals(pos)
                && pending.removing() == removing
                && ItemStack.isSameItemSameComponents(pending.goggles(), goggles)) {
            PENDING.remove(player.getUUID());
            return true;
        }

        PENDING.put(
                player.getUUID(),
                new PendingBinding(
                        goggles.copy(),
                        dimension,
                        pos.immutable(),
                        removing,
                        now + CONFIRMATION_TICKS
                )
        );
        return false;
    }

    public static void clear(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    private record PendingBinding(
            ItemStack goggles,
            ResourceLocation dimension,
            BlockPos pos,
            boolean removing,
            int expiresAtTick
    ) {
    }
}
