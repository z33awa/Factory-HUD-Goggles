package dev.z33.factoryhud.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Optional;

public record HudBinding(
        UUID id,
        ResourceLocation dimension,
        BlockPos pos,
        Direction face,
        float hudX,
        float hudY,
        String note,
        float scale,
        float opacity
) {
    public static final int MAX_NOTE_LENGTH = 80;
    public static final float MIN_SCALE = 0.5F;
    public static final float MAX_SCALE = 2.0F;
    public static final float MIN_OPACITY = 0.2F;
    public static final float MAX_OPACITY = 1.0F;

    public HudBinding {
        hudX = clamp(hudX);
        hudY = clamp(hudY);
        note = sanitizeNote(note);
        scale = clampRange(scale, MIN_SCALE, MAX_SCALE, 1.0F);
        opacity = clampRange(opacity, MIN_OPACITY, MAX_OPACITY, 1.0F);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("dimension", dimension.toString());
        tag.putLong("pos", pos.asLong());
        tag.putString("face", face.getName());
        tag.putFloat("hud_x", hudX);
        tag.putFloat("hud_y", hudY);
        if (!note.isEmpty()) {
            tag.putString("note", note);
        }
        tag.putFloat("scale", scale);
        tag.putFloat("opacity", opacity);
        return tag;
    }

    public static HudBinding load(CompoundTag tag, int index) {
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
        Direction face = Direction.byName(tag.getString("face"));
        if (dimension == null) {
            dimension = ResourceLocation.withDefaultNamespace("overworld");
        }
        if (face == null) {
            face = Direction.UP;
        }
        BlockPos pos = BlockPos.of(tag.getLong("pos"));
        UUID id = tag.hasUUID("id")
                ? tag.getUUID("id")
                : legacyId(dimension, pos);
        float hudX = tag.contains("hud_x") ? tag.getFloat("hud_x") : defaultX(index);
        float hudY = tag.contains("hud_y") ? tag.getFloat("hud_y") : defaultY(index);
        String note = tag.contains("note") ? tag.getString("note") : "";
        float scale = tag.contains("scale") ? tag.getFloat("scale") : 1.0F;
        float opacity = tag.contains("opacity") ? tag.getFloat("opacity") : 1.0F;
        return new HudBinding(id, dimension, pos, face, hudX, hudY, note, scale, opacity);
    }

    public static Optional<HudBinding> tryLoad(CompoundTag tag, int index) {
        try {
            if (!tag.contains("dimension", net.minecraft.nbt.Tag.TAG_STRING)
                    || !tag.contains("pos", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
                    || ResourceLocation.tryParse(tag.getString("dimension")) == null) {
                return Optional.empty();
            }
            return Optional.of(load(tag, index));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public boolean sameTarget(ResourceLocation otherDimension, BlockPos otherPos) {
        return dimension.equals(otherDimension) && pos.equals(otherPos);
    }

    public HudBinding withPosition(float x, float y) {
        return new HudBinding(id, dimension, pos, face, x, y, note, scale, opacity);
    }

    public HudBinding withNote(String value) {
        return new HudBinding(id, dimension, pos, face, hudX, hudY, value, scale, opacity);
    }

    public HudBinding withAppearance(float newScale, float newOpacity) {
        return new HudBinding(id, dimension, pos, face, hudX, hudY, note, newScale, newOpacity);
    }

    public static HudBinding create(
            ResourceLocation dimension,
            BlockPos pos,
            Direction face,
            int index
    ) {
        return new HudBinding(
                UUID.randomUUID(),
                dimension,
                pos.immutable(),
                face,
                defaultX(index),
                defaultY(index),
                "",
                1.0F,
                1.0F
        );
    }

    private static UUID legacyId(ResourceLocation dimension, BlockPos pos) {
        String key = dimension + ":" + pos.asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static float defaultX(int index) {
        int column = (index / 6) % 4;
        return 0.72F - column * 0.22F;
    }

    private static float defaultY(int index) {
        return 0.04F + (index % 6) * 0.155F;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float clampRange(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static String sanitizeNote(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return sanitized.length() <= MAX_NOTE_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_NOTE_LENGTH);
    }
}
