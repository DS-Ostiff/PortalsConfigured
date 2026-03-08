package com.ostiff.portalsconfigured.events;

import com.ostiff.portalsconfigured.PortalsConfigured;
import com.ostiff.portalsconfigured.config.PortalConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

public class PortalEvents {
    /**
     * Intercepts right-click interactions to:
     * 1. Block vanilla obsidian portals when a custom frame block is configured.
     * 2. Activate a custom portal when flint & steel is used on the configured frame block.
     */
    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState clickedState = level.getBlockState(pos);
        ItemStack heldItem = event.getItemStack();

        // Only process flint & steel or fire charges
        boolean isIgniter = heldItem.is(Items.FLINT_AND_STEEL) || heldItem.is(Items.FIRE_CHARGE);
        if (!isIgniter) return;

        Block configuredBlock = PortalConfig.getPortalFrameBlock();
        boolean usingCustomBlock = PortalConfig.isUsingCustomBlock();
        boolean disableVanilla = PortalConfig.DISABLE_VANILLA_PORTAL.get();

        // --- Block vanilla obsidian portals if a custom block is configured ---
        if (usingCustomBlock && disableVanilla && clickedState.is(Blocks.OBSIDIAN)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(
                        Component.literal("This obsidian won't open a portal. You need a stronger frame...")
                                .withStyle(ChatFormatting.DARK_RED)
                );
            }
            return;
        }

        // --- Try to activate a custom portal on the configured frame block ---
        if (usingCustomBlock && clickedState.is(configuredBlock)) {
            event.setCanceled(true);
            if (level instanceof ServerLevel serverLevel) {
                tryActivateCustomPortal(serverLevel, pos, event.getEntity(), heldItem);
            }
        }
    }

    /**
     * Attempts to find and activate a valid portal frame around the clicked position.
     */
    private static void tryActivateCustomPortal(ServerLevel level, BlockPos clickedPos,
                                                Player player, ItemStack igniter) {

        Block frameBlock = PortalConfig.getPortalFrameBlock();
        PortalsConfigured.LOGGER.info("Trying to activate portal at {}", clickedPos);
        PortalsConfigured.LOGGER.info("Configured frame block: {}", frameBlock);

        // Search adjacent air blocks as potential portal interior positions
        for (Direction direction : Direction.values()) {
            BlockPos interiorPos = clickedPos.relative(direction);
            if (!level.getBlockState(interiorPos).isAir()) continue;

            // Try scanning a portal frame on both horizontal axes
            for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                CustomPortalShape shape = CustomPortalShape.find(level, interiorPos, axis, frameBlock);
                PortalsConfigured.LOGGER.info("Direction {} Axis {} shape: {}", direction, axis, shape);

                if (shape != null) {
                    shape.fill(level);

                    if (igniter.is(Items.FLINT_AND_STEEL)) {
                        igniter.hurtAndBreak(1, player,
                                LivingEntity.getSlotForHand(player.getUsedItemHand()));
                    } else {
                        igniter.shrink(1);
                    }

                    level.playSound(null, clickedPos, SoundEvents.PORTAL_TRIGGER,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                    return;
                }
            }
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                    Component.literal("The frame is incomplete or incorrectly shaped.")
                            .withStyle(ChatFormatting.RED)
            );
        }
    }

    /**
     * Checks both portal orientations (X and Z axis) for a valid frame at the given position.
     * Returns the first valid PortalShape found, or null if none.
     *
     * Note: PortalShapeMixin patches PortalShape to accept the configured frame block,
     * so vanilla's frame scanning logic works transparently here.
     */
    private static PortalShape findValidPortalShape(ServerLevel level, BlockPos pos) {
        // findEmptyPortalShape returns Optional<PortalShape> in 1.21.1
        java.util.Optional<PortalShape> xShape = PortalShape.findEmptyPortalShape(level, pos, net.minecraft.core.Direction.Axis.X);
        if (xShape.isPresent()) return xShape.get();

        java.util.Optional<PortalShape> zShape = PortalShape.findEmptyPortalShape(level, pos, net.minecraft.core.Direction.Axis.Z);
        if (zShape.isPresent()) return zShape.get();

        return null;
    }

    /**
     * Damages flint & steel by 1 durability, or consumes one fire charge.
     */
    private static void consumeIgniter(ItemStack igniter, Player player) {
        if (igniter.is(Items.FLINT_AND_STEEL)) {
            igniter.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        } else if (igniter.is(Items.FIRE_CHARGE)) {
            igniter.shrink(1);
        }
    }
}
