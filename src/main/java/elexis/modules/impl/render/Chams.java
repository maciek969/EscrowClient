package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ColorSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.MultiBooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.render.javelin.chams.ChamsPass;
import elexis.modules.impl.render.visual.VisualGeometry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;

@FunctionAdd(name = "Chams", alias = "Chams", category = Category.Render, description = "Color, glow and glass on player models")
public final class Chams extends Function {
    public final ModeSetting mode = new ModeSetting("Mode", "Glow", "Glow", "Flat", "Blur");
    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets", new BooleanSetting("Players", true),
            new BooleanSetting("Naked", true), new BooleanSetting("Invisible", false),
            new BooleanSetting("Friends", false), new BooleanSetting("Self", false));
    private final BooleanSetting themeColor = new BooleanSetting("Color from theme", false);
    private final ColorSetting color = new ColorSetting("Color", -8869121);
    private final BooleanSetting indication = new BooleanSetting("Damage indication", true);
    private final ColorSetting damage = new ColorSetting("Damage color", -1711322283);
    public final BooleanSetting throughWalls = new BooleanSetting("Through walls", false);
    public final BooleanSetting throughArmor = new BooleanSetting("Through armor", false);
    public final SliderSetting fill = new SliderSetting("Fill density", 100, 0, 100, 1);
    public final BooleanSetting glow = new BooleanSetting("Glow", true);
    public final SliderSetting glowRadius = new SliderSetting("Glow radius", 24, 2, 64, 1);
    public final SliderSetting glowStrength = new SliderSetting("Glow strength", 1.6f, .1f, 3, .1f);
    public final BooleanSetting additive = new BooleanSetting("Additive blending", true);
    private final ColorSetting blurColor = new ColorSetting("Blur color", 0xFFABA5FF);
    public final SliderSetting blurRadius = new SliderSetting("Blur strength", 5, 1, 12, 1);
    public final SliderSetting blurTint = new SliderSetting("Color opacity", 28, 0, 100, 1);
    public final BooleanSetting mirror = new BooleanSetting("Mirroring", false);
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public Chams() {
        themeColor.setVisible(() -> !blur());
        color.setVisible(() -> !blur() && !themeColor.get());
        damage.setVisible(indication::get);
        throughArmor.setVisible(this::glowing);
        fill.setVisible(this::glowing);
        glow.setVisible(this::glowing);
        glowRadius.setVisible(() -> glowing() && glow.get());
        glowStrength.setVisible(() -> glowing() && glow.get());
        additive.setVisible(() -> mode.is("Flat"));
        blurColor.setVisible(this::blur);
        blurRadius.setVisible(this::blur);
        blurTint.setVisible(this::blur);
        mirror.setVisible(this::blur);
        addSettings(mode, targets, themeColor, color, indication, damage, throughWalls, throughArmor, fill,
                glow, glowRadius, glowStrength, additive, blurColor, blurRadius, blurTint, mirror);
    }

    public static Chams active() {
        var manager = Elexis.getFunctionManager();
        if (manager == null || mc.world == null) return null;
        Chams module = manager.getChams();
        return module.isState() ? module : null;
    }

    public boolean blur() { return mode.is("Blur"); }
    public boolean glowing() { return mode.is("Glow"); }

    public boolean targets(Entity entity) {
        if (!(entity instanceof AbstractClientPlayerEntity player) || player.isSpectator() || !player.isAlive()) return false;
        if (player == mc.player) return targets.get("Self") && !mc.options.getPerspective().isFirstPerson();
        if (VisualGeometry.friend(player)) return targets.get("Friends");
        if (player.isInvisible()) return targets.get("Invisible");
        for (EquipmentSlot slot : ARMOR) if (!player.getEquippedStack(slot).isEmpty()) return targets.get("Players");
        return targets.get("Naked");
    }

    public int tint(PlayerEntityRenderState state) {
        if (indication.get() && state.hurt) return 0xFF000000 | damage.get() & 0xFFFFFF;
        int rgb = blur() ? blurColor.get() : themeColor.get() ? ThemeColorUtils.getIconColor(0,0) : color.get();
        return (mode.is("Flat") ? color.get() & 0xFF000000 : 0xFF000000) | rgb & 0xFFFFFF;
    }

    @Override public void onDisable() { ChamsPass.release(); super.onDisable(); }
}
