package elexis.modules.impl.render.hud.notification;

import elexis.client.utils.render.main.oldrender.system.IconUse;
import lombok.Getter;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Notifies {
    @Getter
    private final CopyOnWriteArrayList<Notify> notifies = new CopyOnWriteArrayList<>();
    private static final Color INFO_ACCENT = new Color(82, 190, 255);
    private static final Color SUCCESS_ACCENT = new Color(112, 233, 145);
    private static final Color WARN_ACCENT = new Color(255, 192, 83);
    private static final Color ERROR_ACCENT = new Color(255, 105, 105);
    private static final Color ITEM_ACCENT = new Color(240, 195, 68);

    public void add(String title, String detail, IconUse icon, Color accent, long durationMs) {
        String text = detail == null || detail.isEmpty() ? title : title + " " + detail;
        add(Text.of(text == null ? "" : text), icon, accent, durationMs);
    }

    public void add(Text text, IconUse icon, long duration) {
        notifies.add(new Notify(text, icon, defaultAccent(icon), duration));
    }

    public void add(Text text, IconUse icon, Color accent, long duration) {
        notifies.add(new Notify(text, icon, accent == null ? defaultAccent(icon) : accent, duration));
    }

    public void addItem(Text text, String item, long duration) {
        addItem(text, item, ITEM_ACCENT, duration);
    }

    public void addItem(Text text, String item, Color accent, long duration) {
        notifies.add(new Notify(text, item, accent == null ? ITEM_ACCENT : accent, duration));
    }

    private static Color defaultAccent(IconUse icon) {
        if (icon == IconUse.CHECK) {
            return SUCCESS_ACCENT;
        }
        if (icon == IconUse.WARN) {
            return WARN_ACCENT;
        }
        if (icon == IconUse.CROSS) {
            return ERROR_ACCENT;
        }
        return INFO_ACCENT;
    }
}
