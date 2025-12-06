package games.polarbearbytes.walktheline.util;

import net.minecraft.world.World;

public class WorldUtil {
    public static boolean isTheEnd(World world) {
        return world.getRegistryKey() == World.END;
    }
}
