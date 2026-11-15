package elexis.modules.impl.render.javelin.target;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/** The 1.21.11 equivalents of the two Javelin world geometry pipelines. */
public final class TargetRender {
    public static final Matrix4f MATRIX = new Matrix4f();
    private static final Map<String, RenderLayer> LAYERS = new HashMap<>();
    private static final RenderPipeline[] SHAPES = new RenderPipeline[4];
    private static final RenderPipeline[] SPRITES = new RenderPipeline[2];

    private TargetRender() {}

    public static void frame(Matrix4f matrix) {
        MATRIX.set(matrix);
    }

    public static RenderLayer shapes(boolean throughWalls, boolean additive) {
        int index = (throughWalls ? 1 : 0) + (additive ? 2 : 0);
        if (SHAPES[index] == null) SHAPES[index] = pipeline("shapes_" + index, false, throughWalls, additive);
        return LAYERS.computeIfAbsent("shape_" + index, key -> RenderLayer.of(
                "javelin_target_" + key, RenderSetup.builder(SHAPES[index]).expectedBufferSize(131072).translucent().build()));
    }

    public static RenderLayer sprites(Identifier texture, boolean throughWalls) {
        int index = throughWalls ? 1 : 0;
        if (SPRITES[index] == null) SPRITES[index] = pipeline("sprites_" + index, true, throughWalls, true);
        return LAYERS.computeIfAbsent("sprite_" + index + texture, key -> RenderLayer.of(
                "javelin_target_" + key, RenderSetup.builder(SPRITES[index]).texture("Sampler0", texture)
                        .expectedBufferSize(262144).translucent().build()));
    }

    private static RenderPipeline pipeline(String name, boolean texture, boolean throughWalls, boolean additive) {
        var builder = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(Identifier.of("elexis", "javelin_target/" + name))
                .withVertexShader(texture ? "core/position_tex_color" : "core/position_color")
                .withFragmentShader(texture ? "core/position_tex_color" : "core/position_color")
                .withDepthWrite(false)
                .withDepthTestFunction(throughWalls ? DepthTestFunction.NO_DEPTH_TEST : DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withCull(false)
                .withBlend(additive ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT)
                .withVertexFormat(texture ? VertexFormats.POSITION_TEXTURE_COLOR : VertexFormats.POSITION_COLOR,
                        texture ? VertexFormat.DrawMode.QUADS : VertexFormat.DrawMode.TRIANGLES);
        if (texture) builder.withSampler("Sampler0");
        return RenderPipelines.register(builder.build());
    }
}
