package com.ostiff.portalsconfigured.mixin;

import com.ostiff.portalsconfigured.config.PortalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PortalForcer.class)
public class PortalForcerMixin {

    /**
     * Redirects all Blocks.OBSIDIAN.defaultBlockState() calls inside createPortal()
     * to use our configured frame block instead.
     */
    @Redirect(
            method = "createPortal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private BlockState redirectObsidianFrame(net.minecraft.world.level.block.Block block) {
        // Only swap obsidian — leave air and nether portal blocks untouched
        if (block == Blocks.OBSIDIAN) {
            return PortalConfig.getPortalFrameBlock().defaultBlockState();
        }
        return block.defaultBlockState();
    }
}
