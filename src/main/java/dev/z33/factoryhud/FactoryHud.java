package dev.z33.factoryhud;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import dev.z33.factoryhud.item.ModCreativeTabs;
import dev.z33.factoryhud.item.FactoryGogglesItem;
import dev.z33.factoryhud.item.ModItems;
import dev.z33.factoryhud.network.ModNetworking;
import dev.z33.factoryhud.server.HudServerEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(FactoryHud.MOD_ID)
public final class FactoryHud {
    public static final String MOD_ID = "factory_hud";

    public FactoryHud(IEventBus modBus) {
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        GogglesItem.addIsWearingPredicate(player ->
                player.getItemBySlot(EquipmentSlot.HEAD).getItem()
                        instanceof FactoryGogglesItem
        );
        modBus.addListener(ModItems::addCreativeTabContents);
        modBus.addListener(ModNetworking::registerPayloads);
        NeoForge.EVENT_BUS.register(HudServerEvents.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
