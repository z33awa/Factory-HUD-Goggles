package dev.z33.factoryhud.client;

import dev.z33.factoryhud.network.UpdateCardPositionPayload;
import dev.z33.factoryhud.network.UpdateCardNotePayload;
import dev.z33.factoryhud.network.DeleteCardPayload;
import dev.z33.factoryhud.network.UpdateCardAppearancePayload;
import dev.z33.factoryhud.network.UpdateGogglesSettingsPayload;
import dev.z33.factoryhud.data.HudBinding;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class HudEditorScreen extends Screen {
    private static final float[] DIM_OPACITY_STEPS = {
            0.20F, 0.35F, 0.50F, 0.65F, 0.80F
    };
    private final Map<String, Position> previewPositions = new HashMap<>();
    private String draggingId;
    private double dragOffsetX;
    private double dragOffsetY;
    private String noteEditingId;
    private EditBox noteBox;
    private Button settingsButton;
    private Button autoDimButton;
    private Button dimOpacityButton;
    private boolean settingsOpen;

    public HudEditorScreen() {
        super(Component.translatable("screen.factory_hud.editor"));
    }

    @Override
    protected void init() {
        noteBox = new EditBox(
                font,
                width / 2 - 120,
                34,
                240,
                20,
                Component.translatable("screen.factory_hud.editor.note")
        );
        noteBox.setMaxLength(80);
        noteBox.visible = false;
        noteBox.active = false;
        addRenderableWidget(noteBox);

        settingsButton = Button.builder(
                Component.translatable("screen.factory_hud.editor.settings"),
                button -> {
                    settingsOpen = !settingsOpen;
                    refreshSettingsButtons();
                }
        ).bounds(width - 86, 6, 80, 20).build();
        autoDimButton = Button.builder(
                Component.empty(),
                button -> {
                    ClientHudState.Settings settings = ClientHudState.settings();
                    saveSettings(!settings.autoDimUnavailable(), settings.unavailableOpacity());
                }
        ).bounds(width - 204, 36, 194, 20).build();
        dimOpacityButton = Button.builder(
                Component.empty(),
                button -> {
                    ClientHudState.Settings settings = ClientHudState.settings();
                    saveSettings(
                            settings.autoDimUnavailable(),
                            nextDimOpacity(settings.unavailableOpacity())
                    );
                }
        ).bounds(width - 204, 60, 194, 20).build();
        refreshSettingsButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen renders its blur/background and widgets first. Cards and labels must
        // be drawn afterwards or the background shader blurs the editor itself.
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, 6, 0xFFFFFFFF);
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.factory_hud.editor.help"),
                width / 2,
                18,
                0xFFB8C8D0
        );
        if (noteBox.visible) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.factory_hud.editor.note_help"),
                    width / 2,
                    24,
                    0xFFFFFF88
            );
        }

        ClientHudState.Card hoveredCard = null;
        HudOverlay.CardBounds hoveredBounds = null;
        for (ClientHudState.Card card : ClientHudState.cards()) {
            Position position = position(card);
            HudOverlay.CardBounds bounds = HudOverlay.bounds(
                    card,
                    position.x(),
                    position.y(),
                    width,
                    height
            );
            HudOverlay.renderCard(
                    graphics,
                    card,
                    bounds,
                    card.id().equals(draggingId),
                    false
            );
            if (bounds.contains(mouseX, mouseY)) {
                hoveredCard = card;
                hoveredBounds = bounds;
            }
        }

        if (hoveredCard != null && hoveredBounds != null) {
            renderAppearanceHint(graphics, hoveredCard, hoveredBounds);
        }

        if (settingsOpen) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 600);
            graphics.fill(width - 212, 29, width - 4, 87, 0xE0101010);
            graphics.renderOutline(width - 212, 29, 208, 58, 0xFFFFD33D);
            autoDimButton.render(graphics, mouseX, mouseY, partialTick);
            dimOpacityButton.render(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 600);
        settingsButton.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (settingsButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (settingsOpen) {
            if (autoDimButton.mouseClicked(mouseX, mouseY, button)
                    || dimOpacityButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (mouseX >= width - 212 && mouseX < width - 4
                    && mouseY >= 29 && mouseY < 87) {
                return true;
            }
        }
        if (noteBox.visible && button == 0 && noteBox.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        List<ClientHudState.Card> cards = ClientHudState.cards();
        for (int i = cards.size() - 1; i >= 0; i--) {
            ClientHudState.Card card = cards.get(i);
            Position position = position(card);
            HudOverlay.CardBounds bounds = HudOverlay.bounds(
                    card,
                    position.x(),
                    position.y(),
                    width,
                    height
            );
            if (bounds.contains(mouseX, mouseY)) {
                if (button == 1) {
                    if (hasShiftDown()) {
                        deleteCard(card);
                        return true;
                    }
                    beginNoteEdit(card);
                    return true;
                }
                if (button != 0 || noteBox.visible) {
                    return true;
                }
                draggingId = card.id();
                dragOffsetX = mouseX - bounds.x();
                dragOffsetY = mouseY - bounds.y();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (noteBox.visible) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                saveNote();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeNoteEditor();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (button != 0 || draggingId == null) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        ClientHudState.Card card = findCard(draggingId);
        if (card == null) {
            draggingId = null;
            return false;
        }

        HudOverlay.CardBounds current = HudOverlay.bounds(card, 0, 0, width, height);
        float x = (float) ((mouseX - dragOffsetX) / Math.max(1.0, width - current.width()));
        float y = (float) ((mouseY - dragOffsetY) / Math.max(1.0, height - current.height()));
        previewPositions.put(draggingId, new Position(clamp(x), clamp(y)));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingId != null) {
            Position position = previewPositions.get(draggingId);
            if (position != null) {
                try {
                    UUID id = UUID.fromString(draggingId);
                    ClientHudState.updatePosition(id, position.x(), position.y());
                    PacketDistributor.sendToServer(
                            UpdateCardPositionPayload.of(id, position.x(), position.y())
                    );
                } catch (IllegalArgumentException ignored) {
                    // Snapshot IDs originate on the server; malformed values are ignored.
                }
            }
            draggingId = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (scrollY == 0.0 || noteBox.visible) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        ClientHudState.Card card = cardAt(mouseX, mouseY);
        if (card == null) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        float scale = HudOverlay.cardScale(card);
        float opacity = HudOverlay.cardOpacity(card);
        float step = scrollY > 0.0 ? 0.1F : -0.1F;
        if (hasShiftDown()) {
            opacity = roundTenth(Mth.clamp(
                    opacity + step,
                    HudBinding.MIN_OPACITY,
                    HudBinding.MAX_OPACITY
            ));
        } else {
            scale = roundTenth(Mth.clamp(
                    scale + step,
                    HudBinding.MIN_SCALE,
                    HudBinding.MAX_SCALE
            ));
        }

        try {
            UUID id = UUID.fromString(card.id());
            ClientHudState.updateAppearance(id, scale, opacity);
            PacketDistributor.sendToServer(
                    UpdateCardAppearancePayload.of(id, scale, opacity)
            );
        } catch (IllegalArgumentException ignored) {
            // Snapshot IDs originate on the server; malformed values are ignored.
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Position position(ClientHudState.Card card) {
        return previewPositions.getOrDefault(card.id(), new Position(card.hudX(), card.hudY()));
    }

    private ClientHudState.Card findCard(String id) {
        for (ClientHudState.Card card : ClientHudState.cards()) {
            if (card.id().equals(id)) {
                return card;
            }
        }
        return null;
    }

    private ClientHudState.Card cardAt(double mouseX, double mouseY) {
        List<ClientHudState.Card> cards = ClientHudState.cards();
        for (int i = cards.size() - 1; i >= 0; i--) {
            ClientHudState.Card card = cards.get(i);
            Position position = position(card);
            if (HudOverlay.bounds(card, position.x(), position.y(), width, height)
                    .contains(mouseX, mouseY)) {
                return card;
            }
        }
        return null;
    }

    private void beginNoteEdit(ClientHudState.Card card) {
        draggingId = null;
        noteEditingId = card.id();
        noteBox.setValue(card.note() == null ? "" : card.note());
        noteBox.visible = true;
        noteBox.active = true;
        setFocused(noteBox);
        noteBox.setFocused(true);
    }

    private void deleteCard(ClientHudState.Card card) {
        try {
            UUID id = UUID.fromString(card.id());
            if (card.id().equals(noteEditingId)) {
                closeNoteEditor();
            }
            previewPositions.remove(card.id());
            ClientHudState.remove(id);
            PacketDistributor.sendToServer(new DeleteCardPayload(card.id()));
        } catch (IllegalArgumentException ignored) {
            // Snapshot IDs originate on the server; malformed values are ignored.
        }
    }

    private void saveNote() {
        if (noteEditingId == null) {
            closeNoteEditor();
            return;
        }
        String note = noteBox.getValue().trim();
        try {
            UUID id = UUID.fromString(noteEditingId);
            ClientHudState.updateNote(id, note);
            PacketDistributor.sendToServer(UpdateCardNotePayload.of(id, note));
        } catch (IllegalArgumentException ignored) {
            // Snapshot IDs originate on the server; malformed values are ignored.
        }
        closeNoteEditor();
    }

    private void closeNoteEditor() {
        noteEditingId = null;
        noteBox.setFocused(false);
        noteBox.visible = false;
        noteBox.active = false;
        setFocused(null);
    }

    private void renderAppearanceHint(
            GuiGraphics graphics,
            ClientHudState.Card card,
            HudOverlay.CardBounds bounds
    ) {
        Component hint = Component.translatable(
                "screen.factory_hud.editor.appearance",
                Math.round(HudOverlay.cardScale(card) * 100),
                Math.round(HudOverlay.cardOpacity(card) * 100)
        );
        int hintWidth = font.width(hint);
        int hintX = Mth.clamp(bounds.x(), 4, Math.max(4, width - hintWidth - 4));
        int belowY = bounds.y() + bounds.height() + 4;
        int hintY = belowY + font.lineHeight <= height - 4
                ? belowY
                : Math.max(4, bounds.y() - font.lineHeight - 4);

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 550);
        graphics.fill(
                hintX - 2,
                hintY - 2,
                hintX + hintWidth + 2,
                hintY + font.lineHeight + 2,
                0xD0000000
        );
        graphics.drawString(font, hint, hintX, hintY, 0xFFFFFF55, true);
        graphics.pose().popPose();
    }

    private void saveSettings(boolean autoDimUnavailable, float unavailableOpacity) {
        ClientHudState.updateSettings(autoDimUnavailable, unavailableOpacity);
        PacketDistributor.sendToServer(
                UpdateGogglesSettingsPayload.of(autoDimUnavailable, unavailableOpacity)
        );
        refreshSettingsButtons();
    }

    private void refreshSettingsButtons() {
        if (autoDimButton == null || dimOpacityButton == null) {
            return;
        }
        ClientHudState.Settings settings = ClientHudState.settings();
        autoDimButton.setMessage(Component.translatable(
                settings.autoDimUnavailable()
                        ? "screen.factory_hud.editor.auto_dim.on"
                        : "screen.factory_hud.editor.auto_dim.off"
        ));
        dimOpacityButton.setMessage(Component.translatable(
                "screen.factory_hud.editor.dim_opacity",
                Math.round(settings.unavailableOpacity() * 100)
        ));
        dimOpacityButton.active = settings.autoDimUnavailable();
    }

    private static float nextDimOpacity(float current) {
        for (float step : DIM_OPACITY_STEPS) {
            if (step > current + 0.01F) {
                return step;
            }
        }
        return DIM_OPACITY_STEPS[0];
    }

    private static float clamp(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }

    private static float roundTenth(float value) {
        return Math.round(value * 10.0F) / 10.0F;
    }

    private record Position(float x, float y) {
    }
}
