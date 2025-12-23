package games.polarbearbytes.walktheline.util;

import games.polarbearbytes.walktheline.component.WTLComponents;
import games.polarbearbytes.walktheline.config.ConfigManager;
import games.polarbearbytes.walktheline.config.WalkTheLineConfig;
import games.polarbearbytes.walktheline.movement.AxisLockManager;
import games.polarbearbytes.walktheline.state.LockedAxisData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

/**
 * Utility functions
 */
public class Utils {
    public static double getPlayerCoordAlongLockedAxis(PlayerEntity player, Axis axis) {
        return (axis == Axis.X) ? player.getX() : player.getZ();
    }

    public static int colorHexToInt(String colorHex) {
        String[] rgba = colorHex.split("(?<=\\G.{2})");
        int r = Integer.parseInt(rgba[0], 16);
        int g = Integer.parseInt(rgba[1], 16);
        int b = Integer.parseInt(rgba[2], 16);
        int a = Integer.parseInt(rgba[3], 16);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void adjustmentPlayerPosition(ServerPlayerEntity player) {
        LockedAxisData data = WTLComponents.lockedAxis(player.getEntityWorld()).getLockedAxisData(player.getUuid());
        if (data == null) return;

        boolean result = AxisLockManager.checkDistanceFromLockedAxis(player, data);
        //result will be false if it had to move us.
        if (!result) return;

        WalkTheLineConfig cfg = ConfigManager.getConfig();

        double x = player.getX();
        double z = player.getZ();
        double xv = player.getVelocity().x;
        double zv = player.getVelocity().z;

        double coord = data.coordinate();
        double coordRange = cfg.coordinateTolerance;

        if (data.axis() == Direction.Axis.X) {
            double clampedX = clampInRange(x, coord, coordRange);

            // if it's clamped
            if(x != clampedX) {
                x = clampedX;
                xv = 0;
            }
        } else {
            double clampedZ = clampInRange(z, coord, coordRange);

            // if it's clamped
            if(z != clampedZ) {
                z = clampedZ;
                zv = 0;
            }
        }

        // set new normalized position and velocity
        player.setPosition(x, player.getY(), z);
        player.setVelocity(xv, player.getVelocity().y, zv);
    }

    private static double clampInRange(double value, double base, double range) {
        return Math.clamp(value, base - range, base + range);
    }
}
