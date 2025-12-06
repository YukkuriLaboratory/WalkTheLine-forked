package games.polarbearbytes.walktheline.util;

import net.minecraft.world.World;

public class WorldUtil {
    public static boolean isTheEnd(World world) {
        return "the_end".equals(world.getRegistryKey().getValue().getPath());
    }
}
