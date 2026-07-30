package dev.z33.factoryhud.compat.curios;

import com.simibubi.create.compat.curios.GogglesCurioRenderer;
import dev.z33.factoryhud.item.ModItems;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

final class FactoryHudCuriosClient {
    private FactoryHudCuriosClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(FactoryHudCuriosClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CuriosRendererRegistry.register(
                ModItems.FACTORY_GOGGLES.get(),
                () -> new GogglesCurioRenderer(
                        Minecraft.getInstance()
                                .getEntityModels()
                                .bakeLayer(GogglesCurioRenderer.LAYER)
                )
        ));
    }
}
