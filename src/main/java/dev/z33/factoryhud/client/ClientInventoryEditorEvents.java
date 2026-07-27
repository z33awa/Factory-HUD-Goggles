package dev.z33.factoryhud.client;

import dev.z33.factoryhud.FactoryHud;
import dev.z33.factoryhud.item.ModItems;
import dev.z33.factoryhud.network.OpenGogglesEditorRequestPayload;
import net.createmod.ponder.enums.PonderKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = FactoryHud.MOD_ID, value = Dist.CLIENT)
public final class ClientInventoryEditorEvents {
    private static boolean wWasDown;
    private static boolean hoveredGogglesThisFrame;

    private ClientInventoryEditorEvents() {
    }

    @SubscribeEvent
    public static void beforeScreenRender(ScreenEvent.Render.Pre event) {
        hoveredGogglesThisFrame = false;
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(ModItems.FACTORY_GOGGLES.get())) {
            hoveredGogglesThisFrame = true;
        }
    }

    @SubscribeEvent
    public static void afterScreenRender(ScreenEvent.Render.Post event) {
        boolean wDown = PonderKeybinds.PONDER.isDown();
        if (!wDown) {
            wWasDown = false;
            return;
        }
        if (wWasDown || !hoveredGogglesThisFrame) {
            return;
        }
        wWasDown = true;

        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || minecraft.player == null) {
            return;
        }
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || !slot.getItem().is(ModItems.FACTORY_GOGGLES.get())) {
            return;
        }
        OpenGogglesEditorRequestPayload request = target(minecraft, screen, slot);
        if (request == null) {
            return;
        }
        PacketDistributor.sendToServer(request);
    }

    private static OpenGogglesEditorRequestPayload target(
            Minecraft minecraft,
            AbstractContainerScreen<?> screen,
            Slot hoveredSlot
    ) {
        if (hoveredSlot.container == minecraft.player.getInventory()) {
            int direct = hoveredSlot.getContainerSlot();
            if (direct >= 0 && direct < minecraft.player.getInventory().getContainerSize()) {
                return OpenGogglesEditorRequestPayload.inventory(direct);
            }
        }

        if (screen instanceof CreativeModeInventoryScreen) {
            ItemStack hovered = hoveredSlot.getItem();
            for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
                ItemStack candidate = minecraft.player.getInventory().getItem(slot);
                if (ItemStack.isSameItemSameComponents(candidate, hovered)) {
                    return OpenGogglesEditorRequestPayload.inventory(slot);
                }
            }
            return null;
        }

        return OpenGogglesEditorRequestPayload.menu(
                screen.getMenu().containerId,
                hoveredSlot.index
        );
    }
}
