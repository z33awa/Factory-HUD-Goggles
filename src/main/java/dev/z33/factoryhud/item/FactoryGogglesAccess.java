package dev.z33.factoryhud.item;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Resolves the currently worn factory goggles without making common code depend
 * directly on optional equipment APIs.
 */
public final class FactoryGogglesAccess {
    private static final List<Function<LivingEntity, ItemStack>> OPTIONAL_LOCATORS =
            new CopyOnWriteArrayList<>();

    private FactoryGogglesAccess() {
    }

    public static void registerOptionalLocator(Function<LivingEntity, ItemStack> locator) {
        OPTIONAL_LOCATORS.add(locator);
    }

    public static ItemStack findWorn(LivingEntity entity) {
        ItemStack headStack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (headStack.is(ModItems.FACTORY_GOGGLES.get())) {
            return headStack;
        }
        for (Function<LivingEntity, ItemStack> locator : OPTIONAL_LOCATORS) {
            ItemStack stack = locator.apply(entity);
            if (stack.is(ModItems.FACTORY_GOGGLES.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isWearing(LivingEntity entity) {
        return !findWorn(entity).isEmpty();
    }
}
