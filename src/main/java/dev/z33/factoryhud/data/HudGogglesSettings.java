package dev.z33.factoryhud.data;

import net.minecraft.util.Mth;

public record HudGogglesSettings(
        boolean autoDimUnavailable,
        float unavailableOpacity
) {
    public static final float MIN_UNAVAILABLE_OPACITY = 0.1F;
    public static final float MAX_UNAVAILABLE_OPACITY = 0.8F;
    public static final HudGogglesSettings DEFAULT =
            new HudGogglesSettings(true, 0.35F);

    public HudGogglesSettings {
        if (!Float.isFinite(unavailableOpacity)) {
            unavailableOpacity = 0.35F;
        }
        unavailableOpacity = Mth.clamp(
                unavailableOpacity,
                MIN_UNAVAILABLE_OPACITY,
                MAX_UNAVAILABLE_OPACITY
        );
    }
}
