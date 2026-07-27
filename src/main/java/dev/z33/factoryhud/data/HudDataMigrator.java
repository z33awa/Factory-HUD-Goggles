package dev.z33.factoryhud.data;

import dev.z33.factoryhud.FactoryHud;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class HudDataMigrator {
    public static final int CURRENT_VERSION = 4;
    public static final String BINDINGS_KEY = FactoryHud.MOD_ID + ":bindings";
    public static final String VERSION_KEY = FactoryHud.MOD_ID + ":data_version";
    public static final String BACKUP_KEY = FactoryHud.MOD_ID + ":pre_migration_backup";
    public static final String AUTO_DIM_KEY = FactoryHud.MOD_ID + ":auto_dim_unavailable";
    public static final String UNAVAILABLE_OPACITY_KEY =
            FactoryHud.MOD_ID + ":unavailable_opacity";

    private HudDataMigrator() {
    }

    public static int version(CompoundTag root) {
        return root.contains(VERSION_KEY, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0, root.getInt(VERSION_KEY))
                : 0;
    }

    public static boolean isFutureVersion(CompoundTag root) {
        return version(root) > CURRENT_VERSION;
    }

    public static void migrateForRead(CompoundTag root) {
        migrate(root, false);
    }

    public static boolean migrateForWrite(CompoundTag root) {
        if (isFutureVersion(root)) {
            return false;
        }
        migrate(root, true);
        return true;
    }

    private static void migrate(CompoundTag root, boolean createBackup) {
        int sourceVersion = version(root);
        if (sourceVersion >= CURRENT_VERSION) {
            return;
        }

        if (createBackup
                && root.contains(BINDINGS_KEY)
                && !root.contains(BACKUP_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag backup = new CompoundTag();
            backup.putInt(VERSION_KEY, sourceVersion);
            Tag originalBindings = root.get(BINDINGS_KEY);
            if (originalBindings != null) {
                backup.put(BINDINGS_KEY, originalBindings.copy());
            }
            root.put(BACKUP_KEY, backup);
        }

        if (root.contains(BINDINGS_KEY, Tag.TAG_LIST)) {
            ListTag bindings = root.getList(BINDINGS_KEY, Tag.TAG_COMPOUND);
            for (int index = 0; index < bindings.size(); index++) {
                migrateBinding(bindings.getCompound(index), index);
            }
        }
        if (!root.contains(AUTO_DIM_KEY, Tag.TAG_BYTE)) {
            root.putBoolean(AUTO_DIM_KEY, HudGogglesSettings.DEFAULT.autoDimUnavailable());
        }
        if (!root.contains(UNAVAILABLE_OPACITY_KEY, Tag.TAG_ANY_NUMERIC)) {
            root.putFloat(
                    UNAVAILABLE_OPACITY_KEY,
                    HudGogglesSettings.DEFAULT.unavailableOpacity()
            );
        }
        root.putInt(VERSION_KEY, CURRENT_VERSION);
    }

    private static void migrateBinding(CompoundTag tag, int index) {
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
        if (dimension != null && tag.contains("pos", Tag.TAG_ANY_NUMERIC)
                && !tag.hasUUID("id")) {
            BlockPos pos = BlockPos.of(tag.getLong("pos"));
            String key = dimension + ":" + pos.asLong();
            UUID id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
            tag.putUUID("id", id);
        }
        if (!tag.contains("face", Tag.TAG_STRING)) {
            tag.putString("face", "up");
        }
        if (!tag.contains("hud_x", Tag.TAG_ANY_NUMERIC)) {
            int column = (index / 6) % 4;
            tag.putFloat("hud_x", 0.72F - column * 0.22F);
        }
        if (!tag.contains("hud_y", Tag.TAG_ANY_NUMERIC)) {
            tag.putFloat("hud_y", 0.04F + (index % 6) * 0.155F);
        }
        if (!tag.contains("scale", Tag.TAG_ANY_NUMERIC)) {
            tag.putFloat("scale", 1.0F);
        }
        if (!tag.contains("opacity", Tag.TAG_ANY_NUMERIC)) {
            tag.putFloat("opacity", 1.0F);
        }
    }
}
