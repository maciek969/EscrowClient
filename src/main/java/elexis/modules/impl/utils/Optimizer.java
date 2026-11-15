package elexis.modules.impl.utils;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.impl.render.visual.SceneVisuals;
import elexis.modules.impl.render.visual.VisualPostProcessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "Optimizer", alias = "Optimizer", category = Category.Utilities, description = "Light FPS optimizer")
@Environment(EnvType.CLIENT)
public final class Optimizer extends Function {
    public final BooleanSetting reduceParticles = new BooleanSetting("Reduce Particles", true);
    public final BooleanSetting fastMath = new BooleanSetting("Fast Render", true);
    public final BooleanSetting clearMemory = new BooleanSetting("Memory Clean", false);
    private int ticks;

    public Optimizer() {
        this.addSettings(reduceParticles, fastMath, clearMemory);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (!clearMemory.get()) {
            return;
        }
        if (++ticks >= 20 * 60) {
            ticks = 0;
            VisualPostProcessor.trimIfIdle();
        }
    }
    public static boolean fastRender() {
        var module=SceneVisuals.active("Optimizer",Optimizer.class);
        return module!=null && module.fastMath.get();
    }
    public static boolean fewerParticles() {
        var module=SceneVisuals.active("Optimizer",Optimizer.class);
        return module!=null && module.reduceParticles.get();
    }
}
