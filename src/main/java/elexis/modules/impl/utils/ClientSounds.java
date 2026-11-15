package elexis.modules.impl.utils;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "ClientSounds", alias = "ClientSounds", category = Category.Utilities, description = "Volume and tone tuning for client sounds")
@Environment(EnvType.CLIENT)
public class ClientSounds extends Function {
    private final SliderSetting volume = new SliderSetting("Volume", 1.0F, 0.0F, 1.0F, 0.05F);
    private final SliderSetting pitch = new SliderSetting("Tone", 1.0F, 0.5F, 2.0F, 0.05F);

    public ClientSounds() {
        addSettings(volume, pitch);
    }

    public float getVolume() {
        return volume.get();
    }

    public float getPitch() {
        return pitch.get();
    }
}
