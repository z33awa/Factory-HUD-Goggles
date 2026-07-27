package dev.z33.factoryhud.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.List;

public final class HudDataMigrationSelfTest {
    private HudDataMigrationSelfTest() {
    }

    public static void main(String[] args) {
        migratesLegacyAndCreatesBackup();
        isolatesCorruptBindings();
        refusesFutureVersionWrites();
        System.out.println("HUD data migration tests passed");
    }

    private static void migratesLegacyAndCreatesBackup() {
        CompoundTag root = new CompoundTag();
        CompoundTag binding = new CompoundTag();
        binding.putString("dimension", "minecraft:overworld");
        binding.putLong("pos", 123456789L);
        binding.putString("unknown_future_field", "preserve-me");
        ListTag bindings = new ListTag();
        bindings.add(binding);
        root.put(HudDataMigrator.BINDINGS_KEY, bindings);

        check(HudDataMigrator.migrateForWrite(root), "legacy data should be writable");
        check(
                root.getInt(HudDataMigrator.VERSION_KEY) == HudDataMigrator.CURRENT_VERSION,
                "legacy data version was not upgraded"
        );
        check(
                root.contains(HudDataMigrator.BACKUP_KEY, Tag.TAG_COMPOUND),
                "pre-migration backup was not created"
        );
        check(
                root.getBoolean(HudDataMigrator.AUTO_DIM_KEY),
                "auto-dim default was not enabled"
        );
        check(
                Math.abs(root.getFloat(HudDataMigrator.UNAVAILABLE_OPACITY_KEY) - 0.35F)
                        < 0.001F,
                "unavailable opacity default was not added"
        );

        CompoundTag migrated = root.getList(
                HudDataMigrator.BINDINGS_KEY,
                Tag.TAG_COMPOUND
        ).getCompound(0);
        check(migrated.hasUUID("id"), "legacy binding did not receive a stable UUID");
        check(migrated.contains("scale"), "scale default was not added");
        check(migrated.contains("opacity"), "opacity default was not added");
        check(
                "preserve-me".equals(migrated.getString("unknown_future_field")),
                "unknown binding fields were discarded"
        );
        HudBinding parsed = HudBinding.tryLoad(migrated, 0).orElseThrow();
        HudBindingStore.writeBindingsPreservingUnknown(root, List.of(parsed));
        CompoundTag rewritten = root.getList(
                HudDataMigrator.BINDINGS_KEY,
                Tag.TAG_COMPOUND
        ).getCompound(0);
        check(
                "preserve-me".equals(rewritten.getString("unknown_future_field")),
                "normal saves discarded unknown binding fields"
        );

        String backupBefore = root.getCompound(HudDataMigrator.BACKUP_KEY).toString();
        check(HudDataMigrator.migrateForWrite(root), "current data should remain writable");
        check(
                backupBefore.equals(root.getCompound(HudDataMigrator.BACKUP_KEY).toString()),
                "migration backup must not be overwritten"
        );
    }

    private static void isolatesCorruptBindings() {
        CompoundTag corrupt = new CompoundTag();
        corrupt.putLong("pos", 7L);
        check(HudBinding.tryLoad(corrupt, 0).isEmpty(), "missing dimension must be rejected");

        CompoundTag valid = new CompoundTag();
        valid.putString("dimension", "minecraft:overworld");
        valid.putLong("pos", 7L);
        CompoundTag root = new CompoundTag();
        ListTag bindings = new ListTag();
        bindings.add(valid);
        root.put(HudDataMigrator.BINDINGS_KEY, bindings);
        HudDataMigrator.migrateForRead(root);
        check(
                HudBinding.tryLoad(
                        root.getList(
                                HudDataMigrator.BINDINGS_KEY,
                                Tag.TAG_COMPOUND
                        ).getCompound(0),
                        0
                ).isPresent(),
                "valid legacy binding should survive migration"
        );
    }

    private static void refusesFutureVersionWrites() {
        CompoundTag future = new CompoundTag();
        future.putInt(
                HudDataMigrator.VERSION_KEY,
                HudDataMigrator.CURRENT_VERSION + 1
        );
        future.putString("future_only", "keep");
        check(!HudDataMigrator.migrateForWrite(future), "future data must be read-only");
        check(
                "keep".equals(future.getString("future_only")),
                "future data was modified"
        );
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
