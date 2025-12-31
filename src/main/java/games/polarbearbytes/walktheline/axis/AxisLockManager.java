package games.polarbearbytes.walktheline.axis;

import com.mojang.datafixers.util.Pair;
import games.polarbearbytes.walktheline.WalkTheLine;
import games.polarbearbytes.walktheline.component.WTLComponents;
import games.polarbearbytes.walktheline.config.ConfigManager;
import games.polarbearbytes.walktheline.world.StrongholdLocator;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Set;

/**
 * Class for managing what axis and coordinate we are locked on per world (dimension)
 */
public class AxisLockManager {
    //The side to side tolerance for the player to move across the locked axis before being pushed back
    private static final double tolerance = ConfigManager.getConfig().coordinateTolerance;
    //The distance away from locked coordinate to do a teleport instead of doing a pushback
    private static final double teleportTolerance = ConfigManager.getConfig().teleportTolerance;

    /*
     * Register the server events we need to hook into
     */
     public static void register() {
        /*
        When the player changes world (dimension) we need to send a new sync packet to the
        client so it gets the correct locked axis and coordinate for line rendering purposes,
        along with wither or not the mod is enabled
         */
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> {
            if(!WTLComponents.playerState(player).isEnabledWithWorld()) return; // return if non-enabled player
            var axisData = WTLComponents.lockedAxis(to);
            if(axisData.getLockedAxisData(player.getUuid()) == null) {
                var data = determineDimensionLocks(player, player.getEntityWorld().getRegistryKey(), WTLComponents.lockedAxis(from).getLockedAxisData(player.getUuid()).axis() == Axis.X);
                axisData.setLockedAxisData(player.getUuid(), data);
            }
            boolean isInEnd = StrongholdLocator.WorldUtil.isTheEnd(to);
            if(isInEnd) {
                WalkTheLine.LOGGER.debug("Now, player is in the end!");
                WalkTheLine.server.getPlayerManager().getPlayerList().forEach(p -> {
                    if(StrongholdLocator.WorldUtil.isTheEnd(p.getEntityWorld())) return; // ignore player who is in the end
                    p.teleport(
                            player.getEntityWorld(),
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            Set.of(),
                            player.getYaw(),
                            player.getPitch(),
                            true
                    );
                });
            }
        });

        /*
        Same as the dimension change event above but for when player joins, but
        we return early if we haven't any locked data (e.g., when first creating / joining a game)
         */
        ServerPlayerEvents.JOIN.register(player -> {
            if(!WTLComponents.playerState(player).isEnabledWithWorld()) return;
            RegistryKey<World> worldKey = player.getEntityWorld().getRegistryKey();
            LockedAxisData lockedAxisData = WTLComponents.lockedAxis(player.getEntityWorld()).getLockedAxisData(player.getUuid());

            WTLComponents.playerState(player).setCoordTolerance(ConfigManager.getConfig().coordinateTolerance);
        });
    }

    /**
     * Calculate how far the player is from the locked axis and push back or teleport when necessary
     *
     * @param player The server entity representing the player
     * @param data The locked axis and coordinate data
     */
    public static boolean checkDistanceFromLockedAxis(ServerPlayerEntity player, LockedAxisData data){
        if(StrongholdLocator.WorldUtil.isTheEnd(player.getEntityWorld())) return true;
        double coordinate = getPlayerCoordAlongLockedAxis(player, data.axis());
        double distance = coordinate - data.coordinate();
        Entity entity;

        /*
        Check if we are riding a vehicle (boat, cart, strider, etc.)
        If so that is the entity we will apply pushback or teleport to
         */
        if (player.hasVehicle()) {
            entity = player.getVehicle();
        } else {
            entity = player;
        }
        if(entity == null) return true;
        if (Math.abs(distance) <= tolerance) {
            //We are within the bounds so no need to do anything
            return true;
        } else if(Math.abs(distance) >= teleportTolerance){
            //Past the teleport tolerance, so teleport entity back to the locked coordinate
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            Vec3d pos = entity.getEntityPos();
            player.setVelocity(0.0f,0.0f,0.0f);
            switch(data.axis()){
                case X -> {
                    Vec3d newPos = new Vec3d(data.coordinate(), pos.getY(), pos.getZ());
                    double y = StrongholdLocator.WorldUtil.findSafeYAbove(player.getEntityWorld(), newPos);
                    entity.teleport(world, newPos.getX(), y, newPos.getZ() ,EnumSet.noneOf(PositionFlag.class),player.getYaw(),player.getPitch(),false);
                }
                case Z -> {
                    Vec3d newPos = new Vec3d(pos.getX(), pos.getY(), data.coordinate());
                    double y = StrongholdLocator.WorldUtil.findSafeYAbove(player.getEntityWorld(), newPos);
                    entity.teleport(world, newPos.getX(), y, newPos.getZ(),EnumSet.noneOf(PositionFlag.class),player.getYaw(),player.getPitch(),false);
                }
            }
            return false;
        }
        //Entity is outside the tolerated bounds
        if (player.hasVehicle()) {
            // For vehicles: clamp position only, minimal velocity interference
            applyVehicleBoundaryClamping(entity, data, tolerance);
        } else {
            // For walking players: apply pushback as before
            applyPushback(entity, data.axis(), distance);
        }
        return true;
    }

    /**
     * Method for applying a pushback on the entity to keep them contained within the locked axis coordinate
     *
     * @param player The entity that is restricted to the locked axis
     * @param axis The axis they are restricted to
     * @param distance The distance away from the locked coordinate
     */
    private static void applyPushback(Entity player, Axis axis, double distance){
        Vec3d velocity = player.getVelocity();

        double overshoot = Math.abs(distance) - tolerance;
        double direction = -Math.signum(distance);
        double velocityAmount = direction * overshoot * 0.3;

        Vec3d newVelocity = switch (axis) {
            case X -> new Vec3d(velocityAmount, velocity.y, velocity.z);
            case Z -> new Vec3d(velocity.x, velocity.y, velocityAmount);
            default -> throw new IllegalStateException("Unexpected value: " + axis);
        };

        player.setVelocity(newVelocity);
        player.knockedBack = true;
    }

    /**
     * For vehicles: clamp position to boundary and zero only the perpendicular velocity component
     * that is moving away from the boundary. This preserves momentum along the allowed axis.
     *
     * @param vehicle The vehicle entity to clamp
     * @param data The locked axis data
     * @param tolerance The allowed tolerance from the locked coordinate
     */
    private static void applyVehicleBoundaryClamping(Entity vehicle, LockedAxisData data, double tolerance) {
        Axis axis = data.axis();
        double lockedCoord = data.coordinate();
        Vec3d velocity = vehicle.getVelocity();

        // Get current position before clamping (for velocity adjustment)
        double currentCoord = (axis == Axis.X) ? vehicle.getX() : vehicle.getZ();

        // Use shared position clamping logic
        data.clampEntityPosition(vehicle, tolerance);

        // Zero velocity component only if moving away from the locked coordinate
        if (axis == Axis.X) {
            if ((currentCoord > lockedCoord && velocity.x > 0) || (currentCoord < lockedCoord && velocity.x < 0)) {
                vehicle.setVelocity(0, velocity.y, velocity.z);
            }
        } else {
            if ((currentCoord > lockedCoord && velocity.z > 0) || (currentCoord < lockedCoord && velocity.z < 0)) {
                vehicle.setVelocity(velocity.x, velocity.y, 0);
            }
        }
        // Do NOT set knockedBack for vehicles to avoid interference with boat physics
    }

    /**
     * Method for getting the locked axis for the world (dimension) passed in
     * Overworld: locate stronghold and make sure the locked coordinate passes through the center of the portal frame
     * Nether: determine based on where we first teleport into the nether (probably need to tweak this for when changing portals)
     * End: hard coded as the locked axis should always be Z and on coordinate 0
     * <p>
     * If a primary axis is set for this save (by the first player who enabled the mod),
     * this player will use the opposite axis.
     *
     * @param player The server entity representing the player
     * @param worldKey The registry key for the world (dimension)
     * @return The locked axis and coordinate for that world
     */
    public static LockedAxisData determineDimensionLocks(ServerPlayerEntity player, RegistryKey<World> worldKey, boolean isPrimary) {
        MinecraftServer server = player.getEntityWorld().getServer();
        Axis axis;
        double coordinate;

        WalkTheLine.LOGGER.debug("Determining dimension lock for {} in {}", player.getName().getLiteralString(), worldKey.getValue().getPath());
        switch (worldKey.getValue().getPath()) {
            case "overworld" -> {
                // Check if a primary axis exists for this save
                // If it does, use the opposite axis for this player
                var lockedAxis = WTLComponents.lockedAxis(player.getEntityWorld());
                Vec3i crossPoint = lockedAxis.getCrossPoint();
                if(crossPoint == null) {
                    //Get the overworld's spawn location
                    BlockPos spawnPosition = player.getEntityWorld().getSpawnPoint().getPos();

                    //Find the closest stronghold and return position and axis
                    Pair<BlockPos, Direction> locationPair = StrongholdLocator.getClosestStrongHoldPortalroom(spawnPosition);
                    if(locationPair == null || locationPair.getFirst() == null) return null;
                    BlockPos pos = locationPair.getFirst();
                    crossPoint = new Vec3i(pos.getX(), 60, pos.getZ() + (player.getRandom().nextBetween(500,600) * (player.getRandom().nextBoolean() ? 1: -1)));
                    lockedAxis.setCrossPoint(crossPoint);
                }

                axis = isPrimary ? Axis.X: Axis.Z;
                coordinate = crossPoint.getComponentAlongAxis(axis) + 0.5d;

                WalkTheLine.LOGGER.info("Update {}'s axis: {} / coord: {}", player.getStringifiedName(), axis.asString(), coordinate);
            }
            case "the_nether" -> {
                ServerWorld overworld = player.getEntityWorld().getServer().getWorld(World.OVERWORLD);
                if(overworld == null) return null;
                var lockedAxis = WTLComponents.lockedAxis(overworld);
                var data = lockedAxis.getLockedAxisData(player.getUuid());
                axis = data == null ? Axis.X : data.axis();
                coordinate = Math.floor(player.getEntityPos().getComponentAlongAxis(axis)) + 0.5d;
            }
            //the end dimension
            default -> {
                axis = Axis.Z;
                coordinate = 0.5d;
            }
        }
        return new LockedAxisData(WTLComponents.playerState(player).isEnabledWithWorld(), axis, coordinate);
    }

    public static double getPlayerCoordAlongLockedAxis(PlayerEntity player, Axis axis) {
        return (axis == Axis.X) ? player.getX() : player.getZ();
    }
}
