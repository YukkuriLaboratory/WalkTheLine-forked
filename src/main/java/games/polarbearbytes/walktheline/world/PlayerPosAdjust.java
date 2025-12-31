package games.polarbearbytes.walktheline.world;

import games.polarbearbytes.walktheline.axis.AxisLockManager;
import games.polarbearbytes.walktheline.axis.LockedAxisData;
import games.polarbearbytes.walktheline.component.WTLComponents;
import games.polarbearbytes.walktheline.config.ConfigManager;
import games.polarbearbytes.walktheline.config.WalkTheLineConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Direction;

public class PlayerPosAdjust {
    public static void apply(ServerPlayerEntity player) {
        LockedAxisData data = WTLComponents.lockedAxis(player.getEntityWorld()).getLockedAxisData(player.getUuid());
        if (data == null) return;

        boolean result = AxisLockManager.checkDistanceFromLockedAxis(player, data);
        //result will be false if it had to move us.
        if (!result) return;

        // For vehicles: AxisLockManager handles clamping, skip velocity reset here
        if (player.hasVehicle()) return;

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
