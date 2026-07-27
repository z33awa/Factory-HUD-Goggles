package dev.z33.factoryhud.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.mojang.logging.LogUtils;
import dev.z33.factoryhud.data.HudBinding;
import dev.z33.factoryhud.data.HudBindingStore;
import dev.z33.factoryhud.data.HudGogglesSettings;
import dev.z33.factoryhud.network.HudSnapshotPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

public final class HudSnapshotService {
    private static final int CARDS_PER_PACKET = 64;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Class<?>> WARNED_TOOLTIP_SOURCES = ConcurrentHashMap.newKeySet();

    private HudSnapshotService() {
    }

    public static void send(ServerPlayer player, ItemStack goggles) {
        List<HudBinding> bindings = HudBindingStore.get(goggles);
        HudGogglesSettings settings = HudBindingStore.getSettings(goggles);
        if (bindings.isEmpty()) {
            sendBatch(player, new JsonArray(), true, settings);
            return;
        }

        for (int start = 0; start < bindings.size(); start += CARDS_PER_PACKET) {
            JsonArray cards = new JsonArray();
            int end = Math.min(start + CARDS_PER_PACKET, bindings.size());
            for (int i = start; i < end; i++) {
                cards.add(sample(player, bindings.get(i)));
            }
            sendBatch(player, cards, start == 0, settings);
        }
    }

    private static void sendBatch(
            ServerPlayer player,
            JsonArray cards,
            boolean reset,
            HudGogglesSettings settings
    ) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("reset", reset);
        envelope.addProperty("autoDimUnavailable", settings.autoDimUnavailable());
        envelope.addProperty("unavailableOpacity", settings.unavailableOpacity());
        envelope.add("cards", cards);
        PacketDistributor.sendToPlayer(player, new HudSnapshotPayload(envelope.toString()));
    }

    private static JsonObject sample(ServerPlayer player, HudBinding binding) {
        JsonObject card = baseCard(binding);
        if (!player.level().dimension().location().equals(binding.dimension())) {
            card.addProperty("status", "cross_dimension");
            return card;
        }

        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, binding.dimension());
        ServerLevel level = player.server.getLevel(levelKey);
        if (level == null) {
            card.addProperty("status", "missing_dimension");
            return card;
        }

        BlockPos pos = binding.pos();
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) {
            card.addProperty("status", "unloaded");
            return card;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            card.addProperty("status", "missing");
            return card;
        }

        card.addProperty("status", "ok");
        card.addProperty("title", state.getBlock().getName().getString());
        JsonArray lines = new JsonArray();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof StressGaugeBlockEntity stressGauge) {
            List<Component> tooltip = new ArrayList<>();
            addStressGaugeTooltip(tooltip, stressGauge);
            appendTooltipLines(lines, tooltip);
        } else if (blockEntity instanceof IHaveGoggleInformation source) {
            List<Component> tooltip = new ArrayList<>();
            try {
                source.addToGoggleTooltip(tooltip, false);
            } catch (RuntimeException exception) {
                if (WARNED_TOOLTIP_SOURCES.add(blockEntity.getClass())) {
                    LOGGER.warn(
                            "HUD data source {} threw while building its goggle tooltip; using any lines produced before the error",
                            blockEntity.getClass().getName(),
                            exception
                    );
                }
            }
            appendTooltipLines(lines, tooltip);
        }

        if (lines.isEmpty() && blockEntity instanceof Container container) {
            int items = 0;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                items += container.getItem(slot).getCount();
            }
            lines.add("Items: " + items);
            lines.add("Slots: " + container.getContainerSize());
        }

        if (lines.isEmpty()) {
            lines.add("Redstone: " + level.getBestNeighborSignal(pos));
            if (blockEntity != null) {
                lines.add(blockEntity.getType().toString());
            }
        }

        card.add("lines", lines);
        return card;
    }

    private static void appendTooltipLines(JsonArray lines, List<Component> tooltip) {
        for (Component component : tooltip) {
            String text = component.getString().trim();
            if (!text.isEmpty()) {
                lines.add(text);
            }
        }
    }

    /**
     * Create's stress gauge tooltip ends by sending a client-to-server observation packet.
     * HUD snapshots are sampled on the server, so reproduce the data-only part here instead
     * of calling that client-only path.
     */
    private static boolean addStressGaugeTooltip(List<Component> tooltip, StressGaugeBlockEntity gauge) {
        if (!StressImpact.isEnabled()) {
            return false;
        }

        CreateLang.translate("gui.gauge.info_header").forGoggles(tooltip);
        double capacity = gauge.getNetworkCapacity();
        double stress = gauge.getNetworkStress();
        double stressFraction = stress / (capacity == 0 ? 1 : capacity);

        CreateLang.translate("gui.stressometer.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        if (gauge.getTheoreticalSpeed() == 0) {
            CreateLang.text(TooltipHelper.makeProgressBar(3, 0))
                    .translate("gui.stressometer.no_rotation")
                    .style(ChatFormatting.DARK_GRAY)
                    .forGoggles(tooltip);
            return true;
        }

        StressImpact.getFormattedStressText(stressFraction).forGoggles(tooltip);
        CreateLang.translate("gui.stressometer.capacity")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        double remainingCapacity = capacity - stress;
        LangBuilder stressUnit = CreateLang.translate("generic.unit.stress");
        LangBuilder stressTip = CreateLang.number(remainingCapacity)
                .add(stressUnit)
                .style(StressImpact.of(stressFraction).getRelativeColor());
        if (remainingCapacity != capacity) {
            stressTip.text(ChatFormatting.GRAY, " / ")
                    .add(CreateLang.number(capacity)
                            .add(stressUnit)
                            .style(ChatFormatting.DARK_GRAY));
        }
        stressTip.forGoggles(tooltip, 1);
        return true;
    }

    private static JsonObject baseCard(HudBinding binding) {
        JsonObject card = new JsonObject();
        card.addProperty("id", binding.id().toString());
        card.addProperty("dimension", binding.dimension().toString());
        card.addProperty("blockX", binding.pos().getX());
        card.addProperty("blockY", binding.pos().getY());
        card.addProperty("blockZ", binding.pos().getZ());
        card.addProperty("hudX", binding.hudX());
        card.addProperty("hudY", binding.hudY());
        card.addProperty("note", binding.note());
        card.addProperty("scale", binding.scale());
        card.addProperty("opacity", binding.opacity());
        card.addProperty("title", "Linked source");
        card.add("lines", new JsonArray());
        return card;
    }
}
