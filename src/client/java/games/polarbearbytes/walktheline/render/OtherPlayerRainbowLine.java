package games.polarbearbytes.walktheline.render;

import games.polarbearbytes.walktheline.WalkTheLineClient;
import games.polarbearbytes.walktheline.state.LockedAxisData;
import games.polarbearbytes.walktheline.util.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class OtherPlayerRainbowLine extends RainbowLine{
    public static final OtherPlayerRainbowLine INSTANCE = new OtherPlayerRainbowLine();
    public OtherPlayerRainbowLine() {
        super();
    }

    @Override
    public void render(Vec3d cameraPos, Entity entity, MinecraftClient client) {
        if(client.world == null) return;
        LockedAxisData lockedAxisData = WalkTheLineClient.otherPlayerAxisData.get(client.world.getRegistryKey());
        if(lockedAxisData == null) return;
        if(Math.abs(Utils.getPlayerCoordAlongLockedAxis(client.player, lockedAxisData.axis()) - lockedAxisData.coordinate()) > 20) return;
        renderRainbowLine(cameraPos, entity, lockedAxisData, client.options.getViewDistance().getValue());
    }
}
