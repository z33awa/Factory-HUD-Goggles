package dev.z33.factoryhud.client;

import dev.z33.factoryhud.FactoryHud;
import dev.z33.factoryhud.item.FactoryGogglesAccess;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = FactoryHud.MOD_ID, value = Dist.CLIENT)
public final class BoundBlockOutline {
    private static final Object OUTLINE_KEY = new Object();
    private static final int BOUND_TARGET_COLOR = 0x55FF55;
    private static final float CREATE_LINK_LINE_WIDTH = 1.0F / 16.0F;
    private static volatile String targetedCardId;

    private BoundBlockOutline() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        targetedCardId = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.screen != null
                || minecraft.options.hideGui
                || !FactoryGogglesAccess.isWearing(minecraft.player)
                || !(minecraft.hitResult instanceof BlockHitResult blockHit)
                || blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        String dimension = minecraft.level.dimension().location().toString();
        ClientHudState.Card targeted = findBoundCard(dimension, pos);
        if (targeted == null) {
            return;
        }
        targetedCardId = targeted.id();

        VoxelShape shape = minecraft.level.getBlockState(pos)
                .getShape(minecraft.level, pos);
        AABB bounds = shape.isEmpty()
                ? new AABB(pos)
                : shape.bounds().move(pos);
        Outliner.getInstance()
                .showAABB(OUTLINE_KEY, bounds)
                .colored(BOUND_TARGET_COLOR)
                .lineWidth(CREATE_LINK_LINE_WIDTH);
    }

    public static boolean isTargeted(ClientHudState.Card card) {
        return card != null && card.id().equals(targetedCardId);
    }

    private static ClientHudState.Card findBoundCard(String dimension, BlockPos pos) {
        for (ClientHudState.Card card : ClientHudState.cards()) {
            if (dimension.equals(card.dimension())
                    && card.blockX() == pos.getX()
                    && card.blockY() == pos.getY()
                    && card.blockZ() == pos.getZ()) {
                return card;
            }
        }
        return null;
    }
}
