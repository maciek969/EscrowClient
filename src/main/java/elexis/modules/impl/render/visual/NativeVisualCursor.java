package elexis.modules.impl.render.visual;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;

/** A hardware cursor, not a second image painted behind the Windows pointer. */
public final class NativeVisualCursor implements IMinecraft {
    private long cursor,window;
    private int size;
    public void apply(int requestedSize) {
        long handle=mc.getWindow().getHandle();
        int pixels=Math.max(12,Math.min(64,requestedSize));
        if(cursor!=0 && size==pixels && window==handle) return;
        long replacement=0;
        try(var input=mc.getResourceManager().open(Identifier.of("dimasik","images/gui/cursor.png"));
            NativeImage source=NativeImage.read(input);var stack=MemoryStack.stackPush()) {
            ByteBuffer rgba=MemoryUtil.memAlloc(pixels*pixels*4);
            try {
                for(int y=0;y<pixels;y++) for(int x=0;x<pixels;x++) {
                    int color=source.getColorArgb(x*source.getWidth()/pixels,y*source.getHeight()/pixels);
                    rgba.put((byte)(color>>16)).put((byte)(color>>8)).put((byte)color).put((byte)(color>>24));
                }
                rgba.flip();GLFWImage image=GLFWImage.malloc(stack).set(pixels,pixels,rgba);
                replacement=GLFW.glfwCreateCursor(image,0,0);
                if(replacement==0) throw new IllegalStateException("GLFW did not create the visual cursor");
            } finally {MemoryUtil.memFree(rgba);}
            GLFW.glfwSetCursor(handle,replacement);
            if(cursor!=0) GLFW.glfwDestroyCursor(cursor);
            cursor=replacement;window=handle;size=pixels;
        } catch(Exception error) {
            if(replacement!=0) GLFW.glfwDestroyCursor(replacement);
            throw new IllegalStateException("Cannot load visual cursor",error);
        }
    }
    public void close() {
        if(cursor==0) return;
        if(mc.getWindow()!=null && mc.getWindow().getHandle()==window) GLFW.glfwSetCursor(window,0);
        GLFW.glfwDestroyCursor(cursor);cursor=0;size=0;window=0;
    }
    public long handle() {return cursor;}
}
