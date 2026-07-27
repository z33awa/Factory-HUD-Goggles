package dev.z33.factoryhud.client;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.z33.factoryhud.network.HudSnapshotPayload;
import java.util.List;
import java.util.ArrayList;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.UUID;
import dev.z33.factoryhud.data.HudGogglesSettings;

public final class ClientHudState {
    private static final Gson GSON = new Gson();
    private static volatile List<Card> cards = List.of();
    private static volatile Settings settings = Settings.DEFAULT;

    private ClientHudState() {
    }

    public static void handle(HudSnapshotPayload payload, IPayloadContext context) {
        try {
            Envelope envelope = GSON.fromJson(payload.json(), Envelope.class);
            List<Card> next = envelope != null && envelope.reset()
                    ? new ArrayList<>()
                    : new ArrayList<>(cards);
            if (envelope != null && envelope.cards() != null) {
                next.addAll(envelope.cards());
            }
            if (envelope != null
                    && envelope.autoDimUnavailable() != null
                    && envelope.unavailableOpacity() != null) {
                settings = new Settings(
                        envelope.autoDimUnavailable(),
                        envelope.unavailableOpacity()
                );
            }
            cards = List.copyOf(next);
        } catch (JsonSyntaxException ignored) {
            cards = List.of();
        }
    }

    public static List<Card> cards() {
        return cards;
    }

    public static Settings settings() {
        return settings;
    }

    public static void updateSettings(boolean autoDimUnavailable, float unavailableOpacity) {
        settings = new Settings(autoDimUnavailable, unavailableOpacity);
    }

    public static void updatePosition(UUID id, float x, float y) {
        List<Card> updated = new ArrayList<>(cards.size());
        for (Card card : cards) {
            updated.add(card.id().equals(id.toString()) ? card.withPosition(x, y) : card);
        }
        cards = List.copyOf(updated);
    }

    public static void updateNote(UUID id, String note) {
        List<Card> updated = new ArrayList<>(cards.size());
        for (Card card : cards) {
            updated.add(card.id().equals(id.toString()) ? card.withNote(note) : card);
        }
        cards = List.copyOf(updated);
    }

    public static void remove(UUID id) {
        String target = id.toString();
        cards = cards.stream()
                .filter(card -> !card.id().equals(target))
                .toList();
    }

    public static void updateAppearance(UUID id, float scale, float opacity) {
        List<Card> updated = new ArrayList<>(cards.size());
        for (Card card : cards) {
            updated.add(card.id().equals(id.toString())
                    ? card.withAppearance(scale, opacity)
                    : card);
        }
        cards = List.copyOf(updated);
    }

    private record Envelope(
            boolean reset,
            List<Card> cards,
            Boolean autoDimUnavailable,
            Float unavailableOpacity
    ) {
    }

    public record Settings(boolean autoDimUnavailable, float unavailableOpacity) {
        public static final Settings DEFAULT = new Settings(
                HudGogglesSettings.DEFAULT.autoDimUnavailable(),
                HudGogglesSettings.DEFAULT.unavailableOpacity()
        );

        public Settings {
            if (!Float.isFinite(unavailableOpacity)) {
                unavailableOpacity = 0.35F;
            }
            unavailableOpacity = Math.max(
                    HudGogglesSettings.MIN_UNAVAILABLE_OPACITY,
                    Math.min(HudGogglesSettings.MAX_UNAVAILABLE_OPACITY, unavailableOpacity)
            );
        }
    }

    public record Card(
            String id,
            String dimension,
            int blockX,
            int blockY,
            int blockZ,
            float hudX,
            float hudY,
            String note,
            float scale,
            float opacity,
            String title,
            String status,
            List<String> lines
    ) {
        public Card withPosition(float x, float y) {
            return new Card(
                    id, dimension, blockX, blockY, blockZ,
                    x, y, note, scale, opacity, title, status, lines
            );
        }

        public Card withNote(String value) {
            return new Card(
                    id, dimension, blockX, blockY, blockZ,
                    hudX, hudY, value, scale, opacity, title, status, lines
            );
        }

        public Card withAppearance(float newScale, float newOpacity) {
            return new Card(
                    id, dimension, blockX, blockY, blockZ,
                    hudX, hudY, note, newScale, newOpacity, title, status, lines
            );
        }
    }
}
