package elexis.modules.impl.render;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import elexis.modules.impl.render.javelin.crystalhands.*;
import net.minecraft.util.Identifier;
import java.util.Arrays;

/** Javelin crystal model controls, animated UV shaders and astral model explosions. */
@FunctionAdd(name="CustomCrystal", alias="CustomCrystal", category=Category.Render, description="Size, textures, shaders and astral crystal models")
public final class CustomCrystal extends Function {
    private static CustomCrystal instance;
    public final SliderSetting size = new SliderSetting("Size", 1.0F, 0.2F, 3.0F, 0.05F);
    public final SliderSetting spinSpeed = new SliderSetting("Spin speed", 1.0F, 0.0F, 4.0F, 0.05F);
    public final SliderSetting bobHeight = new SliderSetting("Bob height", 1.0F, 0.0F, 3.0F, 0.05F);
    public final BooleanSetting showBase = new BooleanSetting("Stand", true);
    public final BooleanSetting showBeam = new BooleanSetting("Beam", true);
    public final ModeSetting texture = new ModeSetting("Texture", "Vanilla", "Vanilla", "Custom", "Shader", "Astral");
    public final ModeSetting customTexture = new ModeSetting("File", "mellstroy", "mellstroy", "mellstroy_shock", "aqua", "ember", "void", "prism");
    public final ModeSetting shaderMode = new ModeSetting("Shader", "Nebula", "Nebula", "Electric discharge", "Aurora", "Grid", "Magma", "Hologram", "Singularity");
    public final ModeSetting shaderStyle = new ModeSetting("Fill style", "Shader", "Shader", "Fill");
    public final ModeSetting shaderAnimation = new ModeSetting("Shader animation", "Spin", "Spin", "Pulse", "Scan", "Flashes");
    public final SliderSetting shaderSpeed = new SliderSetting("Shader speed", 1.0F, 0.1F, 4.0F, 0.1F);
    public final SliderSetting shaderOpacity = new SliderSetting("Opacity", 85.0F, 10.0F, 100.0F, 1.0F);
    public final ModeSetting shaderColor = new ModeSetting("Shader color", "Theme", "Theme", "Custom");
    public final ColorSetting customColor = new ColorSetting("Custom color", -7504649);
    public final ColorSetting astralColor = new ColorSetting("Astral color", -48395);
    public final ModeSetting astralShape = new ModeSetting("Astral submode", "Classic", Arrays.stream(CrystalShape.values()).map(v -> v.label).toArray(String[]::new));
    public final ModeSetting astralAnimation = new ModeSetting("Astral animation", "Spin", Arrays.stream(CrystalAnimation.values()).map(v -> v.label).toArray(String[]::new));
    public final SliderSetting astralSpeed = new SliderSetting("Effect speed", 1.0F, 0.0F, 3.0F, 0.05F);
    public final SliderSetting astralLineWidth = new SliderSetting("Line thickness", 1.0F, 0.5F, 2.0F, 0.05F);
    public final SliderSetting astralBrightness = new SliderSetting("Astral brightness", 1.05F, 0.2F, 1.5F, 0.05F);
    public final BooleanSetting astralThroughWalls = new BooleanSetting("Astral through walls", true);
    public final BooleanSetting astralExplosion = new BooleanSetting("Explosion animation", true);
    public final ModeSetting astralExplosionType = new ModeSetting("Explosion effect", "Rise", Arrays.stream(CrystalExplosion.values()).map(v -> v.label).toArray(String[]::new));
    public final SliderSetting astralExplosionTime = new SliderSetting("Explosion duration", 0.85F, 0.3F, 1.5F, 0.05F);

    public CustomCrystal() {
        instance = this;
        addSettings(size, spinSpeed, bobHeight, showBase, showBeam, texture, customTexture, shaderMode, shaderStyle, shaderAnimation, shaderSpeed, shaderOpacity, shaderColor, customColor, astralColor, astralShape, astralAnimation, astralSpeed, astralLineWidth, astralBrightness, astralThroughWalls, astralExplosion, astralExplosionType, astralExplosionTime);
        this.customTexture.setVisible(() -> this.texture.is("Custom"));
        this.shaderStyle.setVisible(() -> this.texture.is("Shader"));
        this.shaderMode.setVisible(() -> this.texture.is("Shader") && this.shaderStyle.is("Shader"));
        this.shaderAnimation.setVisible(() -> this.texture.is("Shader") && this.shaderStyle.is("Shader"));
        this.shaderSpeed.setVisible(() -> this.texture.is("Shader") && this.shaderStyle.is("Shader"));
        this.shaderOpacity.setVisible(() -> this.texture.is("Shader"));
        this.shaderColor.setVisible(() -> this.texture.is("Shader") && (this.shaderStyle.is("Fill") || !this.shaderMode.is("Magma")));
        this.customColor
                .setVisible(() -> this.texture.is("Shader") && this.shaderColor.is("Custom") && (this.shaderStyle.is("Fill") || !this.shaderMode.is("Magma")));
        this.astralColor.setVisible(() -> this.texture.is("Astral"));
        this.astralShape.setVisible(() -> this.texture.is("Astral"));
        this.astralAnimation.setVisible(() -> this.texture.is("Astral"));
        this.astralSpeed.setVisible(() -> this.texture.is("Astral"));
        this.astralLineWidth.setVisible(() -> this.texture.is("Astral"));
        this.astralBrightness.setVisible(() -> this.texture.is("Astral"));
        this.astralThroughWalls.setVisible(() -> this.texture.is("Astral"));
        this.astralExplosion.setVisible(() -> this.texture.is("Astral") || this.texture.is("Shader"));
        this.astralExplosionType.setVisible(() -> (this.texture.is("Astral") || this.texture.is("Shader")) && this.astralExplosion.get());
        this.astralExplosionTime.setVisible(() -> (this.texture.is("Astral") || this.texture.is("Shader")) && this.astralExplosion.get());
        this.showBase.setVisible(() -> !this.texture.is("Astral"));
    }

    /** Alias for {@link #astralOptions()} — used by CrystalEffects. */
    public static CrystalOptions astralCrystalOptions() {
        return astralOptions();
    }

    @EventHandler public void tick(EventUpdate.Slow event) {
        if (effectsActive()) CrystalEffects.tick(); else CrystalEffects.clear();
    }
    @EventHandler public void render(EventRender.World event) {
        if (texture.is("Shader")) CrystalShaderTexture.update();
        if (effectsActive()) CrystalEffects.render(event);
    }
    @Override public void onDisable() { CrystalEffects.clear(); super.onDisable(); }
    public static CustomCrystal get() { return instance; }
    public static boolean on() { return instance != null && instance.isState(); }
    public static float scale() { return on() ? instance.size.get() : 1f; }
    public static float spin() { return on() ? instance.spinSpeed.get() : 1f; }
    public static float bob() { return on() ? instance.bobHeight.get() : 1f; }
    public static boolean base() { return !on() || instance.showBase.get(); }
    public static boolean beam() { return !on() || instance.showBeam.get(); }
    public static boolean shaderActive() { return on() && instance.texture.is("Shader"); }
    public static boolean astralActive() { return on() && instance.texture.is("Astral") && !CrystalAstralTexture.failed(); }
    public static boolean effectsActive() { return shaderActive() || astralActive(); }
    public static int astralRgb() { return on() ? instance.astralColor.get() & 0xFFFFFF : 0xFF42F5; }
    public static Identifier textureOverride() {
        if (!on()) return null;
        if (instance.texture.is("Custom")) return Identifier.of("javelin", "textures/crystal/" + instance.customTexture.get() + ".png");
        return shaderActive() ? CrystalShaderTexture.identifier() : null;
    }
    public static CrystalOptions astralOptions() {
        CustomCrystal c = instance;
        if (c == null) {
            // Module not initialized yet — return safe defaults,
            // so CrystalEffects does not crash with NPE.
            return new CrystalOptions(CrystalShape.CLASSIC, CrystalAnimation.ROTATION,
                    1f, 1f, 1f, false, false, CrystalExplosion.RISE, 0.85f);
        }
        boolean astral = astralActive();
        return new CrystalOptions(CrystalShape.from(c.astralShape.get()), CrystalAnimation.from(c.astralAnimation.get()),
                astral ? c.astralSpeed.get() : c.shaderSpeed.get(), c.astralLineWidth.get(),
                astral ? c.astralBrightness.get() : 1f, astral && c.astralThroughWalls.get(),
                c.astralExplosion.get(), CrystalExplosion.from(c.astralExplosionType.get()), c.astralExplosionTime.get());
    }
    public static String shaderModeName() { return on() && !instance.shaderStyle.is("Fill") ? instance.shaderMode.get() : "Flat"; }
    public static String shaderAnimationName() { return on() ? instance.shaderAnimation.get() : "Spin"; }
    public static float shaderSpeedValue() { return on() ? instance.shaderSpeed.get() : 1f; }
    public static float shaderOpacityValue() { return on() ? instance.shaderOpacity.get()/100f : 1f; }
    public static int shaderRgb() { return on() && instance.shaderColor.is("Custom") ? instance.customColor.get() : ThemeColorUtils.getIconColor(0,0); }
}