package com.itsfirestorm.woodcarved.util;

import com.itsfirestorm.woodcarved.crafting.CarvingRecipe;
import com.itsfirestorm.woodcarved.registries.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CarveHelper {

    private CarveHelper(){}

    private record Placement(BlockPos pos, BlockState state) {}

    /**
     * @param level -
     * @param targetPos -
     * @param originalBlock -
     * @param recipe -
     * @param player -
     * @param hand -
     * @param hitPos
     *
     * This confirms the carve action, it must confirm that the player is actually in game and if the recipe
     * the player selected is valid.
     * <br/>
     * It will then check if the block is there at all times, if the block is moved, destroyed, or otherwise altered
     * it will close the menu. The same happens if the player isn't actually holding the Carving Blade.
     * <br/>
     * It then tries to collect the block the player selected and place it.
     * If everything is successful, the carve action should finish with the result block placed in the world.
     */
    public static void carve(Level level, BlockPos targetPos, Block originalBlock,
                             RecipeHolder<CarvingRecipe> recipe, Player player, InteractionHand hand, Vec3 hitPos)
    {
        BlockState currentState = level.getBlockState(targetPos);
        if (currentState.getBlock() != originalBlock) {
            return;
        }

        ItemStack tool = player.getItemInHand(hand);
        if (!tool.is(ModItems.CARVING_BLADE.get())) {
            return;
        }

        ItemStack result = recipe.value().getResultItem(level.registryAccess());
        Block resultBlock = Block.byItem(result.getItem());
        if (resultBlock == Blocks.AIR) {
            return;
        }

        BlockState newState = buildTargetState(level, targetPos, resultBlock, currentState, player, hitPos);

        newState = resolveSurvivability(level, targetPos, newState, resultBlock, player);
        if (newState == null) {
            player.displayClientMessage(
                    Component.translatable("message.woodcarved.cannot_apply").withStyle(ChatFormatting.RED),
                    true
            );
            level.playSound(null, BlockPos.containing(player.position()), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0F, 0.2F);
            return;
        }

        if (!placeCarvedBlock(level, targetPos, newState, player)) {
            return;
        }

        level.playSound(null, targetPos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS);

        if (!player.getAbilities().instabuild) {
            tool.hurtAndBreak(1, player, player.getEquipmentSlotForItem(tool));
        }

    }

    // ==================================================================================
    // || State builders, controls the properties that carry over to the build process ||
    // ==================================================================================

    /**
     * @param resultBlock - Result Block
     * @param oldState - Old Block State
     * @param player - Player Entity
     * @return
     *
     * This function controls the properties the block will have when carved. Most importantly, rotation.
     * This of course just returns the state of the block, and will not actually try to build the block, rather,
     * it tells placeCarvedBlock() the properties the block will have.
     * <br/>
     * Controlled properties:
     * - 16-way rotation
     * - Axis
     * - Attached Face (this tells the build function that the block should be attached to an adjacent block, if possible)
     */
    private static BlockState buildTargetState(Level level, BlockPos targetPos, Block resultBlock,
                                               BlockState oldState, Player player, Vec3 hitPos) {
        BlockState newState = resultBlock.defaultBlockState();

        for (Property<?> property : oldState.getProperties()) {
            if (newState.hasProperty(property)) {
                newState = copyProperty(oldState, newState, property);
            }
        }

        if (newState.hasProperty(BlockStateProperties.ATTACH_FACE)
                && !oldState.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            newState = applyAttachFace(level, targetPos, newState, player, hitPos);
        } else if (newState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && !oldState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            BlockState playerFacingState = newState.setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection());

            Direction facing = playerFacingState.canSurvive(level, targetPos)
                ? player.getDirection()
                : resolveHorizontalFacing(level, targetPos, newState, player, hitPos);

            newState = newState.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);

            if (newState.hasProperty(BlockStateProperties.DOOR_HINGE))  {
                newState = newState.setValue(BlockStateProperties.DOOR_HINGE,
                        resolveDoorHinge(level, targetPos, facing, player, hitPos));
            }
        }

        if (newState.hasProperty(BlockStateProperties.AXIS)
                && !oldState.hasProperty(BlockStateProperties.AXIS)) {
            newState = newState.setValue(BlockStateProperties.AXIS, playerLookAxis(player));
        }

        if (newState.hasProperty(BlockStateProperties.ROTATION_16)
                && !oldState.hasProperty(BlockStateProperties.ROTATION_16)) {
            newState = newState.setValue(BlockStateProperties.ROTATION_16, rotationTowardsPlayer(targetPos, player));
        }

        if (newState.hasProperty(BlockStateProperties.HALF)
                && !oldState.hasProperty(BlockStateProperties.HALF)) {
            newState = newState.setValue(BlockStateProperties.HALF,
                    isUpperHalfClicked(targetPos, hitPos) ? Half.TOP : Half.BOTTOM);
        }

        if (newState.hasProperty(BlockStateProperties.SLAB_TYPE)
                && !oldState.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            newState = newState.setValue(BlockStateProperties.SLAB_TYPE,
                    isUpperHalfClicked(targetPos, hitPos) ? SlabType.TOP : SlabType.BOTTOM);
        }

        return newState;
    }

    private static boolean isUpperHalfClicked(BlockPos pos, Vec3 hitPos) {
        if (hitPos == null) return false;

        double localY = hitPos.y - pos.getY();
        return localY > 0.5D;
    }

    private static DoorHingeSide resolveDoorHinge(Level level, BlockPos pos, Direction facing,
                                                  Player player, Vec3 hitPos) {
        Direction leftDir = facing.getCounterClockWise();
        Direction rightDir = facing.getClockWise();

        BlockPos leftPos = pos.relative(leftDir);
        BlockPos rightPos = pos.relative(rightDir);

        boolean leftSturdy = level.getBlockState(leftPos).isFaceSturdy(level, leftPos, rightDir);
        boolean rightSturdy = level.getBlockState(rightPos).isFaceSturdy(level, rightPos, leftDir);

        if (leftSturdy && !rightSturdy) {
            return DoorHingeSide.LEFT;
        }
        if (rightSturdy && !leftSturdy) {
            return DoorHingeSide.RIGHT;
        }
        Vec3 refPoint = hitPos != null ? hitPos : player.position();
        double offsetX = refPoint.x - (pos.getX() + 0.5D);
        double offsetZ = refPoint.z - (pos.getZ() + 0.5D);
        double rightComponent = offsetX * rightDir.getStepX() + offsetZ * rightDir.getStepZ();

        return rightComponent >= 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
    }

    private static Direction resolveWallDirection(Level level, BlockPos pos, Player player, Vec3 hitPos) {
        Direction facing = player.getDirection();
        Direction leftDir = facing.getCounterClockWise();
        Direction rightDir = facing.getClockWise();

        BlockPos leftPos = pos.relative(leftDir);
        BlockPos rightPos = pos.relative(rightDir);

        boolean leftSturdy = level.getBlockState(leftPos).isFaceSturdy(level, leftPos, rightDir);
        boolean rightSturdy = level.getBlockState(rightPos).isFaceSturdy(level, rightPos, leftDir);

        if (leftSturdy && !rightSturdy) return leftDir;
        if (rightSturdy && !leftSturdy) return rightDir;
        if (!leftSturdy) return null;

        Vec3 refPoint = hitPos != null ? hitPos : player.position();
        double offsetX = refPoint.x - (pos.getX() + 0.5D);
        double offsetZ = refPoint.z - (pos.getZ() + 0.5D);
        double rightComponent = offsetX * rightDir.getStepX() + offsetZ * rightDir.getStepZ();

        return rightComponent >= 0 ? rightDir : leftDir;
    }

    private static Direction resolveHorizontalFacing(Level level, BlockPos targetPos, BlockState newState,
                                                     Player player, Vec3 hitPos) {
        Direction playerFacing = player.getDirection();

        if (newState.hasProperty(BlockStateProperties.DOOR_HINGE)) {
            return playerFacing;
        }

        BlockPos frontPos = targetPos.relative(playerFacing);
        if (level.getBlockState(frontPos).isFaceSturdy(level, frontPos, playerFacing.getOpposite())) {
            return playerFacing.getOpposite();
        }

        Direction wall = resolveWallDirection(level, targetPos, player, hitPos);
        if (wall != null) {
            return wall.getOpposite();
        }

        return playerFacing;
    }

    /**
     * @param state - Block State
     * @param player - Player Entity
     * @return
     *
     * Given the result has the property 'AttachFace' it will try to find any sort of possible support block
     * and give back the complete blockState that contains the property and where it should be placed.
     */
    private static BlockState applyAttachFace(Level level, BlockPos targetPos, BlockState state,
                                              Player player, Vec3 hitPos) {

        Direction playerFacing = player.getDirection();
        BlockPos frontPos = targetPos.relative(playerFacing);
        if (level.getBlockState(frontPos).isFaceSturdy(level, frontPos, playerFacing.getOpposite())) {
            return state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, playerFacing.getOpposite());
        }

        BlockPos belowPos = targetPos.below();
        if (level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP)) {
            return state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection());
        }

        Direction wall = resolveWallDirection(level, targetPos, player, hitPos);
        if (wall != null) {
            return state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, wall.getOpposite());
        }

        BlockPos abovePos = targetPos.above();
        if (level.getBlockState(abovePos).isFaceSturdy(level, abovePos, Direction.DOWN)) {
            return state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection());
        }

        return state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection());
    }

    /**
     * @param pos -
     * @return
     *
     * Same as above, but only controls 4 directions instead of 6. This is used for signs, for example.
     */
    private static Direction findHorizontalSupport(Level level, BlockPos pos, Player player) {
        Direction playerFacing = player.getDirection();
        BlockPos frontPos = pos.relative(playerFacing);
        if (level.getBlockState(frontPos).isFaceSturdy(level, frontPos, playerFacing.getOpposite())) {
            return playerFacing;
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir == playerFacing) continue;
            BlockPos neigborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neigborPos);
            if (neighborState.isFaceSturdy(level, neigborPos, dir.getOpposite())) {
                return dir;
            }
        }
        return null;
    }

    private static Direction.Axis playerLookAxis(Player player) {
        Vec3 look = player.getLookAngle();
        return Direction.getNearest(look.x, look.y, look.z).getAxis();
    }

    /**
     * @param pos - Block Position
     * @param player - Player Entity
     * @return
     *
     * Returns the nearest direction available computed from the player's position towards the block's center,
     * this is mainly used for signs or any sort of block that has 16-way rotation.
     */
    private static int rotationTowardsPlayer(BlockPos pos, Player player) {
        double dx = player.getX() - (pos.getX() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        float yaw = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        return Mth.floor((yaw * 16.0F / 360.F) + 0.5D) & 15;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    // ================================================================================================================
    // || Placement functions, handles the building process, as well as handling things such as multi-block results, ||
    // || correct placement of blocks that shouldn't be airborne, and block connections, such as fences.             ||
    // ================================================================================================================

    /**
     * @param pos - Block Pos
     * @param state - Block State
     * @param player - Player Entity
     * @return
     *
     * a
     */
    private static boolean placeCarvedBlock(Level level, BlockPos pos, BlockState state, Player player) {
        List<Placement> placements = new ArrayList<>();

        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos abovePos = pos.above();
            if (!level.getBlockState(abovePos).canBeReplaced()) {
                return false;
            }
            placements.add(new Placement(pos, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                    DoubleBlockHalf.LOWER)));
            placements.add(new Placement(abovePos, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                    DoubleBlockHalf.UPPER)));
        } else if (state.hasProperty(BlockStateProperties.BED_PART)) {
            // Honestly, we don't even have beds as a possible result per default, but this is more of an
            // 'if it happens, we're prepared for it' situation.
            Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : player.getDirection();
            BlockPos headPos = pos.relative(facing);
            if (!level.getBlockState(headPos).canBeReplaced()) {
                return false;
            }
            BlockState foot = state.setValue(BlockStateProperties.BED_PART, BedPart.FOOT)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                    .setValue(BlockStateProperties.OCCUPIED, false);
            BlockState head = state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                    .setValue(BlockStateProperties.OCCUPIED, false);
            placements.add(new Placement(pos, foot));
            placements.add(new Placement(headPos, head));
        } else {
            placements.add(new Placement(pos, state));
        }

        for (Placement placement : placements) {
            level.setBlock(placement.pos(), placement.state(), 3);
        }

        for (Placement placement : placements) {
            BlockPos placedPos = placement.pos();
            BlockState placedState = updateConnections(level, placedPos, player);

            if (!placedState.canSurvive(level, placedPos)) {
                level.destroyBlock(placedPos, true, player);
            }
        }
        return true;
    }

    private static BlockState resolveSurvivability(Level level, BlockPos pos, BlockState state, Block resultBlock, Player player) {
        if (state.canSurvive(level, pos)) return state;

        if (state.hasProperty(BlockStateProperties.ATTACH_FACE)
                || state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return null;

        Direction support = findHorizontalSupport(level, pos, player);
        if (support == null) return null;

        Block wallVariant = findWallVariant(resultBlock);
        if (wallVariant == null
                || !wallVariant.defaultBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return null;

        BlockState wallState = wallVariant.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, support.getOpposite());

        return wallState.canSurvive(level, pos) ? wallState : null;
    }

    private static Block findWallVariant(Block standingBlock) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(standingBlock);

        String path = id.getPath();
        if (!path.endsWith("_sign") || path.endsWith("_wall_sign")) return null;

        ResourceLocation wallId = ResourceLocation.fromNamespaceAndPath(
                id.getNamespace(), path.substring(0, path.length() - "_sign".length()) + "_wall_sign");

        Block wallBlock = BuiltInRegistries.BLOCK.get(wallId);
        return wallBlock == Blocks.AIR ? null : wallBlock;
    }

    private static BlockState updateConnections(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        BlockState updated = state;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            updated = updated.updateShape(dir, level.getBlockState(neighborPos), level, pos, neighborPos);
        }

        if (updated.isAir() && !state.isAir()) {
            level.destroyBlock(pos, true, player);
            return level.getBlockState(pos);
        }

        if (updated != state) {
            level.setBlock(pos, updated, 3);
        }

        return updated;
    }

}
