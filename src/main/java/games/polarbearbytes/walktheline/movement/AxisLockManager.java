package games.polarbearbytes.walktheline.movement;

import com.mojang.datafixers.util.Pair;
import games.polarbearbytes.walktheline.WalkTheLine;
import games.polarbearbytes.walktheline.component.WTLComponents;
import games.polarbearbytes.walktheline.config.ConfigManager;
import games.polarbearbytes.walktheline.config.WalkTheLineConfig;
import games.polarbearbytes.walktheline.network.SyncPacket;
import games.polarbearbytes.walktheline.state.LockedAxisData;
import games.polarbearbytes.walktheline.util.PosUtil;
import games.polarbearbytes.walktheline.util.Utils;
import games.polarbearbytes.walktheline.util.WorldUtil;
import games.polarbearbytes.walktheline.world.StrongholdLocator;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
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
            if(!WTLComponents.playerState(player).isEnabled()) return; // return if non-enabled player
            var axisData = WTLComponents.lockedAxis(to);
            LockedAxisData lockedAxisData = axisData.getLockedAxisData(player.getUuid());
            boolean isInEnd = WorldUtil.isTheEnd(to);
            if(isInEnd) {
                WalkTheLine.LOGGER.debug("Now, player is in the end!");
                WalkTheLine.server.getPlayerManager().getPlayerList().forEach(p -> {
                    if(WorldUtil.isTheEnd(p.getEntityWorld())) return; // ignore player who is in the end
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
            syncToClient(player,to.getRegistryKey(),lockedAxisData);
        });

        /*
        Same as the dimension change event above but for when player joins, but
        we return early if we haven't any locked data (e.g., when first creating / joining a game)
         */
        ServerPlayerEvents.JOIN.register(player -> {
            if(!WTLComponents.playerState(player).isEnabled()) return;
            RegistryKey<World> worldKey = player.getEntityWorld().getRegistryKey();
            LockedAxisData lockedAxisData = WTLComponents.lockedAxis(player.getEntityWorld()).getLockedAxisData(player.getUuid());

            if(lockedAxisData == null) return;
            syncToClient(player,worldKey,lockedAxisData);
        });
    }

    /**
     * Calculate how far the player is from the locked axis and push back or teleport when necessary
     *
     * @param player The server entity representing the player
     * @param data The locked axis and coordinate data
     */
    public static boolean checkDistanceFromLockedAxis(ServerPlayerEntity player, LockedAxisData data){
        if(WorldUtil.isTheEnd(player.getEntityWorld())) return true;
        double coordinate = Utils.getPlayerCoordAlongLockedAxis(player, data.axis());
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
                    double y = PosUtil.findSafeYAbove(player, newPos);
                    entity.teleport(world, newPos.getX(), y, newPos.getZ() ,EnumSet.noneOf(PositionFlag.class),player.getYaw(),player.getPitch(),false);
                }
                case Z -> {
                    Vec3d newPos = new Vec3d(pos.getX(), pos.getY(), data.coordinate());
                    double y = PosUtil.findSafeYAbove(player, newPos);
                    entity.teleport(world, newPos.getX(), y, newPos.getZ(),EnumSet.noneOf(PositionFlag.class),player.getYaw(),player.getPitch(),false);
                }
            }
            return false;
        }
        //Entity is outside the tolerated bounds so apply a small pushback to keep them within
        applyPushback(entity,data.axis(),distance);
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
        String saveName = server.getSaveProperties().getLevelName();
        Axis axis;
        double coordinate;

        WalkTheLine.LOGGER.debug("Determining dimension lock for {} in {}", player.getName().getLiteralString(), worldKey.getValue().getPath());
        switch (worldKey.getValue().getPath()) {
            case "overworld" -> {
                //Get the overworld's spawn location
                BlockPos spawnPosition = player.getEntityWorld().getSpawnPoint().getPos();

                //Find the closest stronghold and return position and axis
                Pair<BlockPos, Direction> locationPair = StrongholdLocator.getClosestStrongHoldPortalroom(spawnPosition);
                if(locationPair == null || locationPair.getFirst() == null) return null;
                BlockPos pos = locationPair.getFirst();

                // Check if a primary axis exists for this save
                // If it does, use the opposite axis for this player
                if (isPrimary) {
                    axis = Axis.Z;
                    coordinate = pos.toCenterPos().getComponentAlongAxis(axis) + player.getRandom().nextBetween(-400, 400);
                } else {
                    axis = Axis.X;
                    coordinate = pos.toCenterPos().getComponentAlongAxis(axis);
                }

                WalkTheLine.LOGGER.info("Update {}'s axis: {} / coord: {}", player.getStringifiedName(), axis.asString(), coordinate);
            }
            case "the_nether" -> {
                ServerWorld nether = player.getEntityWorld().getServer().getWorld(World.NETHER);
                if(nether == null) return null;
                LockedAxisData data = WTLComponents.lockedAxis(server.getOverworld()).getLockedAxisData(player.getUuid());
                if(data == null) return null;
                axis = data.axis();
                coordinate = Math.floor(player.getEntityPos().getComponentAlongAxis(axis)) + 0.5d;
            }
            //the end dimension
            default -> {
                axis = Axis.Z;
                coordinate = 0.5d;
            }
        }
        LockedAxisData data = new LockedAxisData(WTLComponents.playerState(player).isEnabled(), axis, coordinate, Formatting.RED);
        syncToClient(player,worldKey,data);
        return data;
    }

    /**
     * Send a packet to the client side so it can know where to display
     * the locked axis line indicator
     *
     * @param player The server entity representing the player
     * @param worldKey The registry key for the world (dimension)
     * @param data The locked axis and coordinate data
     */
    public static void syncToClient(ServerPlayerEntity player, RegistryKey<World> worldKey, LockedAxisData data) {
        WalkTheLineConfig cfg = ConfigManager.getConfig();

        // create a packet
        SyncPacket packet = new SyncPacket(player.getUuid(), worldKey,data, cfg.coordinateTolerance);

        // broadcast to all
        player.getEntityWorld().getServer().getPlayerManager().getPlayerList().forEach(p -> {
            ServerPlayNetworking.send(p, packet);
        });
    }
}
