package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.combat.HitAura;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@FunctionAdd(name = "CustomSwing", alias = "Custom Swing", category = Category.Render,
        description = "Меняет анимацию замаха рукой")
public final class CustomSwing extends Function {
    public final ModeSetting mode = new ModeSetting("Мод", "Мод 1",
            "Мод 1", "Мод 2", "Мод 3", "Мод 4", "Мод 5", "Мод 6", "Мод 7", "Мод 8", "Мод 9", "Мод 10", "Мод 11");
    private final SliderSetting shortSwingPower = new SliderSetting("Сила взмаха (2/5)", 5f, 4f, 8f, 0.5f)
            .setVisible(() -> mode.is("Мод 2") || mode.is("Мод 5"));
    private final SliderSetting swingPower = new SliderSetting("Сила взмаха", 35f, 10f, 100f, 1f)
            .setVisible(() -> mode.is("Мод 1") || mode.is("Мод 4") || mode.is("Мод 7") || mode.is("Мод 10"));
    private final SliderSetting rollPower = new SliderSetting("Сила взмаха (8)", 35f, 10f, 35f, 1f)
            .setVisible(() -> mode.is("Мод 8"));
    private final SliderSetting swingDuration = new SliderSetting("Плавность взмаха", 6f, 1f, 10f, 1f)
            .hidden(() -> mode.is("Мод 8") || mode.is("Мод 9"));
    private final SliderSetting spinSpeed = new SliderSetting("Скорость", 5f, 4f, 6f, 0.5f)
            .setVisible(() -> mode.is("Мод 9"));
    private final SliderSetting chopIntensity = new SliderSetting("Интенсивность", 5f, 1f, 10f, 0.5f)
            .setVisible(() -> mode.is("Мод 3"));
    private final SliderSetting swingIncline = new SliderSetting("Сила наклона", 20f, 20f, 75f, 0.1f)
            .setVisible(() -> mode.is("Мод 6"));
    private final BooleanSetting onlyTarget = new BooleanSetting("Только с таргетом", false);

    public CustomSwing() {
        addSettings(mode, shortSwingPower, swingPower, rollPower, swingDuration, spinSpeed, chopIntensity,
                swingIncline, onlyTarget);
    }

    public int getSwingDuration() {
        return mode.is("Мод 8") ? 10 : MathHelper.clamp(swingDuration.get().intValue(), 1, 10);
    }

    public boolean isAnimationActive() {
        if (!isState()) return false;
        if (!onlyTarget.get()) return true;

        var manager = Elexis.getFunctionManager();
        return manager != null && (manager.getHitAura().isState() && HitAura.target != null
                || manager.getAimBot().getTarget() != null || manager.getTriggerBot().getTarget() != null);
    }

    public boolean apply(MatrixStack matrices, ItemStack stack, Hand hand, Arm arm, float progress, float equipProgress) {
        if (hand != Hand.MAIN_HAND || stack.isEmpty() || !isAnimationActive()) return false;
        switch (stack.getUseAction()) {
            case CROSSBOW, TRIDENT, SPEAR:
                return false;
            default:
                break;
        }
        // The native renderer has already applied its equip translation at this call site.
        int side = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(-side * 0.56F, 0.52F + equipProgress * 0.6F, 0.72F);
        renderSwordAnimation(matrices, progress, equipProgress, arm);
        return true;
    }

    private void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float anim = (float)Math.sin(swingProgress * (Math.PI / 2) * 2.0);
        float anim2 = (float)Math.abs(Math.sin(swingProgress * (Math.PI / 4) * 8.0));
        if (mode.is("Мод 11")) {
            if (swingProgress > 0.0F) {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                matrices.translate(0.56F * i, equipProgress * -0.2F - 0.5F, -0.7F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -85.0F));
                matrices.translate(-0.1F * i, 0.28F, 0.2F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0F));
            } else {
                float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                float m = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                float f1 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                matrices.translate(n * i, m, f1);
                this.applyEquipOffset(matrices, arm, equipProgress);
                this.applySwingOffset(matrices, arm, swingProgress);
            }
        } else {
            matrices.translate(0.56F * i, -0.52F, -0.72F);
            switch (mode.get()) {
                case "Мод 2": {
                    float swipe = this.shortSwingPower.get() * anim / 5.0F;
                    matrices.translate(-0.2F * swipe / 2.0F + 0.1F, 0.2F * swipe / 2.0F + 0.2F, -0.1F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60.0F));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                    break;
                }
                case "Мод 3":
                    float intensity = this.chopIntensity.get() / 10.0F;
                    float chop = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    matrices.translate(0.0F, 0.2F + chop * 0.1F, -0.1F - chop * 0.1F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F - chop * 60.0F * intensity));
                    break;
                case "Мод 4":
                    matrices.translate(0.05F, -0.1F * (this.swingPower.get() / 10.0F * anim) + 0.25F, -0.1F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                    break;
                case "Мод 5": {
                    float swipe = this.shortSwingPower.get() * anim / 20.0F;
                    matrices.translate(0.0F, -swipe, 0.0F);
                    break;
                }
                case "Мод 6":
                    float f = MathHelper.sin(swingProgress * (float) (Math.PI / 2) * 2.0F);
                    float f2 = this.swingIncline.get() * 2.5F;
                    float f4 = f2 / 10.0F / 10.0F * 40.0F;
                    matrices.translate(0.1F, 0.2F, -0.1F - f * 0.2F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F + i * f * -(f4 / 2.0F)));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * f * -(f4 / 2.0F)));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(i * f * -f4));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 30.0F));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(i * -80.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 60.0F));
                    break;
                case "Мод 7":
                    matrices.translate(0.1F, 0.2F, -0.1F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - this.swingPower.get() * anim));
                    break;
                case "Мод 8":
                    matrices.translate(0.05F, 0.2F, -0.15F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(140.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F - this.rollPower.get() * anim2));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F));
                    break;
                case "Мод 9":
                    float rotation = MathHelper.clamp(swingProgress * (this.spinSpeed.get() / 4.0F), 0.0F, 1.0F) * -360.0F;
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation));
                    break;
                case "Мод 10":
                    matrices.translate(0.05F, 0.2F, -0.15F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(155.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F - this.swingPower.get() * anim));
                    break;
                default:
                    this.applyCustomSwingOffset(matrices, arm, swingProgress);
            }
        }
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
    }

    private void applyCustomSwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -this.swingPower.get()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
    }
}
