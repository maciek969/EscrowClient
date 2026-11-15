package elexis.mixins.render;

import elexis.Elexis;
import elexis.client.events.impl.render.EventRender;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void Elexis$captureScene(ObjectAllocator allocator,RenderTickCounter ticks,boolean outline,Camera camera,
            Matrix4f view,Matrix4f projection,Matrix4f world,GpuBufferSlice fog,Vector4f fogColor,boolean sky,CallbackInfo ci) {
        elexis.modules.impl.render.visual.VisualPostProcessor.beginFrame(view,projection,fogColor);
        elexis.modules.impl.render.javelin.chams.ChamsPass.begin();
    }
    @Inject(method = "render", at = @At("RETURN"))
    private void Elexis$renderWorldEvent(
            ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            Matrix4f worldMatrix,
            GpuBufferSlice fog,
            Vector4f fogColor,
            boolean shouldRenderSky,
            CallbackInfo ci
    ) {
        elexis.modules.impl.render.javelin.chams.ChamsPass.finish();
        if(Elexis.getEventBus().isListening(EventRender.World.class)) {
            MatrixStack stack = new MatrixStack();
            stack.multiplyPositionMatrix(positionMatrix);
            Elexis.getEventBus().post(new EventRender.World(stack, tickCounter.getTickProgress(true)));
        }
        // Run before GameRenderer clears scene depth to draw first-person hands.
        elexis.modules.impl.render.visual.VisualPostProcessor.render();
    }
}
