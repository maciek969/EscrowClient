package elexis.mixins.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.vertex.VertexFormat;
import elexis.Elexis;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Shadow private com.mojang.blaze3d.textures.GpuTexture itemAtlasTexture;

    @Inject(method = "prepareItem", at = @At("TAIL"))
    private void Elexis$captureHudItemIcon(net.minecraft.client.gui.render.state.ItemGuiElementRenderState state,
                                         float u, float v, int pixels, int atlasSize, CallbackInfo ci) {
        if (itemAtlasTexture instanceof net.minecraft.client.texture.GlTexture texture) {
            elexis.modules.impl.render.hud.HudItemIcons.capture(state.x(), state.y(), texture.getGlId(), u, v, pixels, atlasSize);
            elexis.modules.impl.render.NameTags.captureIcon(state.x(), state.y(), texture.getGlId(), u, v, pixels, atlasSize);
        }
    }
    @Inject(method = "render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V", at = @At("HEAD"))
    private void client$prepare(Supplier<String> nameSupplier, Framebuffer framebuffer, GpuBufferSlice fogBuffer, GpuBufferSlice dynamicTransformsBuffer, GpuBuffer buffer, VertexFormat.IndexType indexType, int from, int _to, CallbackInfo ci) {
        Elexis.getInstance().testRender.prepare();
    }
}
