package com.ostiff.portalsconfigured.config;

import com.ostiff.portalsconfigured.PortalsConfigured;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ModConfigSpec;

public class PortalConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> PORTAL_FRAME_BLOCK;
    public static final ModConfigSpec.BooleanValue DISABLE_VANILLA_PORTAL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Portals Configured - Common Configuration").push("portal");

        PORTAL_FRAME_BLOCK = builder
                .comment(
                        "The block used to build nether portal frames.",
                        "Use the full registry name, e.g. 'kubejs:infused_obsidian' or 'minecraft:obsidian'.",
                        "Defaults to vanilla obsidian (no change to normal behaviour)."
                )
                .define("portalFrameBlock", "minecraft:obsidian");

        DISABLE_VANILLA_PORTAL = builder
                .comment(
                        "If true, vanilla obsidian portals will be blocked when a custom frame block is set.",
                        "Players will receive a message explaining the frame material is wrong."
                )
                .define("disableVanillaPortal", true);

        builder.pop();
        SPEC = builder.build();
    }

    /**
     * Returns the configured portal frame block at runtime.
     * Falls back to obsidian if the configured ID is invalid or the block is not loaded.
     */
    public static Block getPortalFrameBlock() {
        String blockId = PORTAL_FRAME_BLOCK.get();
        ResourceLocation loc = ResourceLocation.tryParse(blockId);

        if (loc == null) {
            PortalsConfigured.LOGGER.error("[PortalsConfigured] Invalid block ID in config: '{}'. Falling back to obsidian.", blockId);
            return Blocks.OBSIDIAN;
        }

        Block block = BuiltInRegistries.BLOCK.get(loc);

        if (block == Blocks.AIR) {
            PortalsConfigured.LOGGER.warn("[PortalsConfigured] Block '{}' not found in registry. Falling back to obsidian.", blockId);
            return Blocks.OBSIDIAN;
        }

        return block;
    }

    /**
     * Returns true if the configured frame block is different from vanilla obsidian.
     */
    public static boolean isUsingCustomBlock() {
        return getPortalFrameBlock() != Blocks.OBSIDIAN;
    }
}
