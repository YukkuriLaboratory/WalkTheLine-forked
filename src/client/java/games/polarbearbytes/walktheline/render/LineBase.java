package games.polarbearbytes.walktheline.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import games.polarbearbytes.walktheline.WalkTheLine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.client.gl.RenderPipelines.POSITION_TEX_COLOR_SNIPPET;
import static net.minecraft.client.render.VertexFormats.POSITION_COLOR_LIGHT;

public abstract class LineBase implements ILine {
    @Nullable protected BlockPos lastEntityPosition;
    private Vec3d lastCameraPosition;
    protected float lineThickness;

    protected final RenderContext renderContext;
    public static final RenderPipeline renderPipeline = RenderPipeline.builder(POSITION_TEX_COLOR_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(POSITION_COLOR_LIGHT, VertexFormat.DrawMode.QUADS)
            .withLocation(Identifier.of(WalkTheLine.MOD_ID, "pipeline"))
            .withCull(false)
            .withDepthWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build();

    public LineBase(){
        this.lastEntityPosition = BlockPos.ORIGIN;
        this.lastCameraPosition = Vec3d.ZERO;
        lineThickness = 1f;
        renderContext = new RenderContext(()-> WalkTheLine.MOD_ID+"/Lines", renderPipeline);
    }

    @Override
    public void draw(Vec3d cameraPos) {
        if(!renderContext.isStarted() || !renderContext.isUploaded()) return;
        renderContext.lineWidth = 5;
        renderContext.draw(null, MinecraftClient.getInstance(), null, null);
    }

    @Override
    public Vec3d getLastCameraPosition() {
        return lastCameraPosition;
    }

    @Override
    public void setLastCameraPosition(Vec3d lastPosition) {
        this.lastCameraPosition  = lastPosition;
    }

    @Override
    public void reset(){
        this.lastEntityPosition = BlockPos.ORIGIN;
        this.lastCameraPosition = Vec3d.ZERO;
    }
}
