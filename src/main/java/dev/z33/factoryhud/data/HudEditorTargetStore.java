package dev.z33.factoryhud.data;

import dev.z33.factoryhud.item.ModItems;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class HudEditorTargetStore {
    private static final Map<ServerPlayer, Target> TARGETS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private HudEditorTargetStore() {
    }

    public static boolean select(ServerPlayer player, Container container, int slot) {
        if (slot < 0 || slot >= container.getContainerSize()) {
            return false;
        }
        ItemStack goggles = container.getItem(slot);
        if (!goggles.is(ModItems.FACTORY_GOGGLES.get())) {
            return false;
        }
        TARGETS.put(player, new Target(container, slot));
        return true;
    }

    public static ItemStack selected(ServerPlayer player) {
        Target target = TARGETS.get(player);
        return target == null ? ItemStack.EMPTY : target.resolve();
    }

    public static void markChanged(ServerPlayer player, ItemStack goggles) {
        Target target = TARGETS.get(player);
        if (target != null && target.resolve() == goggles) {
            target.container().setChanged();
        }
    }

    private record Target(Container container, int slot) {
        private ItemStack resolve() {
            if (slot < 0 || slot >= container.getContainerSize()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = container.getItem(slot);
            return stack.is(ModItems.FACTORY_GOGGLES.get()) ? stack : ItemStack.EMPTY;
        }
    }
}
