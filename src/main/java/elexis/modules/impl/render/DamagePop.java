package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.math.Mathf;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.render.visual.VisualGeometry;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@FunctionAdd(name = "DamagePop", alias = "DamagePop", category = Category.Render, description = "Popup numbers of actually taken damage")
public final class DamagePop extends Function {
    private static final long LIFE = 1_100_000_000L;
    private final ModeSetting style = new ModeSetting("Font", "Default", "Default", "Minecraft");
    private final BooleanSetting glow = new BooleanSetting("Glow", true);
    private final SliderSetting strength = new SliderSetting("Glow strength", 1, .25f, 2, .05f);
    private final DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private final ArrayDeque<Pop> pops = new ArrayDeque<>();
    private Int2FloatOpenHashMap last = new Int2FloatOpenHashMap(), next = new Int2FloatOpenHashMap();
    private ClientWorld world;
    private record Pop(Vec3d position, Vec3d drift, String label, int color, long birth) {}
    private record Projected(float x, float y, float alpha, float scale) {}

    public DamagePop() { strength.setVisible(glow::get); addSettings(style, glow, strength); }

    @EventHandler public void tick(EventUpdate.Fast event) {
        if (world != mc.world) { clear(); world = mc.world; }
        if (nullCheck()) return;
        long now = System.nanoTime();
        pops.removeIf(pop -> now - pop.birth >= LIFE);
        next.clear();
        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == mc.player || entity.squaredDistanceTo(mc.player) > 78 * 78) continue;
            int id = entity.getId();
            float hp = Math.max(0, living.getHealth()) + Math.max(0, living.getAbsorptionAmount());
            if (!Float.isFinite(hp)) continue;
            next.put(id, hp);
            float damage = last.getOrDefault(id, hp) - hp;
            if (damage <= .01f || living.hurtTime <= 0) continue;
            var radial = living.getEntityPos().subtract(mc.player.getEntityPos()).multiply(1,0,1).normalize();
            float direction = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
            Vec3d drift = new Vec3d(radial.z * direction, 0, -radial.x * direction);
            Vec3d base = living.getEntityPos().add(0, living.getHeight() * .55, 0).add(drift.multiply(.32));
            if (pops.size() >= 48) pops.removeFirst();
            pops.addLast(new Pop(base, drift, format.format(damage), damage >= 8 ? 0xFFFF655F : damage >= 4 ? 0xFFFFC569 : 0xFFFDF4EC, now));
        }
        var swap = last;
        last = next;
        next = swap;
    }

    @EventHandler public void render(EventRender.Screen.Hud event) {
        if (world != mc.world || nullCheck() || style.is("Minecraft") || pops.isEmpty()) return;
        long now = System.nanoTime();
        var r = event.getRenderer();
        var font = FontRegistry.SF_MEDIUM;
        float gui = (float)mc.getWindow().getScaleFactor();
        for (Pop pop : pops) {
            Projected projected = project(pop, now);
            if (projected == null) continue;
            float size = 9 * gui * projected.scale;
            float x = projected.x * gui - r.measureText(font, pop.label, size).width / 2;
            float y = projected.y * gui;
            if (glow.get()) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.TAU * i / 8;
                    r.text(font, x + (float)Math.cos(angle) * gui * strength.get(), y + (float)Math.sin(angle) * gui * strength.get(),
                            size, pop.label, VisualGeometry.alpha(pop.color, projected.alpha * .16f));
                }
            }
            r.text(font, x, y, size, pop.label, VisualGeometry.alpha(pop.color, projected.alpha));
        }
    }

    public static void prepare(DrawContext context) {
        var manager = Elexis.getFunctionManager();
        if (manager == null || mc.options.hudHidden || mc.inGameHud.getDebugHud().shouldShowDebugHud()) return;
        var module = manager.getDamagePop();
        if (!module.isState() || !module.style.is("Minecraft") || module.world != mc.world || module.nullCheck()) return;
        long now = System.nanoTime();
        for (Pop pop : module.pops) {
            Projected p = project(pop, now);
            if (p == null) continue;
            int x = Math.round(p.x) - mc.textRenderer.getWidth(pop.label) / 2;
            int y = Math.round(p.y) - 4;
            if (module.glow.get()) {
                int edge = VisualGeometry.alpha(pop.color, p.alpha * .22f);
                int radius = Math.max(1, Math.round(module.strength.get()));
                context.drawText(mc.textRenderer, pop.label, x-radius, y, edge, false);
                context.drawText(mc.textRenderer, pop.label, x+radius, y, edge, false);
                context.drawText(mc.textRenderer, pop.label, x, y-radius, edge, false);
                context.drawText(mc.textRenderer, pop.label, x, y+radius, edge, false);
            }
            context.drawText(mc.textRenderer, pop.label, x, y, VisualGeometry.alpha(pop.color, p.alpha), true);
        }
    }

    private static Projected project(Pop pop, long now) {
        float progress = (float)(now - pop.birth) / LIFE;
        if (progress < 0 || progress >= 1) return null;
        float alpha = Math.clamp(Math.min(progress * 15, (1 - progress) * 3), 0, 1);
        if (alpha < .035f) return null;
        var pos = Mathf.worldSpaceToScreenSpace(pop.position.add(pop.drift.multiply(progress * .65)).add(0, progress * .95, 0));
        if (pos == null || !Double.isFinite(pos.x) || !Double.isFinite(pos.y) || pos.z < 0 || pos.z >= 1) return null;
        return new Projected((float)pos.x, (float)pos.y, alpha, .8f + .2f * Math.min(1, progress * 8));
    }

    private void clear() { pops.clear(); last.clear(); next.clear(); world = null; }
    @Override public void onDisable() { clear(); super.onDisable(); }
}
