package elexis.modules.impl.render;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

@FunctionAdd(name = "ViewModel", alias = "ViewModel", category = Category.Render, description = "Changes first person hand position")
@Environment(EnvType.CLIENT)
public final class ViewModel extends Function {
    private final SliderSetting rightX = new SliderSetting("Right X", 0.0F, -2.0F, 2.0F, 0.05F);
    private final SliderSetting rightY = new SliderSetting("Right Y", 0.0F, -2.0F, 2.0F, 0.05F);
    private final SliderSetting rightZ = new SliderSetting("Right Z", 0.0F, -2.0F, 2.0F, 0.05F);
    private final SliderSetting rightScale = new SliderSetting("Right Scale", 1.0F, 0.2F, 2.0F, 0.05F);
    private final SliderSetting leftX = new SliderSetting("Left X", 0.0F, -2.0F, 2.0F, 0.05F);
    private final SliderSetting leftY = new SliderSetting("Left Y", 0.0F, -2.0F, 2.0F, 0.05F);
    private final SliderSetting leftZ = new SliderSetting("Left Z", 0.0F, -2.0F, 2.0F, 0.05F);
    private final SliderSetting leftScale = new SliderSetting("Left Scale", 1.0F, 0.2F, 2.0F, 0.05F);

    public ViewModel() {
        this.addSettings(rightX, rightY, rightZ, rightScale, leftX, leftY, leftZ, leftScale);
    }

    public void apply(MatrixStack matrices, Arm arm) {
        boolean left = arm == Arm.LEFT;
        float x = left ? leftX.get() : rightX.get();
        float y = left ? leftY.get() : rightY.get();
        float z = left ? leftZ.get() : rightZ.get();
        float scale = Math.max(0.05F, left ? leftScale.get() : rightScale.get());
        matrices.translate(x, y, z);
        matrices.scale(scale, scale, scale);
    }
}
