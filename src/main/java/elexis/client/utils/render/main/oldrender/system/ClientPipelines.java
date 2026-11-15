package elexis.client.utils.render.main.oldrender.system;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

import static net.minecraft.client.gl.RenderPipelines.*;

public class ClientPipelines {
    public static final RenderPipeline HUD = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_SNIPPET).withLocation("pipeline/gui").build());

    public static final RenderPipeline WORLD = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation("pipeline/wclient")
            .build());


    public static final RenderPipeline TARGET_ESP_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/wtex")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
//                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );
    public static final RenderPipeline PARTICLE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/wtex")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );
    public static final Function<Identifier, RenderLayer> TARGET_ESP = Util.memoize(
            texture -> RenderLayer.of(
                    "wtex",
                    RenderSetup.builder(TARGET_ESP_PIPELINE)
                            .expectedBufferSize(1536)
                            .texture("Sampler0", texture)
                            .translucent()
                            .build()
            )
    );
    public static final Function<Identifier, RenderLayer> PARTICLES = Util.memoize(
            texture -> RenderLayer.of(
                    "wparticles",
                    RenderSetup.builder(PARTICLE_PIPELINE)
                            .expectedBufferSize(1536)
                            .texture("Sampler0", texture)
                            .translucent()
                            .build()
            )
    );

    public static final RenderPipeline.Snippet RENDER_TYPE_QUADS = RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .buildSnippet();
    public static final RenderPipeline QUADS = register(RenderPipeline.builder(RENDER_TYPE_QUADS).withLocation("pipeline/quads").build());
    public static final RenderLayer QUAD = RenderLayer.of(
            "lines",
            RenderSetup.builder(QUADS)
                    .expectedBufferSize(1536)
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .build()
    );

    public static final RenderPipeline.Snippet RENDER_TYPE_TRIANGLE_FAN = RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_FAN)
            .buildSnippet();
    public static final RenderPipeline TRIANGLE_FAN = register(RenderPipeline.builder(RENDER_TYPE_TRIANGLE_FAN).withLocation("pipeline/triangle_fan").build());

    public static final RenderLayer WORLD_TRIANGLE_FAN = RenderLayer.of(
            "wtrianglefan",
            RenderSetup.builder(TRIANGLE_FAN)
                    .expectedBufferSize(1536)
                    .build()
    );

    public static final RenderPipeline.Snippet RENDER_TYPE_TRIANGLE_STRIP = RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .buildSnippet();
    public static final RenderPipeline TRIANGLE_STRIP = register(RenderPipeline.builder(RENDER_TYPE_TRIANGLE_STRIP).withLocation("pipeline/triangle_fan").build());

    public static final RenderLayer WORLD_TRIANGLE_STRIP = RenderLayer.of(
            "wtrianglestrip",
            RenderSetup.builder(TRIANGLE_STRIP)
                    .expectedBufferSize(1536)
                    .build()
    );

    public static final RenderPipeline.Snippet RENDER_TYPE_POINTS = RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .buildSnippet();
    public static final RenderPipeline POINTS_PIPELINE = register(RenderPipeline.builder(RENDER_TYPE_POINTS).withLocation("pipeline/points").build());
    public static final RenderLayer POINTS = RenderLayer.of(
            "points",
            RenderSetup.builder(POINTS_PIPELINE)
                    .expectedBufferSize(1536)
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .build()
    );

    public static CustomVertexConsumerProvider customVertexConsumerProvider;
}
