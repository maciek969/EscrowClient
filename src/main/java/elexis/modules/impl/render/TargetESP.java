package elexis.modules.impl.render;

import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.*;
import elexis.modules.impl.render.hud.HudRenderUtil;
import elexis.modules.impl.render.javelin.target.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Javelin's procedural target models, rendered by the native 1.21.11 backend. */
@FunctionAdd(name="TargetESP", alias="TargetESP", category=Category.Render,
        description="Модели, кометы, лезвия и молнии вокруг выбранной цели")
public final class TargetESP extends Function {
   public static TargetESP instance;
   public final ModeSetting mode = new ModeSetting("Режим", "Ring", "Ring", "Crystals", "Bord", "Blades", "Маркер", "Спермо", "Lightning");
   public final SliderSetting markerSize = new SliderSetting("Размер маркера", 0.4875F, 0.2F, 1.6F, 0.0125F);
   public final SliderSetting markerBright = new SliderSetting("Яркость маркера", 2.0F, 1.0F, 4.0F, 0.25F);
   public final ModeSetting markerStyle = new ModeSetting("Стиль маркера", "Обычный", "Обычный", "Шевроны", "Уголки");
   public final ModeSetting bordPath = new ModeSetting("Траектория", "Орбита", "Орбита", "Спираль");
   public final ModeSetting bordCount = new ModeSetting("Комет", "3", "2", "3", "4", "5");
   public final SliderSetting bordSpeed = new SliderSetting("Скорость комет", 1.0F, 0.2F, 3.0F, 0.1F);
   public final BooleanSetting motionTrail = new BooleanSetting("Следовать", true);
   public final SliderSetting orbitTrailLength = new SliderSetting("Длина следа", 63.0F, 6.0F, 63.0F, 1.0F);
   public final ModeSetting bladesStyle = new ModeSetting("Стиль лезвий", "Tilted", "Tilted", "Flat", "Grinder", "Gyro");
   public final ModeSetting bladeCount = new ModeSetting("Лезвия", "4", "3", "4");
   public final SliderSetting bladeSpeed = new SliderSetting("Скорость лезвий", 1.0F, 0.2F, 4.0F, 0.1F);
   public final BooleanSetting throughWalls = new BooleanSetting("Через стены", false);
   public final ModeSetting lightningDepth = new ModeSetting("Глубина молний", "Авто", "Авто", "С глубиной", "Через стены");
   public final BooleanSetting sharpCrystals = new BooleanSetting("Острые кристаллы", false);
   public final ModeSetting colorMode = new ModeSetting("Цвет", "White", "Theme", "White", "Custom");
   public final ColorSetting customColor = new ColorSetting("Свой цвет", -7504649);
   public final Animation showAnim = new Animation(400L, Easing.EASE_OUT_EXPO);
   public Entity lastTarget;
   public final BeginShapeImpl dissolve = new BeginShapeImpl();
   public final Lightning lightning = new Lightning();
   public Vec3d lastTargetPosition;
   public Object targetLevel;
   public boolean targetPresent;
   public boolean captureShape;
   public long lastFrameTime;
   public static final String MARKER_CLASSIC = "Обычный";
   public static final String MARKER_CHEVRONS = "Шевроны";
   public static final String MARKER_CORNERS = "Уголки";
   public static final String BORD_ORBIT = "Орбита";
   public static final String BORD_SPIRAL = "Спираль";
   public static final Identifier MARKER_TEX_CLASSIC = Identifier.of("javelin", "images/target.png");
   public static final Identifier MARKER_TEX_CHEVRONS = Identifier.of("javelin", "images/markers/chev_out6.png");
   public static final Identifier MARKER_TEX_CORNERS = Identifier.of("javelin", "images/markers/corners_diag.png");
   public float markerSpin;
   public float markerSpinSpeed;
   public boolean markerSpinReverse;
   public float markerImpact;
   public static final int ORBIT_COUNT = 5;
   public static final int ORBIT_TRAIL_MAX_LENGTH = 63;
   public final Vec3d[] orbitPositions = new Vec3d[5];
   public final List<List<Vec3d>> orbitTrails = new ArrayList<>();
   public Vec3d orbitTrailCenter;
   public final Animation orbitShrinkAnim = new Animation(300L, Easing.EASE_OUT_EXPO);
   public float orbitAngle;
   public float ringImpact;
   public final List<RingMote> ringMotes = new ArrayList<>();
   public float ringMoteCarry;
   public float bladeAngle;
   public float bladeImpact;
   public final List<RingMote> bladeSparks = new ArrayList<>();
   public float bladeSparkCarry;
   public float spermoImpact;
   public String renderedMode;
   public final Matrix4f crystalTransform = new Matrix4f();
   public final Vector3f crystalVertex = new Vector3f();
   public final float[] octaOut = new float[9];
   public static final float[][] OCTA_TIPS_SHARP = new float[][]{{1.2F, 0.0F, 0.0F}, {-1.2F, 0.0F, 0.0F}};
   public static final float[][] OCTA_TIPS_BLUNT = new float[][]{{1.0F, 0.0F, 0.0F}, {-1.0F, 0.0F, 0.0F}};
   public static final float[][] OCTA_RING_SHARP = new float[][]{{0.0F, 0.35F, 0.0F}, {0.0F, 0.0F, 0.35F}, {0.0F, -0.35F, 0.0F}, {0.0F, 0.0F, -0.35F}};
   public static final float[][] OCTA_RING_BLUNT = new float[][]{{0.0F, 0.7F, 0.0F}, {0.0F, 0.0F, 0.7F}, {0.0F, -0.7F, 0.0F}, {0.0F, 0.0F, -0.7F}};


   public TargetESP() {
      instance = this;
      for (int i = 0; i < 5; i++) orbitTrails.add(new ArrayList<>());
      addSettings(mode, markerSize, markerBright, markerStyle, bordPath, bordCount, bordSpeed, motionTrail, orbitTrailLength, bladesStyle, bladeCount, bladeSpeed, throughWalls, lightningDepth, sharpCrystals, colorMode, customColor);
      markerStyle.setVisible(() -> mode.is("Маркер"));
      markerSize.setVisible(() -> mode.is("Маркер"));
      markerBright.setVisible(() -> mode.is("Маркер"));
      sharpCrystals.setVisible(() -> mode.is("Crystals"));
      bordPath.setVisible(() -> mode.is("Bord"));
      bordCount.setVisible(() -> mode.is("Bord"));
      bordSpeed.setVisible(() -> mode.is("Bord"));
      motionTrail.setVisible(() -> mode.is("Bord"));
      orbitTrailLength.setVisible(() -> mode.is("Bord"));
      bladesStyle.setVisible(() -> mode.is("Blades"));
      bladeCount.setVisible(() -> mode.is("Blades"));
      bladeSpeed.setVisible(() -> mode.is("Blades"));
      throughWalls.setVisible(() -> !mode.is("Lightning"));
      lightningDepth.setVisible(() -> mode.is("Lightning"));
      customColor.setVisible(() -> colorMode.is("Custom"));
   }

   @Override public void onDisable() {
      reset();
      RenderGlowingUtil.close();
      BeginUtil2.close();
      super.onDisable();
   }

   public void reset() {
      lightning.close();
      lastTarget = null;
      lastTargetPosition = null;
      targetPresent = false;
      targetLevel = null;
      captureShape = false;
      dissolve.clear();
      showAnim.setValue(0);
      clearDynamicEffectState();
      renderedMode = null;
      lastFrameTime = 0;
   }

   @EventHandler public void render(EventRender.World event) {
      if (nullCheck()) { reset(); return; }
      if (!mode.modes.contains(mode.get())) mode.set("Ring");
      if (targetLevel != mc.world) reset();
      targetLevel = mc.world;
      TargetRender.frame(event.getMatrixStack().peek().getPositionMatrix());
      long now = System.currentTimeMillis();
      float dt = lastFrameTime == 0 ? 0.016f : MathHelper.clamp((now-lastFrameTime)/1000f, 0f, 0.05f);
      lastFrameTime = now;
      LivingEntity active = currentTarget() instanceof LivingEntity living && living != mc.player
              && !(living instanceof ArmorStandEntity) && living.isAlive() && !living.isRemoved() ? living : null;
      boolean switched = active != null && active != lastTarget;
      boolean isLightning = mode.is("Lightning");
      if (!isLightning && targetPresent && (active == null || switched) && lastTargetPosition != null
              && lastTarget instanceof LivingEntity previous) {
         dissolve.burst(lastTargetPosition.add(0, previous.getHeight()*0.5, 0), now);
      }
      if (switched && !isLightning) { clearDynamicEffectState(); showAnim.setValue(0); }
      targetPresent = active != null;
      if (active != null) lastTarget = active;
      Entity tracked = active != null ? active : isLightning ? lastTarget : null;
      if (tracked != null && tracked.getEntityWorld() == mc.world) lastTargetPosition = tracked.getLerpedPos(event.getTicks());
      showAnim.setDuration(isLightning ? 150 : 400);
      showAnim.setEasing(isLightning ? Easing.LINEAR : Easing.EASE_OUT_EXPO);
      showAnim.animate(targetPresent);
      showAnim.update();
      float show = (float)showAnim.get();
      if (show > 0.005f && lastTarget instanceof LivingEntity living && lastTargetPosition != null) {
         captureShape = targetPresent && !isLightning;
         if (captureShape) dissolve.beginShape(dissolveSampleLimit());
         try { renderSelectedMode(living, lastTargetPosition.x, lastTargetPosition.y, lastTargetPosition.z, now, dt, show); }
         finally { captureShape = false; }
      }
      dissolve.render(now, mode.is("Маркер") || throughWalls.get());
      if (show <= 0.005f && !targetPresent && !dissolve.hasActive()) reset();
   }

   public void renderSelectedMode(LivingEntity target, double x, double y, double z, long now, float dt, float show) {
      if (!mode.get().equals(renderedMode)) { clearDynamicEffectState(); renderedMode = mode.get(); }
      switch (mode.get()) {
         case "Crystals" -> renderCrystals(target,x,y,z,mc.getRenderTickCounter().getTickProgress(true),show);
         case "Bord" -> renderBord(target,x,y,z,dt,show);
         case "Blades" -> renderBlades(target,x,y,z,now,dt,show);
         case "Маркер" -> renderMarker(target,x,y,z,now,dt,show);
         case "Спермо" -> renderSpermo(target,x,y,z,now,dt,show);
         case "Lightning" -> renderLightning(target,x,y,z,mc.getRenderTickCounter().getTickProgress(true),dt,show);
         default -> renderRing(target,x,y,z,now,dt,show);
      }
   }

   public int dissolveSampleLimit() {
      return switch(mode.get()) {
         case "Blades" -> 1200;
         case "Ring" -> 1100;
         case "Маркер" -> 768;
         case "Bord" -> 640;
         default -> 512;
      };
   }

   public void clearDynamicEffectState() {
      for (int i = 0; i < 5; i++) {
         this.orbitPositions[i] = null;
         this.orbitTrails.get(i).clear();
      }

      this.orbitTrailCenter = null;
      this.ringImpact = 0.0F;
      this.ringMotes.clear();
      this.ringMoteCarry = 0.0F;
      this.bladeAngle = 0.0F;
      this.bladeImpact = 0.0F;
      this.bladeSparks.clear();
      this.bladeSparkCarry = 0.0F;
      this.spermoImpact = 0.0F;
   }

   public void followTargetWithOrbitTrail(Vec3d center) {
      if (this.orbitTrailCenter == null) {
         this.orbitTrailCenter = center;
      } else {
         Vec3d delta = center.subtract(this.orbitTrailCenter);
         this.orbitTrailCenter = center;
         if (!this.motionTrail.get() && !(delta.lengthSquared() < 1.0E-10)) {
            for (int i = 0; i < 5; i++) {
               if (this.orbitPositions[i] != null) {
                  this.orbitPositions[i] = this.orbitPositions[i].add(delta);
               }

               List<Vec3d> trail = this.orbitTrails.get(i);

               for (int point = 0; point < trail.size(); point++) {
                  trail.set(point, trail.get(point).add(delta));
               }
            }
         }
      }
   }

   public Vec3d effectCameraPosition() {
      return mc.gameRenderer.getCamera().getCameraPos();
   }

   public void renderLightning(LivingEntity target, double x, double y, double z, float partialTicks, float dt, float show) {
      float hurt = Math.max(0.0F, target.hurtTime - partialTicks);
      float flash = MathHelper.sin(hurt * (float) (Math.PI / 10)) * 0.5F;
      this.lightning
         .render(
            x,
            y,
            z,
            target.getHeight(),
            target.getWidth(),
            this.effectCameraPosition(),
            dt,
            show,
            lerpColor(this.baseColor(), -65536, flash),
            this.lightningThroughWalls(target)
         );
   }

   public boolean lightningThroughWalls(LivingEntity target) {
      if (this.lightningDepth.is("Через стены")) {
         return true;
      } else if (!this.lightningDepth.is("С глубиной")) {
         Vec3d eye = mc.player.getEyePos();
         Box box = target.getBoundingBox();
         Vec3d closest = new Vec3d(MathHelper.clamp(eye.x, box.minX, box.maxX), MathHelper.clamp(eye.y, box.minY, box.maxY), MathHelper.clamp(eye.z, box.minZ, box.maxZ));
         return mc.world.raycast(new RaycastContext(this.effectCameraPosition(), closest, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.BLOCK;
      } else {
         return false;
      }
   }

   public void renderMarker(LivingEntity living, double tx, double ty, double tz, long now, float dt, float show) {
      this.markerSpin = this.markerSpin + this.markerSpinSpeed * (dt * 60.0F);
      this.markerSpinSpeed = this.markerSpinSpeed + (this.markerSpinReverse ? -0.01F : 0.01F) * (dt * 60.0F);
      if (this.markerSpinSpeed > 2.3F) {
         this.markerSpinSpeed = 2.3F;
         this.markerSpinReverse = true;
      } else if (this.markerSpinSpeed < -2.3F) {
         this.markerSpinSpeed = -2.3F;
         this.markerSpinReverse = false;
      }

      this.markerSpin %= 360.0F;
      boolean hurt = living.hurtTime > 0;
      this.markerImpact = this.markerImpact + ((hurt ? 1.0F : 0.0F) - this.markerImpact) * Math.min(1.0F, dt * (hurt ? 16.0F : 3.0F));
      float pulse = 1.0F + 0.15F * MathHelper.sin((float)(now % 2000L) / 2000.0F * (float) (Math.PI * 2));
      float hurtScale = MathHelper.clamp(1.0F - 0.25F * this.markerImpact, 0.7F, 1.0F);
      float half = this.markerSize.get() * show * pulse * hurtScale;
      int argb = withAlpha(lerpColor(this.baseColor(), -53200, this.markerImpact * 0.92F), show);
      float intensity = this.markerBright.get() * (1.0F + this.markerImpact * 2.4F);
      RenderGlowingUtil.begin();
      double my = ty + living.getHeight() * 0.5;
      int passes = (int)intensity;

      for (int i = 0; i < passes; i++) {
         this.effectSprite(tx, my, tz, half, this.markerSpin, argb);
      }

      float rest = intensity - passes;
      if (rest > 0.01F) {
         this.effectSprite(tx, my, tz, half, this.markerSpin, withAlpha(argb, show * rest));
      }

      RenderGlowingUtil.flush(this.markerTexture(), true);
   }

   public void renderSpermo(LivingEntity target, double tx, double ty, double tz, long now, float dt, float show) {
      boolean hurt = target.hurtTime > 0;
      this.spermoImpact = this.spermoImpact + ((hurt ? 1.0F : 0.0F) - this.spermoImpact) * Math.min(1.0F, dt * (hurt ? 16.0F : 3.0F));
      int color = lerpColor(this.baseColor(), -57312, this.spermoImpact * 0.85F);
      int core = lift(color, 0.5F);
      float hScale = target.getHeight() / 1.8F;
      float wScale = Math.max(target.getWidth(), 0.3F) / 0.6F;
      double rotationTime = now / 384.61539872299335 * 1.65;
      float fixedSize = 0.23F * wScale;
      float radius = 0.74F * wScale;
      float alpha = show * 0.35F;
      RenderGlowingUtil.begin();

      for (int branch = 0; branch < 4; branch++) {
         for (int index = 2; index < 13; index++) {
            float sizeFactor = 1.65F - index / 2.25F * 0.2F;
            double trailRotationTime = rotationTime - index * 0.1;
            double cycle = (Math.sin(-index * 0.08) + 1.0) * 0.5;
            double baseAngle = Math.toRadians(branch * 90.0 + trailRotationTime * 65.0 % 360.0);
            double px = tx + Math.cos(baseAngle) * radius;
            double pz = tz + Math.sin(baseAngle) * radius;
            double py = ty + (branch % 2 == 0 ? 0.1 + 1.6 * cycle : 2.1 - 1.6 * cycle) * hScale;
            float curSize = fixedSize * sizeFactor;
            this.effectPoint(px, py, pz, curSize * 1.75F, withAlpha(color, alpha * 0.7F));
            this.effectPoint(px, py, pz, curSize, withAlpha(core, alpha));
         }

         radius *= -1.0F;
      }

      RenderGlowingUtil.flush(RenderGlowingUtil.BLOOM, this.throughWalls.get());
   }

   public void effectPoint(double x, double y, double z, float size, int color) {
      if (this.captureShape) {
         this.dissolve.sample(x, y, z, size * 0.3F, color);
      }

      RenderGlowingUtil.glowPoint(x, y, z, size, color);
   }

   public void effectBeam(double ax, double ay, double az, double bx, double by, double bz, float width, int color) {
      if (this.captureShape) {
         for (int i = 0; i <= 8; i++) {
            double t = i / 8.0;
            this.dissolve.sample(ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t, width, color);
         }
      }

      RenderGlowingUtil.glowBeam(ax, ay, az, bx, by, bz, width, color);
   }

   public void effectSprite(double x, double y, double z, float size, float spin, int color) {
      if (this.captureShape) {
         Quaternionf rotation = mc.gameRenderer.getCamera().getRotation();
         Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(rotation);
         Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(rotation);

         for (int i = 0; i < 32; i++) {
            double a = (Math.PI * 2) * i / 32.0 + Math.toRadians(spin);
            double dx = Math.cos(a) * size * 0.8;
            double dy = Math.sin(a) * size * 0.8;
            this.dissolve.sample(x + right.x * dx + up.x * dy, y + right.y * dx + up.y * dy, z + right.z * dx + up.z * dy, 0.025F, color);
         }
      }

      RenderGlowingUtil.glowSprite(x, y, z, size, spin, color);
   }

   public Identifier markerTexture() {
      if (this.markerStyle.is("Шевроны")) {
         return MARKER_TEX_CHEVRONS;
      } else {
         return this.markerStyle.is("Уголки") ? MARKER_TEX_CORNERS : MARKER_TEX_CLASSIC;
      }
   }

   public Entity currentTarget() {
      return HudRenderUtil.currentHudTarget();
   }

   public int baseColor() {
      return switch ((String)this.colorMode.get()) {
         case "White" -> -854529;
         case "Custom" -> this.customColor.get();
         default -> elexis.client.utils.render.color.basic.ThemeColorUtils.getIconColor(7, 0);
      };
   }

   public static int withAlpha(int color, float alpha) {
      int a = MathHelper.clamp((int)(alpha * 255.0F), 0, 255);
      return a << 24 | color & 16777215;
   }

   public static int lift(int color, float amount) {
      int r = (int)((color >> 16 & 0xFF) + (255 - (color >> 16 & 0xFF)) * amount);
      int g = (int)((color >> 8 & 0xFF) + (255 - (color >> 8 & 0xFF)) * amount);
      int b = (int)((color & 0xFF) + (255 - (color & 0xFF)) * amount);
      return color & 0xFF000000 | r << 16 | g << 8 | b;
   }

   public void renderRing(LivingEntity target, double tx, double ty, double tz, long now, float dt, float show) {
      boolean hurt = target.hurtTime > 0;
      this.ringImpact = this.ringImpact + ((hurt ? 1.0F : 0.0F) - this.ringImpact) * Math.min(1.0F, dt * (hurt ? 16.0F : 3.0F));
      float height = target.getHeight() + 0.2F;
      float phase = (float)(now % 2500L) / 2500.0F;
      float orbit = phase * (float) (Math.PI * 2);
      float wave = 0.5F - 0.5F * (float)Math.cos(orbit);
      float speed = (float)Math.sin(orbit);
      float headY = height * wave;
      float tailY = headY - height * 0.13F * speed * Math.abs(speed);
      float trailFade = Math.abs(speed);
      float radius = Math.max(target.getWidth(), 0.6F) * (0.97F + 0.06F * (1.0F - wave)) - 1.1F;
      int color = lerpColor(this.baseColor(), -57312, this.ringImpact * 0.85F);
      int headColor = withAlpha(color, 0.5F * show);
      int tailColor = withAlpha(color, 0.16F * show * trailFade * trailFade);
      boolean throughWallsOn = this.throughWalls.get();
      Vec3d camPos = this.effectCameraPosition();
      float bx = (float)(tx - camPos.x);
      float by = (float)(ty - camPos.y);
      float bz = (float)(tz - camPos.z);
      BeginUtil2.begin();
      int segments = 92;

      for (int i = 0; i < segments; i++) {
         float a1 = (float)((Math.PI * 2) * i / segments);
         float a2 = (float)((Math.PI * 2) * (i + 1) / segments);
         float x1 = (float)Math.cos(a1) * radius;
         float z1 = (float)Math.sin(a1) * radius;
         float x2 = (float)Math.cos(a2) * radius;
         float z2 = (float)Math.sin(a2) * radius;
         BeginUtil2.tri(bx + x1, by + tailY, bz + z1, tailColor, bx + x1, by + headY, bz + z1, headColor, bx + x2, by + headY, bz + z2, headColor);
         BeginUtil2.tri(bx + x1, by + tailY, bz + z1, tailColor, bx + x2, by + headY, bz + z2, headColor, bx + x2, by + tailY, bz + z2, tailColor);
      }

      BeginUtil2.flushAdditive(throughWallsOn);
      RenderGlowingUtil.begin();
      int colorLift = lift(color, 0.55F);
      double px = 0.0;
      double pz = 0.0;

      for (int i = 0; i <= segments; i++) {
         double angle = (Math.PI * 2) * i / segments;
         double x = tx + Math.cos(angle) * radius;
         double z = tz + Math.sin(angle) * radius;
         if (i > 0) {
            int rim = lerpColor(color, colorLift, 0.5F + 0.5F * (float)Math.sin(angle * 2.0 + now * 0.003));
            this.effectBeam(px, ty + headY, pz, x, ty + headY, z, 0.045F, withAlpha(rim, 0.3F * show));
            this.effectBeam(px, ty + headY, pz, x, ty + headY, z, 0.013F, withAlpha(lift(rim, 0.6F), 0.95F * show));
         }

         px = x;
         pz = z;
      }

      ThreadLocalRandom random = ThreadLocalRandom.current();
      float spawnRate = (130.0F + this.ringImpact * 120.0F) * Math.max(0.25F, show);
      if (!this.targetPresent) {
         this.ringMoteCarry = 0.0F;
      } else {
         this.ringMoteCarry += spawnRate * dt;
      }

      int spawn = (int)this.ringMoteCarry;
      this.ringMoteCarry -= spawn;

      for (int i = 0; i < spawn && this.ringMotes.size() < 250; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double spread = radius * (0.9 + random.nextDouble() * 0.2);
         RingMote mote = new RingMote();
         mote.x = tx + Math.cos(angle) * spread;
         mote.y = ty + headY + (random.nextDouble() - 0.5) * 0.05;
         mote.z = tz + Math.sin(angle) * spread;
         mote.vx = Math.cos(angle) * (random.nextDouble() - 0.5) * 0.014;
         mote.vy = -0.0012 - random.nextDouble() * 0.0075;
         mote.vz = Math.sin(angle) * (random.nextDouble() - 0.5) * 0.014;
         mote.groundY = ty - 0.1;
         mote.maxAge = 11 + random.nextInt(10);
         mote.size = 0.014F + random.nextFloat() * 0.024F;
         mote.seed = random.nextFloat();
         mote.redSpeed = random.nextFloat(1.5F, 7.0F);
         this.ringMotes.add(mote);
      }

      float tickStep = dt * 20.0F;
      int moteBase = this.baseColor();
      int moteBaseLift = lift(moteBase, 0.55F);
      double moteDrag = Math.pow(0.96, tickStep);
      Iterator<RingMote> it = this.ringMotes.iterator();

      while (it.hasNext()) {
         RingMote mote = it.next();
         mote.x = mote.x + mote.vx * tickStep;
         mote.y = mote.y + mote.vy * tickStep;
         mote.z = mote.z + mote.vz * tickStep;
         mote.vx *= moteDrag;
         mote.vz *= moteDrag;
         mote.vy -= 0.0022 * tickStep;
         mote.age += tickStep;
         if (!(mote.age >= mote.maxAge) && !(mote.y < mote.groundY)) {
            mote.redness = mote.redness + (this.ringImpact - mote.redness) * Math.min(1.0F, dt * mote.redSpeed);
            float life = 1.0F - mote.age / mote.maxAge;
            float pulse = 0.8F + 0.2F * (float)Math.sin(mote.age * 0.62F + mote.seed * 40.0F);
            float moteAlpha = show * life * pulse;
            int flowing = lerpColor(moteBase, moteBaseLift, 0.5F + 0.5F * (float)Math.sin(mote.seed * 6.28 + now * 0.003));
            int moteColor = lerpColor(flowing, -57312, mote.redness * 0.85F);
            this.effectPoint(mote.x, mote.y, mote.z, mote.size * 2.8F, withAlpha(moteColor, moteAlpha * 0.22F));
            this.effectPoint(mote.x, mote.y, mote.z, mote.size, withAlpha(lift(moteColor, 0.5F), moteAlpha));
         } else {
            it.remove();
         }
      }

      RenderGlowingUtil.flush(RenderGlowingUtil.BLOOM, throughWallsOn);
   }

   public static int lerpColor(int from, int to, float t) {
      t = MathHelper.clamp(t, 0.0F, 1.0F);
      int r = (int)((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
      int g = (int)((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
      int b = (int)((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   public void renderCrystals(LivingEntity target, double tx, double ty, double tz, float partialTicks, float show) {
      float time = (mc.player.age + partialTicks) * 6.0F;
      float height = target.getHeight();
      float halfWidth = target.getWidth() * 0.5F;
      float pelvisY = height * 0.35F;
      float torsoY = height * 0.55F;
      float neckY = height * 0.74F;
      int color = this.baseColor();
      int bodyColor = withAlpha(lift(color, 0.3F), 0.78F * show);
      int glowColor = lift(color, 0.4F);
      boolean sharp = this.sharpCrystals.get();
      boolean throughWallsOn = this.throughWalls.get();
      Vec3d camPos = this.effectCameraPosition();
      float bx = (float)(tx - camPos.x);
      float by = (float)(ty - camPos.y);
      float bz = (float)(tz - camPos.z);
      int crystalCount = 18;
      Matrix4f transform = this.crystalTransform;
      Vector3f vertex = this.crystalVertex;
      BeginUtil2.begin();
      RenderGlowingUtil.begin();

      for (int i = 0; i < crystalCount; i++) {
         float s1 = (float)Math.sin(i * 1.7F + 0.3F) * 0.5F + 0.5F;
         float s2 = (float)Math.cos(i * 2.3F + 0.7F) * 0.5F + 0.5F;
         float s3 = (float)Math.sin(i * 3.1F + 1.1F) * 0.5F + 0.5F;
         float angle = time + i * (360.0F / crystalCount) + s1 * 12.0F;
         float radius = halfWidth + 0.25F + s3 * 0.15F;
         double angleRadians = Math.toRadians(angle);
         float cx = radius * (float)Math.cos(angleRadians);
         float cz = radius * (float)Math.sin(angleRadians);
         float cy = s2 * height;
         float scale = 0.18F * show;
         if (!(scale < 0.001F)) {
            float heightFraction = cy / height;
            float lookY = heightFraction < 0.33F ? pelvisY : (heightFraction < 0.6F ? torsoY : neckY);
            float dx = -cx;
            float dy = lookY - cy;
            float dz = -cz;
            float yaw = (float)Math.toDegrees(Math.atan2(dz, dx));
            float pitch = (float)Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            transform.identity().translate(bx + cx, by + cy, bz + cz).rotateY((float)Math.toRadians(-yaw)).rotateZ((float)Math.toRadians(pitch)).scale(scale);
            this.emitOctahedron(transform, vertex, sharp, bodyColor);
            this.effectPoint(tx + cx, ty + cy, tz + cz, scale * 2.6F, withAlpha(color, 0.16F * show));
            this.effectPoint(tx + cx, ty + cy, tz + cz, scale * 1.4F, withAlpha(glowColor, 0.3F * show));
         }
      }

      BeginUtil2.flush(throughWallsOn);
      RenderGlowingUtil.flush(RenderGlowingUtil.BLOOM, throughWallsOn);
   }

   public void emitOctahedron(Matrix4f transform, Vector3f v, boolean sharp, int argb) {
      float[][] tips = sharp ? OCTA_TIPS_SHARP : OCTA_TIPS_BLUNT;
      float[][] ring = sharp ? OCTA_RING_SHARP : OCTA_RING_BLUNT;
      float[] out = this.octaOut;

      for (float[] tip : tips) {
         for (int i = 0; i < 4; i++) {
            transformInto(transform, v, tip, out, 0);
            transformInto(transform, v, ring[i], out, 3);
            transformInto(transform, v, ring[(i + 1) % 4], out, 6);
            BeginUtil2.tri(out[0], out[1], out[2], out[3], out[4], out[5], out[6], out[7], out[8], argb);
         }
      }
   }

   public static void transformInto(Matrix4f m, Vector3f v, float[] point, float[] out, int offset) {
      v.set(point[0], point[1], point[2]);
      m.transformPosition(v);
      out[offset] = v.x;
      out[offset + 1] = v.y;
      out[offset + 2] = v.z;
   }

   public void renderBord(LivingEntity target, double tx, double ty, double tz, float dt, float show) {
      Vec3d center = new Vec3d(tx, ty + target.getHeight() / 2.0, tz);
      this.followTargetWithOrbitTrail(center);
      this.orbitAngle = this.orbitAngle + dt * 20.0F * 0.45454547F * 20.0F * this.bordSpeed.get();
      this.orbitShrinkAnim.animate(target.hurtTime > 7);
      this.orbitShrinkAnim.update();
      float shrink = (float)this.orbitShrinkAnim.get();
      int color = this.baseColor();
      float spring = Math.min(1.0F, 45.0F * dt);
      float targetWidth = target.getWidth();
      float height = target.getHeight();
      String path = this.bordPath.get();
      int comets = Integer.parseInt(this.bordCount.get());
      int trailLength = MathHelper.clamp(Math.round(this.orbitTrailLength.get()), 6, 63);

      for (int i = comets; i < 5; i++) {
         this.orbitPositions[i] = null;
         this.orbitTrails.get(i).clear();
      }

      RenderGlowingUtil.begin();

      for (int i = 0; i < comets; i++) {
         float angleOffset = i * 360.0F / comets;
         double radian = Math.toRadians(this.orbitAngle + angleOffset);
         float orbitRadius = 0.2F * (1.0F - shrink) + targetWidth * 0.75F;
         Vec3d anchor = center.add(this.bordOffset(path, i, comets, radian, orbitRadius, height));
         if (this.orbitPositions[i] == null || this.orbitPositions[i].squaredDistanceTo(anchor) > 200.0) {
            this.orbitPositions[i] = anchor;
         }

         this.orbitPositions[i] = this.orbitPositions[i].add(anchor.subtract(this.orbitPositions[i]).multiply(spring));
         List<Vec3d> trail = this.orbitTrails.get(i);
         if (trail.isEmpty() || trail.get(0).squaredDistanceTo(this.orbitPositions[i]) > 1.0E-4) {
            trail.add(0, this.orbitPositions[i]);
         }

         while (trail.size() > trailLength) {
            trail.remove(trail.size() - 1);
         }

         for (int j = 0; j < trail.size(); j++) {
            Vec3d p = trail.get(j);
            float offset = 1.0F - (float)j / trailLength;
            float opacity = (float)Math.pow(offset, 1.8) * show * 0.7F;
            float scale = (0.05F + offset * 0.15F) * 0.8F;
            this.effectPoint(p.x, p.y, p.z, scale, withAlpha(color, opacity));
         }

         if (!trail.isEmpty()) {
            Vec3d head = trail.get(0);
            this.effectPoint(head.x, head.y, head.z, 0.35F * show, withAlpha(color, 0.47F * show));
            this.effectPoint(head.x, head.y, head.z, 0.16F * show, withAlpha(lift(color, 0.7F), 0.9F * show));
         }
      }

      RenderGlowingUtil.flush(RenderGlowingUtil.BLOOM, this.throughWalls.get());
   }

   public Vec3d bordOffset(String path, int i, int comets, double radian, float r, float height) {
      if (path.equals("Спираль")) {
         double plane = i * Math.PI * 2.0 / comets;
         double climb = Math.sin(radian * 0.22 + plane);
         return new Vec3d(Math.sin(radian) * r, climb * height * 0.42, Math.cos(radian) * r);
      } else {
         double oy = 0.3 * Math.sin(Math.toRadians(this.orbitAngle / (i + 1.0F)));
         return new Vec3d(Math.sin(radian) * r, oy, Math.cos(radian) * r);
      }
   }

   public void renderBlades(LivingEntity target, double tx, double ty, double tz, long now, float dt, float show) {
      boolean hurt = target.hurtTime > 0;
      this.bladeImpact = this.bladeImpact + ((hurt ? 1.0F : 0.0F) - this.bladeImpact) * Math.min(1.0F, dt * (hurt ? 14.0F : 2.5F));
      float rev = this.bladeImpact;
      float spin = this.bladeSpeed.get();
      this.bladeAngle += dt * (2.6F + rev * 6.0F) * spin;
      String style = this.bladesStyle.get();
      boolean gyro = style.equals("Gyro");
      boolean grinder = style.equals("Grinder");
      double time = now / 1000.0;
      float height = target.getHeight();
      double orbitR = target.getWidth() * 0.5 + 0.33;
      double grindWave = grinder ? 0.5 - 0.5 * Math.cos(now % 3200L / 3200.0 * Math.PI * 2.0) : 0.0;
      double yFrac = grinder ? 0.8 - 0.45 * grindWave : 0.5;
      double yBase = ty + height * yFrac;
      float groundProx = grinder ? MathHelper.clamp((float)((0.5 - yFrac) / 0.15), 0.0F, 1.0F) : 0.0F;
      double tiltPhase = time * 0.55;
      float tiltAmp = style.equals("Tilted") ? height * 0.16F : 0.0F;
      int color = this.baseColor();
      int bright = lift(color, 0.5F);
      int hot = lift(color, 0.85F);
      Vec3d camPos = this.effectCameraPosition();
      BeginUtil2.begin();
      RenderGlowingUtil.begin();
      float arc = 1.45F + rev * 1.1F;
      float heightMax = 0.2F + rev * 0.06F;
      int blades = this.bladeCount.is("3") ? 3 : 4;
      double step = (Math.PI * 2) / blades;

      for (int i = 0; i < blades; i++) {
         double aHead = this.bladeAngle + i * step;
         double bladeTilt = gyro ? i * step + time * 0.3 : tiltPhase;
         float bladeAmp = gyro ? height * 0.35F : tiltAmp;
         emitSlashRibbon(aHead, arc, 18, heightMax, 0.75F, orbitR, yBase, bladeTilt, bladeAmp, tx, tz, camPos, color, bright, show);
         emitSlashRibbon(aHead, arc * 0.34F, 8, heightMax * 0.55F, 1.0F, orbitR, yBase, bladeTilt, bladeAmp, tx, tz, camPos, bright, hot, show);
         double pex = 0.0;
         double pey = 0.0;
         double pez = 0.0;
         int edgeSegs = 8;

         for (int k = 0; k <= edgeSegs; k++) {
            double u = 0.62 + 0.38 * k / edgeSegs;
            double a = aHead - arc * (1.0 - u);
            double hgt = heightMax * Math.sin(Math.PI * Math.pow(u, 1.35));
            double y = yBase + Math.sin(a + bladeTilt) * bladeAmp + hgt;
            double r = orbitR + hgt * 0.3;
            double x = tx + Math.cos(a) * r;
            double z = tz + Math.sin(a) * r;
            if (k > 0) {
               this.effectBeam(pex, pey, pez, x, y, z, 0.032F, withAlpha(color, 0.35F * show));
               this.effectBeam(pex, pey, pez, x, y, z, 0.011F, withAlpha(hot, 0.95F * show));
            }

            pex = x;
            pey = y;
            pez = z;
         }

         double tipY = yBase + Math.sin(aHead + bladeTilt) * bladeAmp;
         double tipX = tx + Math.cos(aHead) * orbitR;
         double tipZ = tz + Math.sin(aHead) * orbitR;
         this.effectPoint(tipX, tipY, tipZ, 0.12F, withAlpha(color, 0.35F * show));
         this.effectPoint(tipX, tipY, tipZ, 0.05F, withAlpha(hot, 0.95F * show));
      }

      ThreadLocalRandom random = ThreadLocalRandom.current();
      float sparkRate = grinder ? 6.0F + groundProx * (80.0F + rev * 80.0F) : 10.0F + rev * 120.0F;
      if (!this.targetPresent) {
         this.bladeSparkCarry = 0.0F;
      } else {
         this.bladeSparkCarry = this.bladeSparkCarry + sparkRate * Math.max(0.25F, show) * dt;
      }

      int spawn = (int)this.bladeSparkCarry;
      this.bladeSparkCarry -= spawn;

      for (int n = 0; n < spawn && this.bladeSparks.size() < 90; n++) {
         int i = random.nextInt(blades);
         double aHead = this.bladeAngle + i * step;
         double bladeTilt = gyro ? i * step + time * 0.3 : tiltPhase;
         float bladeAmp = gyro ? height * 0.35F : tiltAmp;
         double cosA = Math.cos(aHead);
         double sinA = Math.sin(aHead);
         double speed = (2.6 + rev * 6.0F) * spin * orbitR / 20.0;
         RingMote spark = new RingMote();
         spark.x = tx + cosA * orbitR;
         spark.z = tz + sinA * orbitR;
         if (grinder && groundProx > 0.05F) {
            spark.y = yBase;
            spark.vx = -sinA * speed * 0.5 + (random.nextDouble() - 0.5) * 0.05;
            spark.vy = 0.01 + random.nextDouble() * 0.04;
            spark.vz = cosA * speed * 0.5 + (random.nextDouble() - 0.5) * 0.05;
            spark.groundY = ty + 0.01;
         } else {
            spark.y = yBase + Math.sin(aHead + bladeTilt) * bladeAmp;
            spark.vx = -sinA * speed * (0.7 + random.nextDouble() * 0.5) + (random.nextDouble() - 0.5) * 0.02;
            spark.vy = (random.nextDouble() - 0.3) * 0.03;
            spark.vz = cosA * speed * (0.7 + random.nextDouble() * 0.5) + (random.nextDouble() - 0.5) * 0.02;
            spark.groundY = ty - 0.1;
         }

         spark.maxAge = 6 + random.nextInt(8);
         spark.size = 0.012F + random.nextFloat() * 0.018F;
         spark.seed = random.nextFloat();
         this.bladeSparks.add(spark);
      }

      float tickStep = dt * 20.0F;
      double sparkDrag = Math.pow(0.9, tickStep);
      Iterator<RingMote> it = this.bladeSparks.iterator();

      while (it.hasNext()) {
         RingMote spark = it.next();
         spark.x = spark.x + spark.vx * tickStep;
         spark.y = spark.y + spark.vy * tickStep;
         spark.z = spark.z + spark.vz * tickStep;
         spark.vx *= sparkDrag;
         spark.vz *= sparkDrag;
         spark.vy -= 0.003 * tickStep;
         spark.age += tickStep;
         if (spark.age >= spark.maxAge) {
            it.remove();
         } else {
            if (spark.y < spark.groundY) {
               if (!grinder) {
                  it.remove();
                  continue;
               }

               spark.y = spark.groundY;
               spark.vy = Math.abs(spark.vy) * 0.45;
               spark.vx *= 0.7;
               spark.vz *= 0.7;
            }

            float life = 1.0F - spark.age / spark.maxAge;
            float alpha = show * life;
            this.effectPoint(spark.x, spark.y, spark.z, spark.size * 2.4F, withAlpha(color, alpha * 0.2F));
            this.effectPoint(spark.x, spark.y, spark.z, spark.size, withAlpha(hot, alpha * 0.9F));
         }
      }

      boolean throughWallsOn = this.throughWalls.get();
      BeginUtil2.flushAdditive(throughWallsOn);
      RenderGlowingUtil.flush(RenderGlowingUtil.BLOOM, throughWallsOn);
   }

   public static void emitSlashRibbon(
      double aHead,
      float arc,
      int segs,
      float heightMax,
      float alphaScale,
      double orbitR,
      double yBase,
      double tiltPhase,
      float tiltAmp,
      double tx,
      double tz,
      Vec3d camPos,
      int tailColor,
      int tipColor,
      float show
   ) {
      float pTopX = 0.0F;
      float pTopY = 0.0F;
      float pTopZ = 0.0F;
      float pBotX = 0.0F;
      float pBotY = 0.0F;
      float pBotZ = 0.0F;
      int prevColor = 0;

      for (int k = 0; k <= segs; k++) {
         float u = (float)k / segs;
         double a = aHead - arc * (1.0 - u);
         double cosA = Math.cos(a);
         double sinA = Math.sin(a);
         float hgt = heightMax * (float)Math.sin(Math.PI * Math.pow(u, 1.35));
         double y = yBase + Math.sin(a + tiltPhase) * tiltAmp;
         double rTop = orbitR + hgt * 0.3;
         double rBot = orbitR - hgt * 0.3;
         float topX = (float)(tx + cosA * rTop - camPos.x);
         float topY = (float)(y + hgt - camPos.y);
         float topZ = (float)(tz + sinA * rTop - camPos.z);
         float botX = (float)(tx + cosA * rBot - camPos.x);
         float botY = (float)(y - hgt - camPos.y);
         float botZ = (float)(tz + sinA * rBot - camPos.z);
         float fade = (float)Math.pow(u, 1.6);
         int c = withAlpha(lerpColor(tailColor, tipColor, u), fade * alphaScale * show);
         if (k > 0) {
            BeginUtil2.tri(pTopX, pTopY, pTopZ, prevColor, pBotX, pBotY, pBotZ, prevColor, topX, topY, topZ, c);
            BeginUtil2.tri(pBotX, pBotY, pBotZ, prevColor, botX, botY, botZ, c, topX, topY, topZ, c);
         }

         pTopX = topX;
         pTopY = topY;
         pTopZ = topZ;
         pBotX = botX;
         pBotY = botY;
         pBotZ = botZ;
         prevColor = c;
      }
   }
}
