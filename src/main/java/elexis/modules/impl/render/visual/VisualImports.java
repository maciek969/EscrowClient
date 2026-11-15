package elexis.modules.impl.render.visual;

import elexis.modules.api.Function;

/** Native 1.21.11 adaptations of the imported Visual features. */
public final class VisualImports {
    private VisualImports() {}
    public static Function[] create() {
        return new Function[]{
                new EntityVisuals.Tracers(), new EntityVisuals.SkeletonESP(),
                new EntityVisuals.ArmorDurability(), new EntityVisuals.TNTTimer(), new EntityVisuals.FireworkESP(),
                new BlockVisuals.BlockESP(), new BlockVisuals.BlockOverlay(), new BlockVisuals.HoleESP(), new BlockVisuals.TrapIndicator(),
                new ImpactVisuals.HitEffect(), new ImpactVisuals.HitWave(), new ImpactVisuals.GhostHit(),
                new ImpactVisuals.HitMarkers(), new ImpactVisuals.DeathManager(), new ImpactVisuals.TNTEffect(), new ImpactVisuals.KillCards(),
                new AmbientVisuals.FireFly(), new AmbientVisuals.WorldParticles(), new AmbientVisuals.Cubes(),
                new AmbientVisuals.CubeField(), new AmbientVisuals.LineGlyphs(), new AmbientVisuals.VoidButterflies(),
                new OverlayVisuals.AspectRatio(), new OverlayVisuals.PlayerAlert(), new OverlayVisuals.Picture(),
                new OverlayVisuals.Cursor(), new OverlayVisuals.CatPet(), new OverlayVisuals.AntiAim(),
                new elexis.modules.impl.player.FakePlayer(),
                new SceneVisuals.Saturation(), new SceneVisuals.MotionBlur(), new SceneVisuals.FogAtmosphere(),
                new SceneVisuals.ShaderMist(), new SceneVisuals.Rain(), new SceneVisuals.Beautiful(), new SceneVisuals.Ambience(),
                new ModelVisuals.ImpactNova(), new ModelVisuals.KillSpray(), new WardenESP(),
                new GuiVisuals.BetterMinecraft(), new GuiVisuals.ClickGui()
        };
    }
}
