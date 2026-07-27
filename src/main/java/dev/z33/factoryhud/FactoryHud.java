package dev.z33.factoryhud;

import dev.z33.factoryhud.item.ModItems;
import dev.z33.factoryhud.network.ModNetworking;
import dev.z33.factoryhud.server.HudServerEvents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(FactoryHud.MOD_ID)
public final class FactoryHud {
    public static final String MOD_ID = "factory_hud";

    public FactoryHud(IEventBus modBus) {
        ModItems.register(modBus);
        modBus.addListener(ModItems::addCreativeTabContents);
        modBus.addListener(ModNetworking::registerPayloads);
        NeoForge.EVENT_BUS.register(HudServerEvents.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
