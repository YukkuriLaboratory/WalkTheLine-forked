package games.polarbearbytes.walktheline.util;

import games.polarbearbytes.walktheline.config.ConfigManager;
import games.polarbearbytes.walktheline.config.WalkTheLineConfig;
import games.polarbearbytes.walktheline.movement.AxisLockManager;
import games.polarbearbytes.walktheline.state.LockedAxisData;
import games.polarbearbytes.walktheline.state.PlayerState;
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
        LockedAxisData data = PlayerState.get().getLockedAxisData(player);
        if (data == null) return;
        boolean result = AxisLockManager.checkDistanceFromLockedAxis(player, data);
        //result will be false if it had to move us.
        if (!result) return;

        WalkTheLineConfig cfg = ConfigManager.getConfig();

        double x = player.getX();
        double z = player.getZ();
        double xv = player.getVelocity().x;
        double zv = player.getVelocity().z;

        if (data.axis() == Direction.Axis.X) {
            if (x > data.coordinate() + cfg.coordinateTolerance) {
                x = data.coordinate() + cfg.coordinateTolerance;
                xv = 0;
            }
            if (x < data.coordinate() - cfg.coordinateTolerance) {
                x = data.coordinate() - cfg.coordinateTolerance;
                xv = 0;
            }

        } else {
            if (z > data.coordinate() + cfg.coordinateTolerance) {
                z = data.coordinate() + cfg.coordinateTolerance;
                zv = 0;
            }
            if (z < data.coordinate() - cfg.coordinateTolerance) {
                z = data.coordinate() - cfg.coordinateTolerance;
                zv = 0;
            }
        }

        player.setPosition(x, player.getY(), z);
        player.setVelocity(xv, player.getVelocity().y, zv);
    }
}
