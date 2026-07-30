package dev.z33.factoryhud.compat.curios;

import dev.z33.factoryhud.item.FactoryGogglesAccess;
import dev.z33.factoryhud.item.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.CuriosApi;

public final class FactoryHudCurios {
    private FactoryHudCurios() {
    }

    public static void init(IEventBus modBus) {
        FactoryGogglesAccess.registerOptionalLocator(FactoryHudCurios::findWorn);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FactoryHudCuriosClient.init(modBus);
        }
    }

    private static ItemStack findWorn(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findFirstCurio(
                        stack -> stack.is(ModItems.FACTORY_GOGGLES.get()),
                        "head"
                ))
                .map(result -> result.stack())
                .orElse(ItemStack.EMPTY);
    }
}
