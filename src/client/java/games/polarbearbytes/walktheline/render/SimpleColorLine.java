package games.polarbearbytes.walktheline.render;

import com.mojang.logging.LogUtils;
import games.polarbearbytes.walktheline.WalkTheLine;
import games.polarbearbytes.walktheline.axis.LockedAxisData;
import games.polarbearbytes.walktheline.component.WTLComponents;
import games.polarbearbytes.walktheline.config.WalkTheLineClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * RainbowLine creates a line with a rotating color
 *
 * @author PolarbearBytes
 * @version 1.0
 * @since 2025-07-24
 */
public class SimpleColorLine extends LineBase {
    private final PlayerEntity player;

    protected SimpleColorLine(PlayerEntity player){
        this.lineThickness = 8;
        this.player = player;
    }

    private Vec3d getPlayerPos() {
        var world = MinecraftClient.getInstance().world;
        if(world != null) {
            var clientPlayer = world.getPlayerByUuid(player.getUuid());
            if(clientPlayer != null) {
                return clientPlayer.getEntityPos();
            }
        }
        return player.getEntityPos();
    }

    @Override
    public boolean shouldUpdate(Entity entity, MinecraftClient client) {
        return true;
    }

    @Override
    public void render(Vec3d cameraPos, Entity entity, MinecraftClient client) {
        if(client.world == null) return;
        lastEntityPosition = BlockPos.ofFloored(getPlayerPos());
        var lockedAxis = WTLComponents.lockedAxis(client.world);
        var viewDistance = client.options.getViewDistance().getValue();
        renderRainbowLine(cameraPos, player, lockedAxis.getLockedAxisData(player.getUuid()), viewDistance);
    }

    public void renderRainbowLine(Vec3d cameraPos, Entity entity, LockedAxisData axisData, int viewDistance) {
        if(lastEntityPosition == null || axisData == null ) return;
        LogUtils.getLogger().debug("Rendering for {} (Axis: {}, Coord: {})", player.getStringifiedName(), axisData.axis(), axisData.coordinate());
        var playerState = WTLComponents.playerState(player);
        double tolerance = playerState.getCoordTolerance();

        Integer lineColor = playerState.getLineColor().getColorValue();
        if(lineColor == null) lineColor = 0;
        lineColor = 0xFF000000 | lineColor;


        BufferBuilder lineBuilder = this.renderContext.init();

        WalkTheLineClientConfig config = WalkTheLineClientConfig.getConfig();
        float lineWidth = config.lineWidth / 100f;

        //This is the directions we walk forward and backward
        Direction[] directions = (axisData.axis() == Axis.X ? Axis.Z : Axis.X).getDirections();

        //This is the left to right directions that we are clamped on
        Direction[] perpendicularDirections = axisData.axis().getDirections();

        Vec3d lineStart = getPlayerPos().offset(directions[0], 16* viewDistance);
        Vec3d lineEnd = getPlayerPos().offset(directions[1], 16* viewDistance);

        if(axisData.axis() == Axis.Z){
            lineStart = new Vec3d(lineStart.getX(), lineStart.getY(), axisData.coordinate());
            lineEnd = new Vec3d(lineEnd.getX(), lineEnd.getY(), axisData.coordinate());
        } else {
            lineStart = new Vec3d(axisData.coordinate(), lineStart.getY(), lineStart.getZ());
            lineEnd = new Vec3d(axisData.coordinate(), lineEnd.getY(), lineEnd.getZ());
        }

        float lineOffset = (float)-tolerance - lineWidth;

        Vec3d leftStart = lineStart.offset(perpendicularDirections[0],lineOffset);
        Vec3d leftEnd = lineEnd.offset(perpendicularDirections[0],-tolerance);

        Vec3d rightStart = lineStart.offset(perpendicularDirections[1],lineOffset);
        Vec3d rightEnd = lineEnd.offset(perpendicularDirections[1],-tolerance);

        LogUtils.getLogger().debug("LineColor: {}", lineColor);
        buildFace(leftStart, leftEnd, lineBuilder, cameraPos, config, player.getEntityWorld(), lineColor);
        buildFace(rightStart, rightEnd, lineBuilder, cameraPos, config, player.getEntityWorld(), lineColor);

        try {
            BuiltBuffer lineMeshData = lineBuilder.endNullable();
            if (lineMeshData != null) {
                renderContext.upload(lineMeshData);
                lineMeshData.close();
            }
        } catch (Exception e){
            WalkTheLine.LOGGER.error("RainbowLine#render() error: {}",e.getMessage());
        }
    }

    public void buildFace(Vec3d start, Vec3d end, BufferBuilder builder, Vec3d cameraPos, WalkTheLineClientConfig config, World world, int lineColor){
        float x1 = (float)(start.getX() - cameraPos.x);
        float y1 = (float)(start.getY() - cameraPos.y);
        float z1 = (float)(start.getZ() - cameraPos.z);
        float x2 = (float)(end.getX() - cameraPos.x);
        float y2 = (float)(end.getY() - cameraPos.y);
        float z2 = (float)(end.getZ() - cameraPos.z);

//        int lineColor;
//        if( config.rotatingColor ) {
//            float speed = 0.2f; // smaller = slower, larger = faster
//            float seconds = (float) (LocalTime.now().toNanoOfDay() / 1_000_000_000.0);
//            float hue = (seconds * speed) % 1.0f; // loop hue every (1 / speed) seconds
//            lineColor = Color.HSBtoRGB(hue, 1.0f, 1.0f);
//            lineColor = (lineColor & 0x00FFFFFF) | ((255 * (config.rotatingColorAlpha/100)) << 24);
//        } else {
//            lineColor = Utils.colorHexToInt(config.singleColor.replace("#",""));
//        }

        BlockPos pos;
        int light;

        pos = BlockPos.ofFloored(x1, y1, z1);
        light = WorldRenderer.getLightmapCoordinates(world, pos);
        builder.vertex(x1, y1+0.00f, z1).color(lineColor).light(light).normal(0,1,0);

        pos = BlockPos.ofFloored(x2, y1, z1);
        light = WorldRenderer.getLightmapCoordinates(world, pos);
        builder.vertex(x2, y1+0.01f, z1).color(lineColor).light(light).normal(0,1,0);

        pos = BlockPos.ofFloored(x2, y1, z2);
        light = WorldRenderer.getLightmapCoordinates(world, pos);
        builder.vertex(x2, y1+0.01f, z2).color(lineColor).light(light).normal(0,1,0);

        pos = BlockPos.ofFloored(x1, y1, z2);
        light = WorldRenderer.getLightmapCoordinates(world, pos);
        builder.vertex(x1, y1+0.01f, z2).color(lineColor).light(light).normal(0,1,0);
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient client) {
        //TODO: probably should have some sort of checks here
        this.render(cameraPos, entity, client);
    }
}



