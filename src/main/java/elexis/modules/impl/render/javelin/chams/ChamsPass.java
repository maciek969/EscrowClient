package elexis.modules.impl.render.javelin.chams;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.render.main.newrender.gl.GlState;
import elexis.client.utils.render.main.newrender.gl.ShaderProgram;
import elexis.modules.impl.render.Chams;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

/** Captures colored models in a separate framebuffer, then applies Javelin's glass or glow. */
public final class ChamsPass implements IMinecraft {
    private record LayerKey(Identifier texture, int flags) {}
    private static final RenderPipeline[] PIPELINES = new RenderPipeline[6];
    private static final Map<LayerKey, RenderLayer> LAYERS = new LinkedHashMap<>(64, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<LayerKey, RenderLayer> entry) { return size() > 256; }
    };
    private static SimpleFramebuffer mask, scene, armor;
    private static final OutputTarget MASK = new OutputTarget("Javelin chams mask", () -> mask);
    private static final OutputTarget ARMOR = new OutputTarget("Javelin chams armor", () -> armor);
    private static ShaderProgram composite;
    private static int vao, readFbo, writeFbo, sampler;
    private static final int[] textures = new int[5], samplers = new int[5];
    private static boolean world, drawn, failed;
    private ChamsPass() {}

    public static void begin() {
        Chams module = Chams.active();
        drawn = false;
        world = module != null;
        if (module == null || module.mode.is("Flat") || failed) return;
        var main = mc.getFramebuffer();
        if (!(main.getColorAttachment() instanceof GlTexture) || !(main.getDepthAttachment() instanceof GlTexture)) return;
        try {
            if (mask == null || mask.textureWidth != main.textureWidth || mask.textureHeight != main.textureHeight) {
                releaseBuffers();
                mask = new SimpleFramebuffer("Javelin model mask", main.textureWidth, main.textureHeight, true);
                scene = new SimpleFramebuffer("Javelin glass scene", main.textureWidth, main.textureHeight, false);
            }
            if (composite == null) composite = ShaderProgram.fromResources("assets/elexis/shaders/visual/scene.vert",
                    "assets/javelin/shaders/core/chams_composite.fsh");
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.clearColorAndDepthTextures(mask.getColorAttachment(), 0, mask.getDepthAttachment(), 1);
            if (module.glowing()) {
                if (armor == null) armor = new SimpleFramebuffer("Javelin armor depth", main.textureWidth, main.textureHeight, true);
                encoder.clearColorAndDepthTextures(armor.getColorAttachment(), 0, armor.getDepthAttachment(), 1);
            }
        } catch (RuntimeException error) { fail(error); }
    }

    public static boolean available(Chams module) {
        return world && !failed && (module.mode.is("Flat") || mask != null && composite != null);
    }

    public static RenderLayer layer(Chams module, Identifier texture) {
        boolean flat = module.mode.is("Flat");
        int flags = flat ? (module.throughWalls.get() ? 1 : 0) | (module.additive.get() ? 2 : 0) : 4;
        if (!flat) drawn = true;
        return layer(texture, flags);
    }

    public static RenderLayer armorLayer(Identifier texture) { return layer(texture, 5); }

    private static RenderLayer layer(Identifier texture, int flags) {
        boolean flat = flags < 4;
        return LAYERS.computeIfAbsent(new LayerKey(texture, flags), key -> {
            if (PIPELINES[flags] == null) {
                var builder = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                        .withLocation(Identifier.of("javelin", "pipeline/chams_" + flags))
                        .withVertexShader(Identifier.of("javelin", flat ? "core/chams_flat" : "core/chams_fill"))
                        .withFragmentShader(Identifier.of("javelin", flat ? "core/chams_flat" : "core/chams_fill"))
                        .withDepthTestFunction(flat && (flags & 1) != 0 ? DepthTestFunction.NO_DEPTH_TEST : DepthTestFunction.LEQUAL_DEPTH_TEST)
                        .withDepthWrite(!flat).withCull(false);
                if (flat) builder.withBlend((flags & 2) != 0 ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT);
                else builder.withoutBlend();
                PIPELINES[flags] = RenderPipelines.register(builder.build());
            }
            var setup = RenderSetup.builder(PIPELINES[flags]).texture("Sampler0", texture);
            if (flat) setup.translucent(); else setup.outputTarget(flags == 5 ? ARMOR : MASK);
            return RenderLayer.of("javelin_chams_" + flags, setup.build());
        });
    }

    public static void finish() {
        world = false;
        var module = Chams.active();
        if (module == null || !drawn || failed || mask == null) return;
        var main = mc.getFramebuffer();
        if (!(main.getColorAttachment() instanceof GlTexture mainColor) || !(main.getDepthAttachment() instanceof GlTexture mainDepth)
                || !(mask.getColorAttachment() instanceof GlTexture maskColor) || !(mask.getDepthAttachment() instanceof GlTexture maskDepth)
                || !(scene.getColorAttachment() instanceof GlTexture sceneColor)) return;
        var state = GlState.push();
        for (int i = 0; i < textures.length; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            textures[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
            samplers[i] = glGetInteger(GL_SAMPLER_BINDING);
        }
        try {
            if (vao == 0) vao = glGenVertexArrays();
            if (readFbo == 0) readFbo = glGenFramebuffers();
            if (writeFbo == 0) writeFbo = glGenFramebuffers();
            if (sampler == 0) {
                sampler = glGenSamplers();
                glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            }
            int width = main.textureWidth, height = main.textureHeight;
            glDisable(GL_SCISSOR_TEST);
            attach(GL_READ_FRAMEBUFFER, readFbo, mainColor.getGlId());
            glReadBuffer(GL_COLOR_ATTACHMENT0);
            attach(GL_DRAW_FRAMEBUFFER, writeFbo, sceneColor.getGlId());
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            glBlitFramebuffer(0,0,width,height,0,0,width,height,GL_COLOR_BUFFER_BIT,GL_NEAREST);
            attach(GL_DRAW_FRAMEBUFFER, writeFbo, mainColor.getGlId());
            glViewport(0,0,width,height);
            glDisable(GL_BLEND);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);
            glDisable(GL_FRAMEBUFFER_SRGB);
            glDepthMask(false);
            glColorMask(true,true,true,true);
            composite.use();
            bind("SceneSampler", sceneColor, 0);
            bind("MaskSampler", maskColor, 1);
            bind("SceneDepthSampler", mainDepth, 2);
            bind("MaskDepthSampler", maskDepth, 3);
            bind("ArmorDepthSampler", armor != null && armor.getDepthAttachment() instanceof GlTexture texture ? texture : maskDepth, 4);
            glUniform4f(composite.getUniformLocation("Params"),
                    module.blur() ? module.blurRadius.get() : module.glowRadius.get(),
                    module.blur() ? module.blurTint.get() / 100 : module.fill.get() / 100,
                    module.blur() ? 0 : module.glow.get() ? module.glowStrength.get() : 0,
                    module.throughWalls.get() ? 1 : 0);
            glUniform3i(composite.getUniformLocation("Mode"), module.blur() ? 1 : 0, module.mirror.get() ? 1 : 0,
                    module.glowing() && module.throughArmor.get() ? 1 : 0);
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, 3);
        } catch (RuntimeException error) {
            fail(error);
        } finally {
            for (int i = 0; i < textures.length; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(GL_TEXTURE_2D, textures[i]);
                glBindSampler(i, samplers[i]);
            }
            GlState.pop(state);
        }
    }

    private static void attach(int target, int fbo, int texture) {
        glBindFramebuffer(target, fbo);
        glFramebufferTexture2D(target, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
    }

    private static void bind(String name, GlTexture texture, int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, texture.getGlId());
        glBindSampler(unit, sampler);
        glUniform1i(composite.getUniformLocation(name), unit);
    }

    private static void fail(RuntimeException failure) {
        failed = true;
        System.err.println("[Elexis Chams] " + failure.getMessage());
    }

    private static void releaseBuffers() {
        if (mask != null) mask.delete();
        if (scene != null) scene.delete();
        if (armor != null) armor.delete();
        mask = scene = armor = null;
    }

    public static void release() {
        world = drawn = failed = false;
        releaseBuffers();
        if (composite != null) composite.delete();
        composite = null;
        if (vao != 0) glDeleteVertexArrays(vao);
        if (readFbo != 0) glDeleteFramebuffers(readFbo);
        if (writeFbo != 0) glDeleteFramebuffers(writeFbo);
        if (sampler != 0) glDeleteSamplers(sampler);
        vao = readFbo = writeFbo = sampler = 0;
    }
}
