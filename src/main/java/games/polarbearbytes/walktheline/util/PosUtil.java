package games.polarbearbytes.walktheline.util;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PosUtil {
    /**
     * When teleporting player find a safe space to teleport player
     * Block beneath player needs to not be air, block at foot and head level need to be air
     *
     * @param player The server entity representing the player
     * @param position The position we want to teleport to
     * @return The Y coordinate that we have determined to be safe
     */
    public static double findSafeYAbove(ServerPlayerEntity player, Vec3d position) {
        ServerWorld world = player.getEntityWorld();
        BlockPos.Mutable mutablePosition = new BlockPos.Mutable((int) Math.floor(position.getX()), world.getHeight(), (int) Math.floor(position.getZ()));
        int bottom = world.getBottomY();
        boolean isHeadAir = world.getBlockState(mutablePosition).isAir();
        boolean isFootAir = world.getBlockState(mutablePosition.move(Direction.DOWN)).isAir();
        boolean isBelowAir;
        boolean isBelowBedrock;

        //scan from sky downwards
        while(mutablePosition.getY() >= bottom) {
            BlockState state = world.getBlockState(mutablePosition.move(Direction.DOWN));
            isBelowAir = state.isAir();

            String name = Registries.BLOCK.getId(state.getBlock()).toString();
            isBelowBedrock = name.equals("minecraft:bedrock");

            if (!isBelowAir && isFootAir && isHeadAir && !isBelowBedrock) {
                return mutablePosition.getY() + 1;
            }
            isHeadAir = isFootAir;
            isFootAir = isBelowAir;
        }
        // Fallback to original position
        return position.getY();
    }
}
