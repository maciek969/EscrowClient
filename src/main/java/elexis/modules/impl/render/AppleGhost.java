package elexis.modules.impl.render;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.mixins.accessors.JavelinPotionSwirlsAccessor;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.render.javelin.target.RenderGlowingUtil;
import elexis.modules.impl.render.javelin.target.TargetRender;
import elexis.modules.impl.render.visual.VisualGeometry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@FunctionAdd(name = "AppleGhost", alias = "AppleGhost", category = Category.Render, description = "Золотые частицы регенерации после яблока")
public final class AppleGhost extends Function {
    private static final int[] GOLD = {-14040, -10163, -7046, -18155, -3933};
    private static final Identifier GHOST = Identifier.of("javelin", "textures/modules/appleghost/ghost-glow.png");
    private final SliderSetting amount = new SliderSetting("Количество", 3, 1, 8, 1);
    private final SliderSetting size = new SliderSetting("Размер", .1f, .05f, .2f, .01f);
    private final BooleanSetting self = new BooleanSetting("Показывать на себе", true);
    private final Map<AbstractClientPlayerEntity, Eating> players = new HashMap<>();
    private final List<Spark> particles = new ArrayList<>();
    private ClientWorld world;

    private static final class Eating {
        int use, duration, arm, regeneration;
        boolean active;
    }
    private static final class Spark {
        Vec3d previous, position, velocity;
        int age, life, color;
        float size, phase;
    }

    public AppleGhost() { addSettings(amount, size, self); }

    @EventHandler public void tick(EventUpdate.Fast event) {
        if (world != mc.world) { clear(); world = mc.world; }
        if (nullCheck()) return;
        for (var iterator = particles.iterator(); iterator.hasNext();) {
            Spark spark = iterator.next();
            if (++spark.age >= spark.life) { iterator.remove(); continue; }
            spark.previous = spark.position;
            double wave = spark.age * .19 + spark.phase;
            var v = spark.velocity;
            spark.velocity = new Vec3d((v.x + Math.sin(wave) * .00055) * .975,
                    Math.min(.066, v.y + .00075), (v.z + Math.cos(wave * .91) * .00055) * .975);
            spark.position = spark.position.add(spark.velocity);
        }
        players.keySet().removeIf(p -> p.isRemoved() || p.getEntityWorld() != mc.world);
        for (var player : mc.world.getPlayers()) {
            if (player.isSpectator() || player.isRemoved() || player.squaredDistanceTo(mc.player) > 3136) continue;
            Eating state = players.computeIfAbsent(player, p -> new Eating());
            var item = player.getActiveItem();
            if (player.isUsingItem() && (item.isOf(Items.GOLDEN_APPLE) || item.isOf(Items.ENCHANTED_GOLDEN_APPLE))) {
                state.use = Math.max(state.use + 1, player.getItemUseTime());
                state.duration = item.getMaxUseTime(player);
            } else if (state.use > 0) {
                if (state.use >= state.duration - 5) state.arm = 24;
                state.use = 0;
            } else if (state.arm > 0) state.arm--;
            var effect = player.getStatusEffect(StatusEffects.REGENERATION);
            int duration = effect == null ? 0 : effect.getDuration();
            boolean regenerating = player == mc.player ? duration > 0 : duration > 0 || regenerationSwirl(player);
            boolean started = player == mc.player ? regenerating && (state.regeneration <= 0 || duration > state.regeneration + 4)
                    : regenerating && !state.active;
            if (state.arm > 0 && started) { state.active = true; state.arm = 0; }
            if (!regenerating) state.active = false;
            state.regeneration = duration;
            if (state.active && (player != mc.player || self.get())) spawn(player);
        }
    }

    private static boolean regenerationSwirl(AbstractClientPlayerEntity player) {
        int color = StatusEffects.REGENERATION.value().getColor();
        for (var particle : player.getDataTracker().get(JavelinPotionSwirlsAccessor.elexis$potionSwirls())) {
            if (particle instanceof TintedParticleEffect effect && effect.getType() == ParticleTypes.ENTITY_EFFECT
                    && Math.round(effect.getRed() * 255) == (color >> 16 & 255)
                    && Math.round(effect.getGreen() * 255) == (color >> 8 & 255)
                    && Math.round(effect.getBlue() * 255) == (color & 255)) return true;
        }
        return false;
    }

    private void spawn(AbstractClientPlayerEntity player) {
        var random = ThreadLocalRandom.current();
        for (int i = 0; i < Math.round(amount.get()) && particles.size() < 720; i++) {
            double angle = random.nextDouble(Math.TAU), radius = random.nextDouble(.24, .55);
            var spark = new Spark();
            spark.position = spark.previous = player.getEntityPos().add(Math.cos(angle)*radius, random.nextDouble(.12, 1.48), Math.sin(angle)*radius);
            spark.velocity = new Vec3d(-Math.sin(angle)*.008, random.nextDouble(.026, .052), Math.cos(angle)*.008);
            spark.life = random.nextInt(17, 30);
            spark.color = GOLD[random.nextInt(GOLD.length)];
            spark.size = size.get() * random.nextFloat(.78f, 1.22f);
            spark.phase = random.nextFloat() * 6;
            particles.add(spark);
        }
    }

    @EventHandler public void render(EventRender.World event) {
        if (nullCheck() || world != mc.world || particles.isEmpty()) return;
        TargetRender.frame(event.getMatrixStack().peek().getPositionMatrix());
        RenderGlowingUtil.begin();
        for (Spark spark : particles) {
            var pos = spark.previous.lerp(spark.position, event.getTicks());
            float life = (spark.age + event.getTicks()) / spark.life;
            float alpha = Math.max(0, Math.min(life * 8, (1 - life) * 2));
            RenderGlowingUtil.glowSprite(pos.x, pos.y, pos.z, spark.size, spark.age * 4,
                    VisualGeometry.alpha(spark.color, alpha * .82f));
        }
        RenderGlowingUtil.flush(GHOST, false);
    }

    private void clear() { players.clear(); particles.clear(); world = null; }
    @Override public void onDisable() { clear(); super.onDisable(); }
}
