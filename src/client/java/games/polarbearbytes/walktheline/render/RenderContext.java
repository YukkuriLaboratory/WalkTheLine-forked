package games.polarbearbytes.walktheline.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import games.polarbearbytes.walktheline.WalkTheLine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ScissorState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.BufferAllocator;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Class to render stuff
 * Inspired / rewrote based on code from sakura-ryoko's MiniHUD / Malilib forks
 * <a href="https://github.com/sakura-ryoko">sakura-ryoko's GitHub</a>
 */
public class RenderContext {
    protected Supplier<String> id;

    protected final RenderPipeline renderPipeline;

    protected BufferAllocator bufferAllocator;
    protected BufferBuilder bufferBuilder;
    protected GpuBuffer vertexBuffer;
    protected GpuBuffer indexBuffer;
    protected ShapeIndexBuffer shapeIndexBuffer;
    protected DrawMode drawMode;
    protected VertexFormat vertexFormat;

    protected int indexCount;
    protected IndexType indexType;

    public float lineWidth;

    protected boolean started;

    public boolean isStarted() {
        return started;
    }

    protected boolean uploaded;

    public boolean isUploaded() {
        return uploaded;
    }

    public RenderContext(Supplier<String> id, RenderPipeline renderPipeline){
        this.id = id;
        this.renderPipeline = renderPipeline;
        started = false;
        uploaded = false;
        lineWidth = 1;
    }

    public BufferBuilder init(){
        this.reset();
        drawMode = renderPipeline.getVertexFormatMode();
        vertexFormat = renderPipeline.getVertexFormat();

        bufferAllocator = new BufferAllocator(vertexFormat.getVertexSize() * 4);
        bufferBuilder = new BufferBuilder(bufferAllocator, drawMode, vertexFormat);

        shapeIndexBuffer = RenderSystem.getSequentialBuffer(drawMode);

        vertexBuffer = null;
        indexBuffer = null;

        indexCount = -1;

        started = true;
        uploaded = false;

        return bufferBuilder;
    }

    public void upload(BuiltBuffer meshData){
        if(!RenderSystem.isOnRenderThread() || meshData == null) return;
        int meshDataSize = meshData.getBuffer().remaining();

        //Close any pre-existing buffers
        if(vertexBuffer != null){
            this.vertexBuffer.close();
        }

        if(indexBuffer != null) {
            indexBuffer.close();
            indexBuffer = null;
        }

        if(vertexBuffer == null || vertexBuffer.size() < meshDataSize){
            //Create the vertex buffer or recreate it with the correct size
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> this.id+"/VertexBuffer", 40, meshDataSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        if (!vertexBuffer.isClosed()) {
            //Copy the vertex data to the buffer
            commandEncoder.writeToBuffer(vertexBuffer.slice(), meshData.getBuffer());
        } else {
            throw new RuntimeException("Copying Vertex Data to Buffer When Buffer is Closed");
        }

        //TODO: Probably should be buffer sorting logic here, but so far do not need it

        //Set how many indices our meshData has
        indexCount = meshData.getDrawParameters().indexCount();
        //Set the data type for the indices
        indexType = meshData.getDrawParameters().indexType();
        uploaded = true;
    }

    protected void draw(Framebuffer framebuffer, MinecraftClient client, GpuTextureView glTextureView){
        if(!RenderSystem.isOnRenderThread()) return;
        GpuDevice device = RenderSystem.getDevice();
        if(device == null){
            WalkTheLine.LOGGER.error("RenderSystem did not return a valid device!");
            return;
        }

        GpuTextureView texture1;
        GpuTextureView texture2;

        Framebuffer fb = framebuffer == null ? client.getFramebuffer() : framebuffer;

        texture1 = fb.getColorAttachmentView();
        texture2 = fb.useDepthAttachment ? fb.getDepthAttachmentView() : null;


        GpuBuffer indexBuffer = this.shapeIndexBuffer.getIndexBuffer(this.indexCount);

        Vector4f colorMod = new Vector4f(1f, 1f, 1f, 1f);
        Vector3f modelOffset = new Vector3f();
        Matrix4f texMatrix = new Matrix4f();

        Matrix3f normalMatrix = new Matrix3f(RenderSystem.getModelViewMatrix()).invert().transpose();

        GpuBufferSlice bufferSlice = RenderSystem.getDynamicUniforms()
                .write(
                        RenderSystem.getModelViewMatrix(),
                        colorMod,
                        modelOffset,
                        new Matrix4f(normalMatrix),
                        0f);
        try(RenderPass pass = device.createCommandEncoder().createRenderPass(this.id,
                texture1, OptionalInt.empty(),
                texture2, OptionalDouble.empty())) {
            pass.setPipeline(this.renderPipeline);
            pass.bindSampler("Lightmap", MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().getGlTextureView());

            ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();

            if (scissorState.isEnabled()) {
                pass.enableScissor(scissorState.getX(), scissorState.getY(), scissorState.getWidth(), scissorState.getHeight());
            }
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", bufferSlice);

            if (this.indexBuffer == null) {
                pass.setIndexBuffer(indexBuffer, shapeIndexBuffer.getIndexType());
            } else {
                pass.setIndexBuffer(this.indexBuffer, indexType);
            }
            pass.setVertexBuffer(0, vertexBuffer);
            if (glTextureView != null){
                pass.bindSampler("Sampler0", glTextureView);
            }

            pass.drawIndexed(0, 0, this.indexCount, 1);
            RenderSystem.lineWidth(RenderSystem.getShaderLineWidth());
        }
    }

    public void reset(){
        if(bufferAllocator != null){
            bufferAllocator.close();
            bufferAllocator = null;
        }
        if(bufferBuilder != null){
            try{
                BuiltBuffer built = bufferBuilder.endNullable();
                if(built != null){
                    built.close();
                }
            } catch(Exception ignored){}

            bufferBuilder = null;
        }
        if(shapeIndexBuffer != null){
            shapeIndexBuffer = null;
        }
        if(vertexBuffer != null){
            vertexBuffer.close();
            vertexBuffer = null;
        }
        if(indexBuffer != null){
            indexBuffer.close();
            indexBuffer = null;
        }
    }
}
