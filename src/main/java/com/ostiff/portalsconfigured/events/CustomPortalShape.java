package com.ostiff.portalsconfigured.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CustomPortalShape {
    // Valid portal sizes: width 2-21, height 3-21 (matching vanilla constraints)
    private static final int MIN_WIDTH = 2;
    private static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 21;

    private final ServerLevel level;
    private final Direction.Axis axis;          // X or Z axis the portal faces
    private final Direction rightDir;           // direction along the width
    private final Block frameBlock;
    private final BlockPos bottomLeft;          // bottom-left interior corner
    private final int width;
    private final int height;
    private final List<BlockPos> interiorBlocks; // air blocks to fill with portal

    private CustomPortalShape(ServerLevel level, Direction.Axis axis, Direction rightDir,
                              Block frameBlock, BlockPos bottomLeft,
                              int width, int height, List<BlockPos> interiorBlocks) {
        this.level = level;
        this.axis = axis;
        this.rightDir = rightDir;
        this.frameBlock = frameBlock;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
        this.interiorBlocks = interiorBlocks;
    }
    /**
     * Try to find a valid portal frame of the given frameBlock around the given interior position.
     * Returns null if no valid frame is found.
     */
    @Nullable
    public static CustomPortalShape find(ServerLevel level, BlockPos interiorPos,
                                         Direction.Axis axis, Block frameBlock) {

        // The two directions along the portal plane for this axis
        Direction rightDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction upDir = Direction.UP;

        // --- Find the bottom-left corner of the interior ---
        // Walk down to find the floor
        BlockPos current = interiorPos;
        while (current.getY() > level.getMinBuildHeight() && isAirOrPortal(level, current.below())) {
            current = current.below();
        }
        // current is now the lowest interior row

        // Walk left to find the left wall
        while (isAirOrPortal(level, current.relative(rightDir.getOpposite()))) {
            current = current.relative(rightDir.getOpposite());
        }
        BlockPos bottomLeft = current;

        // --- Measure width ---
        int width = 0;
        BlockPos widthCheck = bottomLeft;
        while (width <= MAX_WIDTH && isAirOrPortal(level, widthCheck)) {
            widthCheck = widthCheck.relative(rightDir);
            width++;
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) return null;

        // --- Measure height ---
        int height = 0;
        BlockPos heightCheck = bottomLeft;
        while (height <= MAX_HEIGHT && isAirOrPortal(level, heightCheck)) {
            heightCheck = heightCheck.relative(upDir);
            height++;
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) return null;

        // --- Validate the frame ---
        // Bottom wall
        for (int w = -1; w <= width; w++) {
            BlockPos framePos = bottomLeft.relative(rightDir, w).below();
            if (w == -1 || w == width) {
                // Corners must be frame block
                if (!isFrame(level, framePos, frameBlock)) return null;
            } else {
                if (!isFrame(level, framePos, frameBlock)) return null;
            }
        }

        // Top wall
        for (int w = -1; w <= width; w++) {
            BlockPos framePos = bottomLeft.relative(rightDir, w).above(height);
            if (!isFrame(level, framePos, frameBlock)) return null;
        }

        // Left and right walls
        for (int h = 0; h < height; h++) {
            BlockPos leftFrame = bottomLeft.relative(rightDir, -1).relative(upDir, h);
            BlockPos rightFrame = bottomLeft.relative(rightDir, width).relative(upDir, h);
            if (!isFrame(level, leftFrame, frameBlock)) return null;
            if (!isFrame(level, rightFrame, frameBlock)) return null;
        }

        // --- Collect interior blocks ---
        List<BlockPos> interiorBlocks = new ArrayList<>();
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                BlockPos interior = bottomLeft.relative(rightDir, w).relative(upDir, h);
                // Interior must be air (not already a portal, not a solid block)
                if (!isAirOrPortal(level, interior)) return null;
                interiorBlocks.add(interior.immutable());
            }
        }

        return new CustomPortalShape(level, axis, rightDir, frameBlock,
                bottomLeft, width, height, interiorBlocks);
    }

    /**
     * Fill the interior with nether portal blocks.
     */
    public void fill(ServerLevel level) {
        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState()
                .setValue(NetherPortalBlock.AXIS, axis);

        for (BlockPos pos : interiorBlocks) {
            level.setBlock(pos, portalState, 18); // 18 = no block update + no re-render
        }
    }

    private static boolean isFrame(ServerLevel level, BlockPos pos, Block frameBlock) {
        return level.getBlockState(pos).is(frameBlock);
    }

    private static boolean isAirOrPortal(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.NETHER_PORTAL);
    }

    @Override
    public String toString() {
        return String.format("CustomPortalShape{bottomLeft=%s, width=%d, height=%d, axis=%s}",
                bottomLeft, width, height, axis);
    }
}
