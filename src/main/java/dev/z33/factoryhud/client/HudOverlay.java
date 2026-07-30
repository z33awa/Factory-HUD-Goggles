package dev.z33.factoryhud.client;

import dev.z33.factoryhud.FactoryHud;
import dev.z33.factoryhud.data.HudBinding;
import dev.z33.factoryhud.item.FactoryGogglesAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = FactoryHud.MOD_ID, value = Dist.CLIENT)
public final class HudOverlay {
    private static final int BACKGROUND_COLOR = 0xFF100010;
    private static final int BORDER_COLOR_TOP = 0xFFD7D7D7;
    private static final int BORDER_COLOR_BOTTOM = 0xFF898989;
    private static final int TARGET_BORDER_TOP = 0xFF55FF55;
    private static final int TARGET_BORDER_BOTTOM = 0xFF178A32;

    private HudOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null
                || minecraft.screen instanceof HudEditorScreen) {
            return;
        }
        if (!FactoryGogglesAccess.isWearing(minecraft.player)) {
            return;
        }

        List<ClientHudState.Card> cards = ClientHudState.cards();
        for (ClientHudState.Card card : cards) {
            CardBounds bounds = bounds(
                    card,
                    card.hudX(),
                    card.hudY(),
                    graphics.guiWidth(),
                    graphics.guiHeight()
            );
            renderCard(
                    graphics,
                    card,
                    bounds,
                    false,
                    BoundBlockOutline.isTargeted(card)
            );
        }
    }

    public static void renderCard(
            GuiGraphics graphics,
            ClientHudState.Card card,
            CardBounds bounds,
            boolean selected,
            boolean targeted
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        List<Component> tooltip = tooltip(card);
        float scale = cardScale(card);
        float opacity = cardOpacity(card);
        ClientHudState.Settings settings = ClientHudState.settings();
        float unavailableAlpha = !(minecraft.screen instanceof HudEditorScreen)
                && settings.autoDimUnavailable()
                && !"ok".equals(card.status())
                ? settings.unavailableOpacity()
                : 1.0F;
        float backgroundAlpha = opacity * unavailableAlpha;
        int topBorder = selected
                ? 0xFF48D7E5
                : targeted
                        ? TARGET_BORDER_TOP
                        : withOpacity(BORDER_COLOR_TOP, backgroundAlpha);
        int bottomBorder = selected
                ? 0xFF208C99
                : targeted
                        ? TARGET_BORDER_BOTTOM
                        : withOpacity(BORDER_COLOR_BOTTOM, backgroundAlpha);
        int background = withOpacity(BACKGROUND_COLOR, backgroundAlpha);

        int contentWidth = contentWidth(minecraft.font, tooltip);
        int contentHeight = contentHeight(tooltip.size());
        int cardWidth = contentWidth + 8;
        int cardHeight = contentHeight + 8;

        // This deliberately does not use the vanilla/Create tooltip renderer.
        // Tooltip enhancement mods can replace that renderer and discard custom alpha.
        graphics.pose().pushPose();
        graphics.pose().translate(bounds.x(), bounds.y(), 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.flush();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        drawCardPanel(
                graphics,
                cardWidth,
                cardHeight,
                background,
                topBorder,
                bottomBorder
        );
        drawCardText(graphics, minecraft.font, tooltip, unavailableAlpha, cardWidth);
        graphics.flush();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().popPose();

        if (targeted && !selected) {
            drawTargetBorder(graphics, bounds);
        }

        if (selected) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 450);
            graphics.drawString(
                    minecraft.font,
                    Component.translatable("screen.factory_hud.editor.dragging"),
                    bounds.x() + 2,
                    Math.max(2, bounds.y() - 10),
                    0xFF62D7E3,
                    true
            );
            graphics.pose().popPose();
        }
    }

    private static void drawTargetBorder(GuiGraphics graphics, CardBounds bounds) {
        int thickness = 2;
        int x = bounds.x();
        int y = bounds.y();
        int right = x + bounds.width();
        int bottom = y + bounds.height();

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);
        graphics.fill(x, y, right, y + thickness, TARGET_BORDER_TOP);
        graphics.fill(x, bottom - thickness, right, bottom, TARGET_BORDER_TOP);
        graphics.fill(x, y + thickness, x + thickness, bottom - thickness, TARGET_BORDER_TOP);
        graphics.fill(right - thickness, y + thickness, right, bottom - thickness, TARGET_BORDER_TOP);
        graphics.pose().popPose();
    }

    private static void drawCardPanel(
            GuiGraphics graphics,
            int width,
            int height,
            int background,
            int borderTop,
            int borderBottom
    ) {
        // One-pixel chamfered corners preserve the compact Create-style card silhouette.
        graphics.fill(1, 0, width - 1, 1, borderTop);
        graphics.fill(1, height - 1, width - 1, height, borderBottom);
        graphics.fill(0, 1, 1, height - 1, borderTop);
        graphics.fill(width - 1, 1, width, height - 1, borderBottom);
        graphics.fill(1, 1, width - 1, height - 1, background);
    }

    private static void drawCardText(
            GuiGraphics graphics,
            Font font,
            List<Component> lines,
            float alpha,
            int cardWidth
    ) {
        int textColor = withOpacity(0xFFFFFFFF, alpha);
        for (int index = 0; index < lines.size(); index++) {
            int y = index == 0 ? 4 : 16 + (index - 1) * 10;
            graphics.drawString(font, lines.get(index), 4, y, textColor, false);
        }

        if (lines.size() > 1) {
            int separator = withOpacity(BORDER_COLOR_TOP, alpha);
            graphics.fill(4, 14, Math.max(5, cardWidth - 4), 15, separator);
        }
    }

    public static CardBounds bounds(
            ClientHudState.Card card,
            float normalizedX,
            float normalizedY,
            int screenWidth,
            int screenHeight
    ) {
        Font font = Minecraft.getInstance().font;
        List<Component> tooltip = tooltip(card);
        int textWidth = contentWidth(font, tooltip);
        int textHeight = contentHeight(tooltip.size());
        float scale = cardScale(card);
        int width = Math.max(1, Math.round((textWidth + 8) * scale));
        int height = Math.max(1, Math.round((textHeight + 8) * scale));
        int x = Math.round(Mth.clamp(normalizedX, 0.0F, 1.0F) * Math.max(0, screenWidth - width));
        int y = Math.round(Mth.clamp(normalizedY, 0.0F, 1.0F) * Math.max(0, screenHeight - height));
        return new CardBounds(x, y, width, height);
    }

    private static int contentWidth(Font font, List<Component> lines) {
        int width = 0;
        for (FormattedText line : lines) {
            width = Math.max(width, font.width(line));
        }
        return width;
    }

    private static int contentHeight(int lineCount) {
        return lineCount > 1 ? 8 + (lineCount - 1) * 10 + 2 : 8;
    }

    public static float cardScale(ClientHudState.Card card) {
        float value = card.scale();
        if (!Float.isFinite(value) || value <= 0.0F) {
            return 1.0F;
        }
        return Mth.clamp(value, HudBinding.MIN_SCALE, HudBinding.MAX_SCALE);
    }

    public static float cardOpacity(ClientHudState.Card card) {
        float value = card.opacity();
        if (!Float.isFinite(value) || value <= 0.0F) {
            return 1.0F;
        }
        return Mth.clamp(value, HudBinding.MIN_OPACITY, HudBinding.MAX_OPACITY);
    }

    private static int withOpacity(int color, float opacity) {
        int alpha = color >>> 24;
        int adjustedAlpha = Mth.clamp(Math.round(alpha * opacity), 0, 255);
        return color & 0x00FFFFFF | adjustedAlpha << 24;
    }

    private static List<Component> tooltip(ClientHudState.Card card) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(safe(card.title())).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal(
                card.blockX() + ", " + card.blockY() + ", " + card.blockZ()
        ).withStyle(ChatFormatting.DARK_GRAY));

        if ("ok".equals(card.status()) && card.lines() != null) {
            for (String line : card.lines()) {
                tooltip.add(Component.literal(trim(line, 42)).withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable(
                    "hud.factory_hud.status." + safeStatus(card.status())
            ).withStyle(ChatFormatting.GOLD));
        }
        if (card.note() != null && !card.note().isBlank()) {
            tooltip.add(Component.translatable(
                    "hud.factory_hud.note",
                    trim(card.note(), 80)
            ).withStyle(ChatFormatting.YELLOW));
        }
        return tooltip;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "Linked source" : value;
    }

    private static String safeStatus(String value) {
        return switch (value == null ? "" : value) {
            case "cross_dimension", "missing_dimension", "unloaded", "missing" -> value;
            default -> "unknown";
        };
    }

    private static String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    public record CardBounds(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + height;
        }
    }
}
