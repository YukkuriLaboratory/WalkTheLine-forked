package games.polarbearbytes.walktheline.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LineRenderer implements IRenderer {
    private final ConcurrentHashMap<UUID, LineBase> playerLineRenderers = new ConcurrentHashMap<>();

    public static final LineRenderer INSTANCE = new LineRenderer();

    public static LineRenderer getInstance() {
        return INSTANCE;
    }

    private LineRenderer(){
    }

    public void render(Framebuffer framebuffer, Matrix4f positionMatrix, Matrix4f projectionMatrix, MinecraftClient client, FrameGraphBuilder frameGraphBuilder, DefaultFramebufferSet fbSet, Frustum frustum, Camera camera, BufferBuilderStorage buffers, Profiler profiler){
        Entity cameraEntity = client.getCameraEntity();
        this.update(camera.getCameraPos(), cameraEntity, client);
        this.draw(camera.getCameraPos());
    }

    public void update(Vec3d cameraPos, Entity entity, MinecraftClient client){
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if(!playerLineRenderers.containsKey(player.getUuid())) {
                playerLineRenderers.put(player.getUuid(), new SimpleColorLine(player));
            }
        }
        for(LineBase renderer : this.playerLineRenderers.values()){
            if(!renderer.shouldUpdate(entity, client)) return;
            renderer.lastEntityPosition = BlockPos.ofFloored(entity.getEntityPos());
            renderer.update(cameraPos, entity, client);
            renderer.setLastCameraPosition(cameraPos);
        }
    }

    public void draw(Vec3d cameraPos){
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();

        for(LineBase renderer : this.playerLineRenderers.values()){
            Vec3d updatePosition = renderer.getLastCameraPosition();
            matrix4fstack.pushMatrix();
            matrix4fstack.translate((float) (updatePosition.x - cameraPos.x), (float) (updatePosition.y - cameraPos.y), (float) (updatePosition.z - cameraPos.z));
            renderer.draw(cameraPos);
            matrix4fstack.popMatrix();
        }
    }

    public void reset(){
        for(LineBase renderer : this.playerLineRenderers.values()){
            renderer.reset();
        }
    }

    public void clearRenderers() {
        playerLineRenderers.clear();
    }
}
