package dev.z33.factoryhud.item;

import dev.z33.factoryhud.data.HudBindingStore;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public final class FactoryGogglesItem extends Item implements Equipable {
    public FactoryGogglesItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.factory_hud.binding_count",
                HudBindingStore.get(stack).size()
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.factory_hud.edit_hint"
        ).withStyle(style -> style.withColor(0xFFD33D).withBold(true)));
        tooltip.add(Component.translatable(
                "tooltip.factory_hud.bind_hint"
        ).withStyle(ChatFormatting.DARK_GRAY));
        if (HudBindingStore.hasFutureData(stack)) {
            tooltip.add(Component.translatable(
                    "tooltip.factory_hud.future_data"
            ).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            net.minecraft.world.InteractionHand usedHand
    ) {
        ItemStack goggles = player.getItemInHand(usedHand);
        // Do not auto-swap equipment here: in Creative it can equip a copied stack.
        // Editing is opened from the hovered inventory stack with W instead.
        return InteractionResultHolder.pass(goggles);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        HudBindingStore.ToggleResult result = HudBindingStore.toggle(
                player,
                context.getItemInHand(),
                context.getLevel().dimension().location(),
                context.getClickedPos(),
                context.getClickedFace()
        );
        Component message = switch (result) {
            case ADDED -> Component.translatable("message.factory_hud.bound")
                    .withStyle(ChatFormatting.AQUA);
            case REMOVED -> Component.translatable("message.factory_hud.unbound")
                    .withStyle(ChatFormatting.YELLOW);
            case NO_GOGGLES -> Component.translatable("message.factory_hud.hold_goggles")
                    .withStyle(ChatFormatting.RED);
            case INCOMPATIBLE_DATA -> Component.translatable(
                    "message.factory_hud.incompatible_data"
            ).withStyle(ChatFormatting.RED);
        };
        player.displayClientMessage(message, true);
        return InteractionResult.CONSUME;
    }
}
