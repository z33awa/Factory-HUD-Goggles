package dev.z33.factoryhud.data;

import dev.z33.factoryhud.FactoryHud;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import dev.z33.factoryhud.item.ModItems;
import java.util.UUID;

public final class HudBindingStore {
    private static final String ROOT_KEY = HudDataMigrator.BINDINGS_KEY;

    private HudBindingStore() {
    }

    public static List<HudBinding> get(ItemStack goggles) {
        CompoundTag root = goggles.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (HudDataMigrator.isFutureVersion(root)) {
            return readBindings(root);
        }
        HudDataMigrator.migrateForRead(root);
        List<HudBinding> bindings = readBindings(root);
        if (bindings.isEmpty()
                && root.contains(ROOT_KEY, Tag.TAG_LIST)
                && !root.getList(ROOT_KEY, Tag.TAG_COMPOUND).isEmpty()
                && root.contains(HudDataMigrator.BACKUP_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag backup = root.getCompound(HudDataMigrator.BACKUP_KEY).copy();
            HudDataMigrator.migrateForRead(backup);
            return readBindings(backup);
        }
        return bindings;
    }

    private static List<HudBinding> readBindings(CompoundTag root) {
        ListTag stored = root.getList(ROOT_KEY, Tag.TAG_COMPOUND);
        List<HudBinding> bindings = new ArrayList<>(stored.size());
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < stored.size(); i++) {
            HudBinding.tryLoad(stored.getCompound(i), i).ifPresent(binding -> {
                if (ids.add(binding.id())) {
                    bindings.add(binding);
                }
            });
        }
        return bindings;
    }

    public static boolean hasFutureData(ItemStack goggles) {
        CompoundTag root = goggles.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return HudDataMigrator.isFutureVersion(root);
    }

    public static HudGogglesSettings getSettings(ItemStack goggles) {
        CompoundTag root =
                goggles.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!HudDataMigrator.isFutureVersion(root)) {
            HudDataMigrator.migrateForRead(root);
        }
        boolean autoDim = root.contains(HudDataMigrator.AUTO_DIM_KEY, Tag.TAG_BYTE)
                ? root.getBoolean(HudDataMigrator.AUTO_DIM_KEY)
                : HudGogglesSettings.DEFAULT.autoDimUnavailable();
        float opacity = root.contains(
                HudDataMigrator.UNAVAILABLE_OPACITY_KEY,
                Tag.TAG_ANY_NUMERIC
        ) ? root.getFloat(HudDataMigrator.UNAVAILABLE_OPACITY_KEY)
                : HudGogglesSettings.DEFAULT.unavailableOpacity();
        return new HudGogglesSettings(autoDim, opacity);
    }

    public static boolean updateSettings(
            ServerPlayer player,
            boolean autoDimUnavailable,
            float unavailableOpacity
    ) {
        if (!Float.isFinite(unavailableOpacity)
                || unavailableOpacity < HudGogglesSettings.MIN_UNAVAILABLE_OPACITY
                || unavailableOpacity > HudGogglesSettings.MAX_UNAVAILABLE_OPACITY) {
            return false;
        }
        ItemStack goggles = HudEditorTargetStore.selected(player);
        if (goggles.isEmpty() || hasFutureData(goggles)) {
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, goggles, root -> {
            if (!HudDataMigrator.migrateForWrite(root)) {
                return;
            }
            root.putBoolean(HudDataMigrator.AUTO_DIM_KEY, autoDimUnavailable);
            root.putFloat(HudDataMigrator.UNAVAILABLE_OPACITY_KEY, unavailableOpacity);
            root.putInt(HudDataMigrator.VERSION_KEY, HudDataMigrator.CURRENT_VERSION);
        });
        HudEditorTargetStore.markChanged(player, goggles);
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    public static ToggleResult toggle(
            ServerPlayer player,
            ItemStack goggles,
            ResourceLocation dimension,
            BlockPos pos,
            Direction face
    ) {
        if (!goggles.is(ModItems.FACTORY_GOGGLES.get())) {
            return ToggleResult.NO_GOGGLES;
        }

        List<HudBinding> bindings = get(goggles);
        int existing = -1;
        for (int i = 0; i < bindings.size(); i++) {
            if (bindings.get(i).sameTarget(dimension, pos)) {
                existing = i;
                break;
            }
        }

        if (existing >= 0) {
            bindings.remove(existing);
            if (!save(goggles, bindings)) {
                return ToggleResult.INCOMPATIBLE_DATA;
            }
            player.inventoryMenu.broadcastChanges();
            return ToggleResult.REMOVED;
        }

        bindings.add(HudBinding.create(dimension, pos, face, bindings.size()));
        if (!save(goggles, bindings)) {
            return ToggleResult.INCOMPATIBLE_DATA;
        }
        player.inventoryMenu.broadcastChanges();
        return ToggleResult.ADDED;
    }

    public static boolean updatePosition(ServerPlayer player, UUID id, float x, float y) {
        ItemStack goggles = gogglesContaining(player, id);
        if (goggles.isEmpty() || !Float.isFinite(x) || !Float.isFinite(y)) {
            return false;
        }

        List<HudBinding> bindings = get(goggles);
        for (int i = 0; i < bindings.size(); i++) {
            if (bindings.get(i).id().equals(id)) {
                bindings.set(i, bindings.get(i).withPosition(x, y));
                if (!save(goggles, bindings)) {
                    return false;
                }
                HudEditorTargetStore.markChanged(player, goggles);
                player.inventoryMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    public static boolean updateNote(ServerPlayer player, UUID id, String note) {
        ItemStack goggles = gogglesContaining(player, id);
        if (goggles.isEmpty() || note == null || note.length() > HudBinding.MAX_NOTE_LENGTH) {
            return false;
        }

        List<HudBinding> bindings = get(goggles);
        for (int i = 0; i < bindings.size(); i++) {
            if (bindings.get(i).id().equals(id)) {
                bindings.set(i, bindings.get(i).withNote(note));
                if (!save(goggles, bindings)) {
                    return false;
                }
                HudEditorTargetStore.markChanged(player, goggles);
                player.inventoryMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    public static boolean updateAppearance(
            ServerPlayer player,
            UUID id,
            float scale,
            float opacity
    ) {
        if (!Float.isFinite(scale) || !Float.isFinite(opacity)
                || scale < HudBinding.MIN_SCALE || scale > HudBinding.MAX_SCALE
                || opacity < HudBinding.MIN_OPACITY || opacity > HudBinding.MAX_OPACITY) {
            return false;
        }
        ItemStack goggles = gogglesContaining(player, id);
        if (goggles.isEmpty()) {
            return false;
        }

        List<HudBinding> bindings = get(goggles);
        for (int i = 0; i < bindings.size(); i++) {
            if (bindings.get(i).id().equals(id)) {
                bindings.set(i, bindings.get(i).withAppearance(scale, opacity));
                if (!save(goggles, bindings)) {
                    return false;
                }
                HudEditorTargetStore.markChanged(player, goggles);
                player.inventoryMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    public static ItemStack gogglesContaining(ServerPlayer player, UUID id) {
        ItemStack selected = HudEditorTargetStore.selected(player);
        if (contains(selected, id)) {
            return selected;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack goggles = player.getInventory().getItem(slot);
            if (contains(goggles, id)) {
                return goggles;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack remove(ServerPlayer player, UUID id) {
        ItemStack goggles = gogglesContaining(player, id);
        if (goggles.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<HudBinding> bindings = get(goggles);
        if (!bindings.removeIf(binding -> binding.id().equals(id))) {
            return ItemStack.EMPTY;
        }
        if (!save(goggles, bindings)) {
            return ItemStack.EMPTY;
        }
        HudEditorTargetStore.markChanged(player, goggles);
        player.inventoryMenu.broadcastChanges();
        return goggles;
    }

    private static boolean contains(ItemStack goggles, UUID id) {
        if (!goggles.is(ModItems.FACTORY_GOGGLES.get())) {
            return false;
        }
        for (HudBinding binding : get(goggles)) {
            if (binding.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean migrateLegacyPlayerData(ServerPlayer player, ItemStack goggles) {
        if (!get(goggles).isEmpty()) {
            return false;
        }
        CompoundTag playerData = player.getPersistentData();
        if (!playerData.contains(ROOT_KEY, Tag.TAG_LIST)) {
            return false;
        }

        ListTag stored = playerData.getList(ROOT_KEY, Tag.TAG_COMPOUND);
        List<HudBinding> migrated = new ArrayList<>(stored.size());
        for (int i = 0; i < stored.size(); i++) {
            HudBinding.tryLoad(stored.getCompound(i), i).ifPresent(migrated::add);
        }
        if (!save(goggles, migrated)) {
            return false;
        }
        playerData.remove(ROOT_KEY);
        player.inventoryMenu.broadcastChanges();
        return !migrated.isEmpty();
    }

    public static ItemStack equippedGoggles(ServerPlayer player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        return stack.is(ModItems.FACTORY_GOGGLES.get()) ? stack : ItemStack.EMPTY;
    }

    private static boolean save(ItemStack goggles, List<HudBinding> bindings) {
        CompoundTag existingRoot =
                goggles.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (HudDataMigrator.isFutureVersion(existingRoot)) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, goggles, root -> {
            if (!HudDataMigrator.migrateForWrite(root)) {
                return;
            }
            writeBindingsPreservingUnknown(root, bindings);
        });
        return true;
    }

    static void writeBindingsPreservingUnknown(
            CompoundTag root,
            List<HudBinding> bindings
    ) {
        ListTag oldList = root.getList(ROOT_KEY, Tag.TAG_COMPOUND);
        Map<UUID, CompoundTag> oldById = new HashMap<>();
        for (int i = 0; i < oldList.size(); i++) {
            CompoundTag oldTag = oldList.getCompound(i);
            if (oldTag.hasUUID("id")) {
                oldById.putIfAbsent(oldTag.getUUID("id"), oldTag);
            }
        }

        ListTag next = new ListTag();
        for (HudBinding binding : bindings) {
            CompoundTag preserved = oldById.containsKey(binding.id())
                    ? oldById.get(binding.id()).copy()
                    : new CompoundTag();
            preserved.merge(binding.save());
            next.add(preserved);
        }
        root.put(ROOT_KEY, next);
        root.putInt(HudDataMigrator.VERSION_KEY, HudDataMigrator.CURRENT_VERSION);
    }

    public enum ToggleResult {
        ADDED,
        REMOVED,
        NO_GOGGLES,
        INCOMPATIBLE_DATA
    }
}
