package elexis.modules.api;

import elexis.Elexis;
import elexis.client.utils.client.IMinecraft;
import elexis.modules.api.settings.api.Setting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import elexis.modules.impl.render.Interface;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public abstract class Function implements IMinecraft {

    final String name;
    final String description;
    final Category category;

    boolean state;
    @Setter
    int bind;
    final List<Setting<?>> settings = new ObjectArrayList<>();

    @Setter
    private Float bindAlpha;

    public Function() {
        FunctionAdd annotation = getClass().getAnnotation(FunctionAdd.class);
        this.name = annotation.name();
        this.description = annotation.description();
        this.category = annotation.category();
        this.bind = annotation.key();
    }

    public void addSettings(Setting<?>... settings) {
        this.settings.addAll(List.of(settings));
    }

    public void onEnable() {
        Elexis.getEventBus().subscribe(this);
    }

    public void onDisable() {
        Elexis.getEventBus().unsubscribe(this);
    }

    public String getAlias() {
        return getClass().getAnnotation(FunctionAdd.class).alias();
    }

    public final void toggle() {
        setState(!state);
    }

    public final void setState(boolean newState) {
        if (state == newState) {
            return;
        }

        state = newState;

        try {
            if (state) onEnable();
            else onDisable();
            if (!Elexis.noNeedSounds) {
                Interface.notify(Text.literal(getAlias() + (state ? " включён" : " выключен")),
                        state ? 0xFF73D9A4 : 0xFFE77B7B, 1200);
            }
        } catch (Exception e) {
        }
    }

    public boolean nullCheck() {
        return mc.player == null || mc.world == null;
    }

    public void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(packet);
    }

    public boolean getState(){
        return state;
    }

    public boolean isState() {
        return state;
    }
}
