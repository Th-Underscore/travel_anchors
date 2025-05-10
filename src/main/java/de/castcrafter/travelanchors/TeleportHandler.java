package de.castcrafter.travelanchors;

import de.castcrafter.travelanchors.TravelAnchors;
import de.castcrafter.travelanchors.config.ClientConfig;
import de.castcrafter.travelanchors.config.CommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Optional;

public class TeleportHandler {

    public static boolean anchorTeleport(Level level, Player player, @Nullable BlockPos except, @Nullable InteractionHand hand) {
        Pair<BlockPos, String> anchor = getAnchorToTeleport(level, player, except);
        return teleportPlayer(player, anchor, hand);
    }

    public static Pair<BlockPos, String> getAnchorToTeleport(Level level, Player player, @Nullable BlockPos except) {
        if (!player.isShiftKeyDown()) {
            double maxDistance = getMaxDistance(player);
            Vec3 positionVec = player.position().add(0, player.getEyeHeight(), 0);
            Optional<Pair<BlockPos, String>> anchor = TravelAnchorList.get(level).getAnchorsAround(player.position(), Math.pow(maxDistance, 2))
                    .filter(pair -> except == null || !except.equals(pair.getLeft()))
                    .filter(p -> Math.abs(getAngleRadians(positionVec, p.getLeft(), player.getYRot(), player.getXRot())) <= Math.toRadians(CommonConfig.max_angle))
                    .min((p1, p2) -> {
                        double angle1 = getAngleRadians(positionVec, p1.getLeft(), player.getYRot(), player.getXRot());
                        double angle2 = getAngleRadians(positionVec, p2.getLeft(), player.getYRot(), player.getXRot());
                        if (Math.abs(Mth.wrapDegrees(angle1 - angle2)) < 0.1) { // About 4 deg
                            double dst1sqr = positionVec.distanceToSqr(p1.getLeft().getX() + 0.5, p1.getLeft().getY() + 1, p1.getLeft().getZ() + 0.5);
                            double dst2sqr = positionVec.distanceToSqr(p2.getLeft().getX() + 0.5, p2.getLeft().getY() + 1, p2.getLeft().getZ() + 0.5);
                            double anchorDistSqr = p1.getLeft().distSqr(p2.getLeft());
                            if (Math.min(dst1sqr, dst2sqr) < anchorDistSqr * 4) {
                                return Double.compare(dst1sqr, dst2sqr);
                            }
                        }
                        return Double.compare(Math.abs(angle1), Math.abs(angle2));
                    })
                    .filter(p -> canTeleportTo(level, p.getLeft()));
            return anchor.orElse(null);
        } else {
            return null;
        }
    }
    
    // ctrlPressed parameter removed, will only use global config
    // ctrlPressed parameter removed, will only use global config
    public static boolean teleportPlayer(Player player, @Nullable Pair<BlockPos, String> anchor, @Nullable InteractionHand hand) {
        if (anchor != null) {
            if (!player.level().isClientSide) {
                Vec3 teleportVec = checkTeleport(player, anchor.getLeft().above());
                if (teleportVec == null) {
                    return false;
                }
                player.teleportTo(teleportVec.x(), teleportVec.y(), teleportVec.z());
            }
            player.fallDistance = 0; // Reset fall distance regardless of velocity setting

            if (player.getServer() != null) {
                player.getServer().tell(new TickTask(player.getServer().getTickCount() + 1, () -> {
                    if (player.isAlive()) {
                    }
                }));
            }

            if (hand != null) {
                player.swing(hand, true);
            }
            player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1F, 1F);
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("travelanchors.tp.success", anchor.getRight()), true);
            }
            return true;
        } else {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("travelanchors.tp.fail"), true);
            }
            return false;
        }
    }

    private static final double MIN_TELEPORT_DISTANCE = 2.0;
    private static final double TELEPORT_STEP_BACK = 0.5;

    public static boolean tryShortTeleport(Level level, Player player) {
        return tryShortTeleport(level, player, null);
    }

    public static boolean tryShortTeleport(Level level, Player player, @Nullable InteractionHand usedHand) {
        if (!level.isClientSide) {
            TravelAnchors.logger.warn("tryShortTeleport called on server side. This should not happen. Packet should be used.");
            return false;
        }
        
        InteractionHand hand;
        if (usedHand == null) {
            hand = player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        } else {
            hand = usedHand;
        }
        ItemStack itemStack = player.getItemInHand(hand);
        if (TeleportHandler.canPlayerTeleport(player, hand)) {
            if (TeleportHandler.canItemTeleport(player, hand) && !player.getCooldowns().isOnCooldown(itemStack.getItem())) {
                boolean invertVelocity = Keybinds.INVERT_VELOCITY_KEY.isDown();
                if (TeleportHandler.shortTeleport(level, player, hand, invertVelocity)) {
                    player.getCooldowns().addCooldown(itemStack.getItem(), CommonConfig.short_tp_cooldown);
                    return true;
                }
            }
        }
        return false;
    }

    
    public static boolean shortTeleport(Level level, Player player, InteractionHand hand, boolean invertVelocity) {
        if (!level.isClientSide) {
            TravelAnchors.logger.warn("shortTeleport called on server side. This should not happen. Packet should be used.");
            return false;
        }

        boolean clientShouldKeepVelocity = ClientConfig.keepVelocityOnTeleport;
        if (invertVelocity) {
            clientShouldKeepVelocity = !clientShouldKeepVelocity;
        }

        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        Vec3 targetSpot = null;

        // NOTE: Determine if player is targeting a nearby block to teleport through
        boolean teleportThroughBlock = false;
        Vec3 eyePos = player.getEyePosition();
        for (double rayDist = 0.5; rayDist <= MIN_TELEPORT_DISTANCE; rayDist += 0.5) { // Player is looking at a solid block within MIN_TELEPORT_DISTANCE
            Vec3 currentRayPos = eyePos.add(lookVec.scale(rayDist));
            BlockPos blockAtRay = BlockPos.containing(currentRayPos);
            BlockState stateAtRay = level.getBlockState(blockAtRay);
            if (level.isLoaded(blockAtRay) && !isBlockPassable(stateAtRay, level, blockAtRay)) {
                teleportThroughBlock = true;
                break;
            }
        }

        if (teleportThroughBlock) {
            // NOTE: Player is targeting a nearby block. Iterate forwards from MIN_TELEPORT_DISTANCE
            // to find the first valid spot *after* this block.
            for (double currentDistance = MIN_TELEPORT_DISTANCE; currentDistance <= CommonConfig.max_short_tp_distance; currentDistance += TELEPORT_STEP_BACK) {
                Vec3 candidateFeetPos = playerPos.add(lookVec.scale(currentDistance));
                Vec3 potentialSpot = getValidTeleportSpotForCandidate(level, player, candidateFeetPos);
                if (potentialSpot != null) {
                    targetSpot = potentialSpot;
                    break; 
                }
            }
        } else {
            // NOTE: No specific nearby block targeted for "teleport through", or target is too far/not solid.
            // Use original backwards iteration to find the furthest valid spot.
            for (double currentDistance = CommonConfig.max_short_tp_distance; currentDistance >= MIN_TELEPORT_DISTANCE; currentDistance -= TELEPORT_STEP_BACK) {
                Vec3 candidateFeetPos = playerPos.add(lookVec.scale(currentDistance));
                Vec3 potentialSpot = getValidTeleportSpotForCandidate(level, player, candidateFeetPos);
                if (potentialSpot != null) {
                    targetSpot = potentialSpot;
                    break;
                }
            }
        }

        if (targetSpot != null) {
            TravelAnchors.getNetwork().sendShortTeleportRequest(level, hand, clientShouldKeepVelocity);
            player.swing(hand, true);
            player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1F, 1F);
            return true;
        } else {
            return false;
        }
    }

    public static boolean performShortTeleportOnServer(Level level, Player player, InteractionHand hand, boolean keepVelocityDecision) {
        if (level.isClientSide) {
            return false;
        }

        if (!TeleportHandler.canPlayerTeleport(player, hand) || !TeleportHandler.canItemTeleport(player, hand) || player.getCooldowns().isOnCooldown(player.getItemInHand(hand).getItem())) {
            return false;
        }

        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position(); // Player's feet position
        Vec3 finalTeleportVec = null;

        boolean teleportThroughBlock = false;
        Vec3 eyePos = player.getEyePosition();
        for (double rayDist = 0.5; rayDist <= MIN_TELEPORT_DISTANCE; rayDist += 0.5) { // Player is looking at a solid block within MIN_TELEPORT_DISTANCE
            Vec3 currentRayPos = eyePos.add(lookVec.scale(rayDist));
            BlockPos blockAtRay = BlockPos.containing(currentRayPos);
            BlockState stateAtRay = level.getBlockState(blockAtRay);
            if (level.isLoaded(blockAtRay) && !isBlockPassable(stateAtRay, level, blockAtRay)) {
                teleportThroughBlock = true;
                break;
            }
        }

        if (teleportThroughBlock) {
            for (double currentDistance = MIN_TELEPORT_DISTANCE; currentDistance <= CommonConfig.max_short_tp_distance; currentDistance += TELEPORT_STEP_BACK) {
                Vec3 candidateFeetPos = playerPos.add(lookVec.scale(currentDistance));
                Vec3 potentialSpot = getValidTeleportSpotForCandidate(level, player, candidateFeetPos);
                if (potentialSpot != null) {
                    finalTeleportVec = potentialSpot;
                    break;
                }
            }
        } else {
            for (double currentDistance = CommonConfig.max_short_tp_distance; currentDistance >= MIN_TELEPORT_DISTANCE; currentDistance -= TELEPORT_STEP_BACK) {
                Vec3 candidateFeetPos = playerPos.add(lookVec.scale(currentDistance));
                Vec3 potentialSpot = getValidTeleportSpotForCandidate(level, player, candidateFeetPos);
                if (potentialSpot != null) {
                    finalTeleportVec = potentialSpot;
                    break;
                }
            }
        }

        if (finalTeleportVec != null) {
            Vec3 velocityBefore = player.getDeltaMovement();
            Vec3 oldVelocityToRestore = velocityBefore;
            
            player.teleportTo(finalTeleportVec.x(), finalTeleportVec.y(), finalTeleportVec.z());
            player.fallDistance = 0;

            if (player.getServer() != null) {
                final Vec3 finalOldVelocityToRestore = oldVelocityToRestore;
                if (keepVelocityDecision) {
                    player.setDeltaMovement(finalOldVelocityToRestore);
                } else {
                    player.setDeltaMovement(Vec3.ZERO);
                }
                player.hurtMarked = true; // Tell client to update velocity
            }
            return true;
        } else {
            player.displayClientMessage(Component.translatable("travelanchors.hop.fail"), true);
            return false;
        }
    }
    
    @Nullable
    private static Vec3 getValidTeleportSpotForCandidate(Level level, Player player, Vec3 candidateFeetPos) {
        double playerHeight = player.getBbHeight();
        double playerWidth = player.getBbWidth();

        // Step 1: Check if player can occupy the candidateFeetPos (feet and head space are non-solid)
        // This ensures that if the direct line of sight lands inside a block, it's considered invalid,
        // allowing the forward-iterating loop in shortTeleport to find a spot *after* the obstruction.
        BlockPos initialFeetBlockPos = BlockPos.containing(candidateFeetPos.x, candidateFeetPos.y, candidateFeetPos.z);
        BlockPos initialHeadBlockPos = BlockPos.containing(candidateFeetPos.x, candidateFeetPos.y + playerHeight - 0.01, candidateFeetPos.z); // Check just below top of BB

        BlockState initialFeetBlockState = level.getBlockState(initialFeetBlockPos);
        BlockState initialHeadBlockState = level.getBlockState(initialHeadBlockPos);

        // Player must be in air or fluid at feet and head level
        if (!isBlockPassable(initialFeetBlockState, level, initialFeetBlockPos) || !isBlockPassable(initialHeadBlockState, level, initialHeadBlockPos)) {
            return null; // Candidate position itself is obstructed
        }

        // Step 2: Apply .5 Snapping for X/Z Axes (Wall Avoidance)
        double snappedX = candidateFeetPos.x;
        double snappedZ = candidateFeetPos.z;

        // X-axis snapping
        BlockPos xCheckBlock = BlockPos.containing(candidateFeetPos.x, candidateFeetPos.y + playerHeight / 2, candidateFeetPos.z);
        BlockState xCheckBlockState = level.getBlockState(xCheckBlock);
        if (!isBlockPassable(xCheckBlockState, level, xCheckBlock)) {
            if (candidateFeetPos.x >= xCheckBlock.getX() && candidateFeetPos.x < xCheckBlock.getX() + 1) {
                snappedX = xCheckBlock.getX() + 0.5;
            }
        }

        // Z-axis snapping
        BlockPos zCheckBlock = BlockPos.containing(candidateFeetPos.x, candidateFeetPos.y + playerHeight / 2, candidateFeetPos.z);
        BlockState zCheckBlockState = level.getBlockState(zCheckBlock);
        if (!isBlockPassable(zCheckBlockState, level, zCheckBlock)) {
            if (candidateFeetPos.z >= zCheckBlock.getZ() && candidateFeetPos.z < zCheckBlock.getZ() + 1) {
                snappedZ = zCheckBlock.getZ() + 0.5;
            }
        }
        
        Vec3 currentAdjustedPos = new Vec3(snappedX, candidateFeetPos.y, snappedZ);

        // Step 3: Final Standable & Collision Check for the currentAdjustedPos
        if (currentAdjustedPos.y < level.getMinBuildHeight() + 1) {
            return null;
        }

        BlockPos finalBlockBelowFeet = BlockPos.containing(currentAdjustedPos.x, currentAdjustedPos.y - 0.01, currentAdjustedPos.z);
        if (!canTeleportTo(level, finalBlockBelowFeet)) { // Checks for solid ground below final pos and clear head/body space above it
            // Step 4: Y-axis Snapping
            // Check if the block right above the final x/z snapped position is a valid spot.
            // If so, move the teleport spot to the block above.
            BlockPos blockUnderUpperPos = BlockPos.containing(currentAdjustedPos.x(), currentAdjustedPos.y(), currentAdjustedPos.z());
            if (canTeleportTo(level, blockUnderUpperPos)) {
                currentAdjustedPos = new Vec3(currentAdjustedPos.x(), currentAdjustedPos.y() + 1, currentAdjustedPos.z());
            } else {
                return null;
            }
        }
        
        // Final AABB collision check at the (potentially X/Z/Y snapped) position
        net.minecraft.world.phys.AABB finalPlayerAABB = new net.minecraft.world.phys.AABB(
            currentAdjustedPos.x - playerWidth / 2, currentAdjustedPos.y, currentAdjustedPos.z - playerWidth / 2,
            currentAdjustedPos.x + playerWidth / 2, currentAdjustedPos.y + playerHeight, currentAdjustedPos.z + playerWidth / 2
        );

        if (!level.noCollision(player, finalPlayerAABB)) {
            return null;
        }

        // Step 5: Event Firing
        if (CommonConfig.fireTeleportEvent) {
            EntityTeleportEvent event = new EntityTeleportEvent(player, currentAdjustedPos.x, currentAdjustedPos.y, currentAdjustedPos.z);
            if (MinecraftForge.EVENT_BUS.post(event)) {
                return null;
            }
            return new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        } else {
            return currentAdjustedPos;
        }
    }

    // NOTE: New method to check if a block is passable for teleportation raycast
    private static boolean isBlockPassable(BlockState blockState, BlockGetter world, BlockPos pos) {
        // 1. Fluids, grass, flowers, air (these have an empty collision shape)
        if (blockState.getCollisionShape(world, pos).isEmpty()) {
            return true;
        }
        // 2. Ladders
        if (blockState.getBlock() instanceof LadderBlock) {
            return true;
        }
        // 3. Other blocks player can be inside (e.g., vines, cobwebs)
        //    Could add more specific checks here if needed, e.g., using BlockTags.CLIMBABLE
        //    or checking for specific block instances like CobwebBlock.
        //    For now, isEmpty and LadderBlock cover common cases.
        return false;
    }
    
    public static boolean canTeleportTo(BlockGetter level, BlockPos target) {
        BlockPos posAbove1 = target.immutable().above(1);
        BlockPos posAbove2 = target.immutable().above(2);
        return isBlockPassable(level.getBlockState(posAbove1), level, posAbove1)
                && isBlockPassable(level.getBlockState(posAbove2), level, posAbove2)
                && target.getY() >= level.getMinBuildHeight();
    }

    public static boolean canPlayerTeleportAnyHand(Player player) {
        return canPlayerTeleport(player, InteractionHand.MAIN_HAND) || canPlayerTeleport(player, InteractionHand.OFF_HAND);
    }
    
    public static boolean canPlayerTeleport(Player player, InteractionHand hand) {
        return canItemTeleport(player, hand) || canBlockTeleport(player);
    }

    public static boolean canBlockTeleport(Player player) {
        return (player.level().getBlockState(player.blockPosition().immutable().below()).getBlock() == ModBlocks.travelAnchor
                && !player.isShiftKeyDown());
    }

    public static boolean canItemTeleport(Player player, InteractionHand hand) {
        return player.getItemInHand(hand).getItem() == ModItems.travelStaff
                || player.getItemInHand(hand).getEnchantmentLevel(ModEnchantments.teleportation) >= 1;
    }

    private static double getAngleRadians(Vec3 positionVec, BlockPos anchor, float yRot, float xRot) {
        Vec3 blockVec = new Vec3(anchor.getX() + 0.5 - positionVec.x, anchor.getY() + 1.0 - positionVec.y, anchor.getZ() + 0.5 - positionVec.z).normalize();
        Vec3 lookVec = Vec3.directionFromRotation(xRot, yRot).normalize();
        return Math.acos(lookVec.dot(blockVec));
    }

    public static double getMaxDistance(Player player) {
        int mainHandLevel = player.getItemInHand(InteractionHand.MAIN_HAND).getEnchantmentLevel(ModEnchantments.range);
        int offHandLevel = player.getItemInHand(InteractionHand.OFF_HAND).getEnchantmentLevel(ModEnchantments.range);
        int lvl = Math.max(mainHandLevel, offHandLevel);
        return CommonConfig.max_distance * (1 + (lvl / 2d));
    }
    
    public static boolean canElevate(Player player) {
        return player.level().getBlockState(player.blockPosition().immutable().below()).getBlock() == ModBlocks.travelAnchor;
    }
    
    public static boolean elevateUp(Player player) {
        if (!canElevate(player)) {
            return false;
        }
        Level level = player.level();
        BlockPos.MutableBlockPos searchPos = player.blockPosition().immutable().mutable();
        while (!level.isOutsideBuildHeight(searchPos) && (level.getBlockState(searchPos).getBlock() != ModBlocks.travelAnchor || !canTeleportTo(level, searchPos))) {
            searchPos.move(Direction.UP);
        }
        BlockState state = level.getBlockState(searchPos);
        Pair<BlockPos, String> anchor = null;
        if (state.getBlock() == ModBlocks.travelAnchor && canTeleportTo(level, searchPos)) {
            BlockPos target = searchPos.immutable();
            String name = ModBlocks.travelAnchor.getBlockEntity(level, target).getName();
            if (!name.isEmpty()) {
                anchor = Pair.of(target, name);
            }
        }
        return teleportPlayer(player, anchor, null);
    }
    
    public static boolean elevateDown(Player player) {
        if (!canElevate(player)) {
            return false;
        }
        Level level = player.level();
        BlockPos.MutableBlockPos searchPos = player.blockPosition().immutable().below(2).mutable();
        while (!level.isOutsideBuildHeight(searchPos) && (level.getBlockState(searchPos).getBlock() != ModBlocks.travelAnchor || !canTeleportTo(level, searchPos))) {
            searchPos.move(Direction.DOWN);
        }
        BlockState state = level.getBlockState(searchPos);
        Pair<BlockPos, String> anchor = null;
        if (state.getBlock() == ModBlocks.travelAnchor && canTeleportTo(level, searchPos)) {
            BlockPos target = searchPos.immutable();
            String name = ModBlocks.travelAnchor.getBlockEntity(level, target).getName();
            if (!name.isEmpty()) {
                anchor = Pair.of(target, name);
            }
        }
        return teleportPlayer(player, anchor, null);
    }
    
    @Nullable
    private static Vec3 checkTeleport(Player player, BlockPos target) {
        if (CommonConfig.fireTeleportEvent) {
            EntityTeleportEvent event = new EntityTeleportEvent(player, target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            if (MinecraftForge.EVENT_BUS.post(event)) {
                return null;
            }
            return new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        } else {
            return new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        }
    }
}
