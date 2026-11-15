package elexis;

import com.mojang.blaze3d.systems.RenderSystem;
import elexis.client.ClientContainer;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.config.*;
import elexis.client.utils.eventbus.EventBus;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.PlayerUtils;
import elexis.client.utils.player.ServerUtil;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.client.utils.render.main.newrender.gl.GlBackend;
import elexis.client.utils.render.main.newrender.gl.GlState;
import elexis.client.utils.render.main.newrender.text.FontObject;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.client.utils.render.main.oldrender.ClientRenderer;
import elexis.client.utils.render.main.oldrender.system.sys2d.CRenderSystem;
import elexis.commands.commands.CommandDispatcher;
import elexis.commands.commands.manager.CommandRepository;
import elexis.modules.api.FunctionManager;
import elexis.modules.impl.utils.UnHook;
import elexis.rpc.RPC;
import elexis.ui.gui.ClickGui;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@Log4j2
public class Elexis implements ModInitializer, IMinecraft {
    @Getter private static Elexis instance;
    @Getter private static final EventBus eventBus = new EventBus();
    @Getter private static FunctionManager functionManager;
    @Getter @Setter private boolean check = true;

    public ClientRenderer testRender;

    @Getter public GlBackend backend;
    @Getter public Renderer2D renderer;
    @Getter public FontObject uiFont;
    @Getter public boolean initialized = false;

    @Getter public ConfigStorage configStorage;
    @Getter public FriendStorage friendStorage;
    @Getter public BlockESPStorage blockESPStorage;
    @Getter public GPSStorage gpsStorage = new GPSStorage();
    @Getter public StaffStorage staffStorage = new StaffStorage();
    private boolean clickGuiKeyPressed;

    @Getter CommandRepository commandRepository;
    @Getter CommandDispatcher commandDispatcher;
    private final RPC rpc = new RPC();

    static ClickGui clickGui;

    public static boolean noNeedSounds = false;

    public Elexis() {
        instance = this;
        configStorage = new ConfigStorage();
        friendStorage = new FriendStorage();
        blockESPStorage = new BlockESPStorage();
    }

    @Override
    public void onInitialize() {
        log("starting...");

        eventBus.registerLambdaFactory("elexis",
                (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        eventBus.subscribe(this);

        try {
            configStorage.init();
        } catch (IOException e) {
            e.printStackTrace();
        }

        functionManager = new FunctionManager();
        functionManager.init();
        initCommands();

        configStorage.loadConfiguration("backup");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                rpc.stopRpc();
            } catch (Exception ignored) {}

            try {
                saveCfg();
            } catch (Exception ignored) {}
        }));

        clickGui = new ClickGui();
        eventBus.subscribe(clickGui);
        rpc.startRpc();
    }

    void initCommands() {
        commandRepository = new CommandRepository();
        commandDispatcher = new CommandDispatcher(getEventBus());
    }

    void saveCfg() {
        if (!UnHook.unhooked) {
            ConfigStorage.saveConfiguration("backup");
        }
    }

    private synchronized void onInit() {
        if (initialized) {
            return;
        }
        backend = new GlBackend();
        renderer = new Renderer2D(backend);
        FontRegistry.initialize(backend, renderer);
        uiFont = FontRegistry.SF_MEDIUM;
        initialized = true;
    }

    public void init() {
        testRender = new ClientRenderer();
    }

    public void ensureRendererInitialized() {
        onInit();
    }

    @EventHandler
    private void onUpdate(EventUpdate.Slow event) {
        if (mc.world == null) return;

        if (mc.currentScreen == null && !UnHook.unhooked) {
            ClickGui.opened = false;
        }

        boolean shiftPressed = InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (shiftPressed && !clickGuiKeyPressed) {
            if (mc.currentScreen == null && !UnHook.unhooked && !ClickGui.opened) {
                mc.setScreen(clickGui);
            }
        }
        clickGuiKeyPressed = shiftPressed;

        if (mc.currentScreen instanceof ClickGui) {
            PlayerUtils.updateKeyBindingsState(new KeyBinding[]{
                    mc.options.forwardKey,
                    mc.options.backKey,
                    mc.options.leftKey,
                    mc.options.rightKey,
                    mc.options.jumpKey,
                    mc.options.sprintKey
            });
        }

        if (mc.player != null) ServerUtil.tick();
    }

    public static void hookRender() {
        Elexis client = getInstance();
        if (client == null || !client.initialized || client.testRender == null || mc.world == null || mc.player == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ClickGui)) return;
        var framebuffer = mc.getFramebuffer();
        int width = framebuffer.textureWidth;
        int height = framebuffer.textureHeight;
        if (width <= 0 || height <= 0 || !(framebuffer.getColorAttachmentView() instanceof GlTextureView view)
                || !(RenderSystem.getDevice() instanceof net.minecraft.client.gl.GlBackend gl)) return;

        // Use the same colour attachment Minecraft presents. The texture view owns this FBO.
        int target = view.getOrCreateFramebuffer(gl.getBufferManager(), null);
        var snapshot = GlState.push();
        try {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glViewport(0, 0, width, height);
            GL11.glColorMask(true, true, true, true);
            var renderer = client.renderer;
            renderer.begin(width, height);
            try {
                renderer.prepareBlur(8);
                var legacy = client.testRender.getCrenderSystem();
                var uiFont = client.uiFont;
                if (!mc.options.hudHidden && !mc.inGameHud.getDebugHud().shouldShowDebugHud()) {
                    eventBus.post(new EventRender.Screen.Hud(renderer, uiFont, width, height));
                }
                renderLegacy(legacy, CRenderSystem.RenderLayer.HUD);
                eventBus.post(new EventRender.Screen.Gui(renderer, uiFont, width, height));
                renderLegacy(legacy, CRenderSystem.RenderLayer.GUI);
                eventBus.post(new EventRender.Screen.Notifications(renderer, uiFont, width, height));
            } finally {
                renderer.end();
            }
        } finally {
            GlState.pop(snapshot);
        }
    }

    private static void renderLegacy(CRenderSystem renderer, CRenderSystem.RenderLayer layer) {
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        var snapshot = GlState.push();
        int sampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        try {
            GL33.glBindSampler(0, 0);
            renderer.render(layer);
        } finally {
            GlState.pop(snapshot);
            GL33.glBindSampler(0, sampler);
            GL13.glActiveTexture(activeTexture);
        }
    }

    public static void log(String str, Object... args) {
        log.info("{} -> {}", ClientContainer.getName(), str.formatted(args));
    }
}
