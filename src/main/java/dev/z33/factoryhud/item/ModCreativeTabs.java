package dev.z33.factoryhud.item;

import dev.z33.factoryhud.FactoryHud;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FactoryHud.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.factory_hud"))
                    .icon(() -> new ItemStack(ModItems.CREATIVE_TAB_ICON.get()))
                    .displayItems((parameters, output) -> output.accept(ModItems.FACTORY_GOGGLES.get()))
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
