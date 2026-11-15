package elexis.client.utils.render.easy;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

import static elexis.client.utils.client.IMinecraft.mc;

public class RenderUtil {
    public static float getTickDelta() {
        return mc.getRenderTickCounter().getDynamicDeltaTicks();
    }

    // === Blend ===
    public static void enableBlend() {
        GlStateManager._enableBlend();
    }

    public static void disableBlend() {
        GlStateManager._disableBlend();
    }

    public static void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        GlStateManager._blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        blendFuncSeparate(srcFactor, dstFactor, srcFactor, dstFactor);
    }

    public static void defaultBlendFunc() {
        blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    // === Depth Test ===
    public static void enableDepthTest() {
        GlStateManager._enableDepthTest();
    }

    public static void disableDepthTest() {
        GlStateManager._disableDepthTest();
    }

    public static void depthFunc(int func) {
        GlStateManager._depthFunc(func);
    }

    public static void depthMask(boolean mask) {
        GlStateManager._depthMask(mask);
    }

    // === Cull Face ===
    public static void enableCull() {
        GlStateManager._enableCull();
    }

    public static void disableCull() {
        GlStateManager._disableCull();
    }

    // === Scissor Test ===
    public static void enableScissor() {
        GlStateManager._enableScissorTest();
    }

    public static void disableScissor() {
        GlStateManager._disableScissorTest();
    }

    public static void scissor(int x, int y, int width, int height) {
        GlStateManager._scissorBox(x, y, width, height);
    }

    // === Polygon Offset ===
    public static void enablePolygonOffset() {
        GlStateManager._enablePolygonOffset();
    }

    public static void disablePolygonOffset() {
        GlStateManager._disablePolygonOffset();
    }

    public static void polygonOffset(float factor, float units) {
        GlStateManager._polygonOffset(factor, units);
    }

    // === Color Logic Op ===
    public static void enableColorLogicOp() {
        GlStateManager._enableColorLogicOp();
    }

    public static void disableColorLogicOp() {
        GlStateManager._disableColorLogicOp();
    }

    public static void logicOp(int op) {
        GlStateManager._logicOp(op);
    }

    // === Texture ===
    public static void activeTexture(int textureUnit) {
        GlStateManager._activeTexture(textureUnit);
    }

    public static void bindTexture(int textureId) {
        GlStateManager._bindTexture(textureId);
    }

    public static int genTexture() {
        return GlStateManager._genTexture();
    }

    public static void deleteTexture(int textureId) {
        GlStateManager._deleteTexture(textureId);
    }

    public static void texParameter(int target, int pname, int param) {
        GlStateManager._texParameter(target, pname, param);
    }

    // === Framebuffer ===
    public static void bindFramebuffer(int target, int framebuffer) {
        GlStateManager._glBindFramebuffer(target, framebuffer);
    }

    public static int genFramebuffer() {
        return GlStateManager.glGenFramebuffers();
    }

    public static void deleteFramebuffer(int framebuffer) {
        GlStateManager._glDeleteFramebuffers(framebuffer);
    }

    public static void framebufferTexture2D(int target, int attachment, int texTarget, int texture, int level) {
        GlStateManager._glFramebufferTexture2D(target, attachment, texTarget, texture, level);
    }

    // === Viewport ===
    public static void viewport(int x, int y, int width, int height) {
        GlStateManager._viewport(x, y, width, height);
    }

    // === Color Mask ===
    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GlStateManager._colorMask(red, green, blue, alpha);
    }

    // === Clear ===
    public static void clear(int mask) {
        GlStateManager._clear(mask);
    }

    // === Vertex Attrib ===
    public static void enableVertexAttribArray(int index) {
        GlStateManager._enableVertexAttribArray(index);
    }

    public static void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GlStateManager._vertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    // === Draw ===
    public static void drawArrays(int mode, int first, int count) {
        GlStateManager._drawArrays(mode, first, count);
    }

    public static void drawElements(int mode, int count, int type, long indices) {
        GlStateManager._drawElements(mode, type, count, indices);
    }

    // === Shader / Program
    public static void useProgram(int program) {
        GlStateManager._glUseProgram(program);
    }

    public static int createProgram() {
        return GlStateManager.glCreateProgram();
    }

    public static void uniform1i(int location, int value) {
        GlStateManager._glUniform1i(location, value);
    }

    public static void uniform1f(int location, float value) {
        GL20.glUniform1f(location, value);
    }

    public static void uniformMatrix4fv(int location, boolean transpose, FloatBuffer matrix) {
        GL20.glUniformMatrix4fv(location, transpose, matrix);
    }
}
