package elexis.modules.impl.render;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ColorSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.render.javelin.target.RenderGlowingUtil;
import elexis.modules.impl.render.javelin.target.TargetRender;
import elexis.modules.impl.render.visual.VisualGeometry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@FunctionAdd(name = "DashTrail", alias = "DashTrail", category = Category.Render, description = "Движущийся шлейф осколков Javelin")
public final class DashTrail extends Function {
    private static final String ROOT = "textures/modules/dashtrail/";
    private static final Identifier BLOOM = Identifier.of("javelin", ROOT + "dashbloomsample.png");
    private static final Identifier[][] TEXTURES = textures();
    private final BooleanSetting self = new BooleanSetting("Себя", true);
    private final BooleanSetting firstPerson = new BooleanSetting("В первом лице", true);
    private final BooleanSetting players = new BooleanSetting("Другие игроки", false);
    private final BooleanSetting friends = new BooleanSetting("Друзья", true);
    private final SliderSetting range = new SliderSetting("Дистанция", 25, 8, 100, 1);
    private final ModeSetting quality = new ModeSetting("Качество", "Баланс", "Производительность", "Баланс", "Максимум");
    private final SliderSetting length = new SliderSetting("Длина", .75f, .3f, 1.5f, .05f);
    private final SliderSetting glow = new SliderSetting("Свечение", 1, .3f, 2, .05f);
    private final BooleanSetting sparks = new BooleanSetting("Искры", true);
    private final BooleanSetting smoothing = new BooleanSetting("Сглаживание", false);
    private final ModeSetting colorMode = new ModeSetting("Цвет", "Палитра", "Палитра", "Theme", "Custom", "Радуга");
    private final ColorSetting customColor = new ColorSetting("Свой цвет", -5625601);
    private final SliderSetting rainbowSpeed = new SliderSetting("Скорость радуги", 2, .5f, 10, .5f);
    private final List<Shard> shards = new ArrayList<>();
    private final List<Shard> sparkParticles = new ArrayList<>();
    private final Map<AbstractClientPlayerEntity, Vec3d> previousPositions = new HashMap<>();
    private final Map<Identifier, List<Shard>> buckets = new HashMap<>();
    private ClientWorld world;

    private static final class Shard {
        Vec3d previous, position, velocity;
        AbstractClientPlayerEntity owner;
        Identifier[] frames;
        int age, life, color, frame;
        float size, roll;
    }

    public DashTrail() {
        firstPerson.setVisible(self::get);
        customColor.setVisible(() -> colorMode.is("Custom"));
        rainbowSpeed.setVisible(() -> colorMode.is("Радуга"));
        addSettings(self, firstPerson, players, friends, range, quality, length, glow, sparks, smoothing, colorMode, customColor, rainbowSpeed);
    }

    @EventHandler public void tick(EventUpdate.Fast event) {
        if (world != mc.world) { clear(); world = mc.world; }
        if (nullCheck()) return;
        var random = ThreadLocalRandom.current();
        for (int i = shards.size() - 1; i >= 0; i--) {
            Shard shard = shards.get(i);
            if (++shard.age >= shard.life) { shards.remove(i); continue; }
            if (smoothing.get() && i + 1 < shards.size() && shard.owner == shards.get(i + 1).owner) {
                shard.velocity = shards.get(i + 1).velocity;
            }
            shard.previous = shard.position;
            shard.velocity = shard.velocity.multiply(1 / 1.05);
            shard.position = shard.position.add(shard.velocity.multiply(5, shard.velocity.y < 0 ? 5 : 5 / 3.5, 5));
            if (sparks.get() && sparkParticles.size() < 900 && shard.age < shard.life * .3f && random.nextInt(12) > 5) {
                var spark = new Shard();
                spark.previous = spark.position = shard.position;
                spark.velocity = new Vec3d(random.nextDouble(-.02,.02), random.nextDouble(-.02,.02), random.nextDouble(-.02,.02));
                spark.color = shard.color;
                spark.size = shard.size * .22f;
                spark.life = 20;
                sparkParticles.add(spark);
            }
        }
        for (var iterator = sparkParticles.iterator(); iterator.hasNext();) {
            Shard spark = iterator.next();
            if (++spark.age >= spark.life) { iterator.remove(); continue; }
            spark.previous = spark.position;
            spark.position = spark.position.add(spark.velocity);
        }
        previousPositions.keySet().removeIf(p -> p.isRemoved() || p.getEntityWorld() != mc.world);
        for (var player : mc.world.getPlayers()) {
            if (player.isRemoved() || player.isSpectator()) continue;
            Vec3d position = player.getEntityPos();
            Vec3d old = previousPositions.put(player, position);
            if (old == null || !shows(player)) continue;
            Vec3d movement = position.subtract(old);
            if (movement.horizontalLengthSquared() >= .08 * .08 && movement.lengthSquared() < 16) spawn(player, position, movement, random);
        }
    }

    private boolean shows(AbstractClientPlayerEntity player) {
        if (player == mc.player) return self.get() && (firstPerson.get() || !mc.options.getPerspective().isFirstPerson());
        return player.squaredDistanceTo(mc.player) <= range.get()*range.get()
                && (VisualGeometry.friend(player) ? friends.get() : players.get());
    }

    private void spawn(AbstractClientPlayerEntity player, Vec3d pos, Vec3d movement, ThreadLocalRandom random) {
        float countMultiplier = quality.is("Максимум") ? 3 : quality.is("Производительность") ? .34f : 1;
        float sizeMultiplier = quality.is("Максимум") ? .55f : quality.is("Производительность") ? 1.35f : 1;
        int count = Math.clamp((int)(movement.length() / .08 * countMultiplier), 1, (int)(16 * countMultiplier));
        for (int i = 0; i < count && shards.size() < 900; i++) {
            var shard = new Shard();
            shard.owner = player;
            shard.position = shard.previous = pos.subtract(movement.multiply((double)i/count)).add(random.nextDouble(-.0875,.0875),
                    player.getHeight() * (.333 + random.nextDouble() * .175), random.nextDouble(-.0875,.0875));
            shard.velocity = movement.multiply(.04);
            shard.color = switch (colorMode.get()) {
                case "Theme" -> ThemeColorUtils.getIconColor(0, 0);
                case "Custom" -> customColor.get();
                case "Радуга" -> Color.HSBtoRGB((float)(System.nanoTime() % 60_000_000_000L) * 1e-9f * rainbowSpeed.get() / 6, .8f, 1);
                default -> Color.HSBtoRGB(random.nextFloat(), 1, 1);
            };
            shard.life = Math.max(4, Math.round(random.nextInt(11, 17) * length.get()));
            shard.size = .1f * sizeMultiplier * random.nextFloat(.7f, 1.3f);
            shard.roll = random.nextFloat(360);
            shard.frames = TEXTURES[random.nextInt(100) > 40 ? random.nextInt(1, TEXTURES.length) : 0];
            shard.frame = shard.frames == TEXTURES[0] ? random.nextInt(shard.frames.length) : -1;
            shards.add(shard);
        }
    }

    @EventHandler public void render(EventRender.World event) {
        if (nullCheck() || world != mc.world || shards.isEmpty() && sparkParticles.isEmpty()) return;
        float delta = event.getTicks();
        TargetRender.frame(event.getMatrixStack().peek().getPositionMatrix());
        RenderGlowingUtil.begin();
        for (Shard shard : shards) {
            draw(shard, delta, shard.size * glow.get() * (1 + 1.5f * (1 - (float)shard.age/shard.life)), .42f);
            int frame = shard.frame < 0 ? Math.min(shard.frames.length-1, (int)((shard.age + delta) * shard.frames.length / shard.life)) : shard.frame;
            buckets.computeIfAbsent(shard.frames[frame], id -> new ArrayList<>()).add(shard);
        }
        for (Shard spark : sparkParticles) draw(spark, delta, spark.size * glow.get(), .9f);
        RenderGlowingUtil.flush(BLOOM, false);
        for (var entry : buckets.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            RenderGlowingUtil.begin();
            for (Shard shard : entry.getValue()) draw(shard, delta, shard.size, 1);
            RenderGlowingUtil.flush(entry.getKey(), false);
            entry.getValue().clear();
        }
    }

    private static void draw(Shard shard, float delta, float size, float opacity) {
        Vec3d pos = shard.previous.lerp(shard.position, delta);
        float progress = (shard.age + delta) / shard.life;
        float alpha = Math.clamp(Math.min(progress * 8, (1 - progress) * 2.5f), 0, 1) * opacity;
        RenderGlowingUtil.glowSprite(pos.x, pos.y, pos.z, size, shard.roll, VisualGeometry.alpha(shard.color, alpha));
    }

    private static Identifier[][] textures() {
        int[] count = {21, 11, 23, 32, 16, 32};
        Identifier[][] result = new Identifier[count.length][];
        for (int group = 0; group < count.length; group++) {
            result[group] = new Identifier[count[group]];
            String folder = ROOT + "dashcubics/" + (group == 0 ? "" : "group_dashs/group" + group + "/");
            for (int frame = 0; frame < count[group]; frame++) result[group][frame] = Identifier.of("javelin", folder + "dashcubic" + (frame+1) + ".png");
        }
        return result;
    }

    private void clear() { shards.clear(); sparkParticles.clear(); previousPositions.clear(); buckets.clear(); world = null; }
    @Override public void onDisable() { clear(); super.onDisable(); }
}
