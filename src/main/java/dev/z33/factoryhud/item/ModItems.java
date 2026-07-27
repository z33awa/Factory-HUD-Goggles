package dev.z33.factoryhud.item;

import dev.z33.factoryhud.FactoryHud;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FactoryHud.MOD_ID);

    public static final DeferredItem<FactoryGogglesItem> FACTORY_GOGGLES = ITEMS.register(
            "factory_goggles",
            () -> new FactoryGogglesItem(
                    new Item.Properties().stacksTo(1).durability(330)
            )
    );

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(FACTORY_GOGGLES);
        }
    }
}
