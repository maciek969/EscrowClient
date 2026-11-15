package elexis.modules.impl.render.javelin.crystalhands;

import elexis.modules.impl.render.CustomCrystal;
import elexis.client.events.impl.render.EventRender;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;                       // исправлено: было net.minecraft.world.entity.Entity
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.joml.Quaternionf;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;

public final class CrystalEffects {
   public static final MinecraftClient MC = MinecraftClient.getInstance();
   public static final int MAX_GHOSTS = 18;
   public static final int BATCH_SIZE = 48;
   public static final double TRACK_DISTANCE_SQ = 2304.0;
   public static final double RENDER_DISTANCE_SQ = 9216.0;
   public static final List<CrystalGhost> GHOSTS = new ArrayList<>();
   public static MatrixStack POSE;
   public static final EndCrystalEntityRenderState STATE = new EndCrystalEntityRenderState();
   public static final Quaternionf ROTATION = new Quaternionf();
   public static EndCrystalEntityModel model;
   public static Object modelSource;
   public static Object trackedLevel;
   public static double effectTime;
   public static double lastGameSample = Double.NaN;
   public static boolean throughWalls;

   public static void clear() {
      GHOSTS.clear();
      trackedLevel = null;
      effectTime = 0.0;
      lastGameSample = Double.NaN;
   }

   public static void checkLevel() {
      if (trackedLevel != MC.world) {
         clear();
         trackedLevel = MC.world;
      }
   }

   public static void onRemove(EndCrystalEntity crystal, RemovalReason reason) {
      if (CustomCrystal.effectsActive()
              && MC.world != null
              && MC.player != null
              && crystal.getEntityWorld() == MC.world
              && !(MC.player.squaredDistanceTo(crystal) > 2304.0)
              && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
         checkLevel();
         CrystalOptions options = CustomCrystal.astralCrystalOptions();
         if (options.explosionEnabled()) {
            boolean emissive = CustomCrystal.astralActive();
            Identifier texture = emissive ? CrystalAstralTexture.ensure(options.lineWidth(), options.shape()) : CrystalShaderTexture.identifier();
            if (texture != null) {
               if (GHOSTS.size() >= 18) {
                  GHOSTS.removeFirst();
               }

               float partial = MC.getRenderTickCounter().getTickProgress(true);
               GHOSTS.add(
                       new CrystalGhost(
                               crystal.getX(),
                               crystal.getY(),
                               crystal.getZ(),
                               crystal.endCrystalAge + partial,
                               crystal.getId(),
                               CustomCrystal.scale(),
                               emissive ? CustomCrystal.astralRgb() : 16777215,
                               texture,
                               emissive,
                               options,
                               (float)effectTime,
                               ThreadLocalRandom.current().nextFloat(0.7F, 1.2F),
                               System.nanoTime()
                       )
               );
            }
         }
      }
   }

   public static boolean replaces(EndCrystalEntityRenderState state) {
      return MC.gameRenderer.getCamera().getCameraPos().squaredDistanceTo(state.x, state.y, state.z) <= 9216.0;
   }

   public static void tick() {
      if (MC.world != null && MC.player != null) {
         checkLevel();
         CrystalOptions options = CustomCrystal.astralCrystalOptions();
         if (!options.explosionEnabled()) {
            GHOSTS.clear();
         } else {
            long now = System.nanoTime();
            GHOSTS.removeIf(ghost -> ghost.progress(now) >= 1.0F);
         }
      } else {
         clear();
      }
   }

   public static void render(EventRender.World event) {
      POSE = event.getMatrixStack();
      if (MC.world != null && MC.player != null) {
         checkLevel();
         ensureModel();
         CrystalOptions options = CustomCrystal.astralCrystalOptions();
         if (model != null) {
            float partial = MC.getRenderTickCounter().getTickProgress(true);
            float time = sampleTime(partial, options.effectSpeed());
            Vec3d camera = MC.gameRenderer.getCamera().getCameraPos();
            if (CustomCrystal.astralActive()) {
               Identifier texture = CrystalAstralTexture.ensure(options.lineWidth(), options.shape());
               if (texture != null) {
                  float scale = CustomCrystal.scale();
                  int rgb = CustomCrystal.astralRgb();
                  begin(options.throughWalls(), texture);
                  int rendered = 0;

                  for (Entity entity : MC.world.getEntities()) {              // исправлено: net.minecraft.entity.Entity
                     if (entity instanceof EndCrystalEntity crystal && !crystal.isRemoved()) {
                        Vec3d position = crystal.getLerpedPos(partial);
                        if (!(camera.squaredDistanceTo(position) > 9216.0)) {
                           captureModel(
                                   position.x, position.y, position.z, crystal.endCrystalAge + partial, scale, rgb, time, crystal.getId(), options, camera, CrystalBurst.STILL
                           );
                           if (++rendered % 48 == 0) {
                              flush(texture, options.brightness(), true);
                              begin(options.throughWalls(), texture);
                           }
                        }
                     }
                  }

                  flush(texture, options.brightness(), true);
               }
            }

            if (!options.explosionEnabled()) {
               GHOSTS.clear();
            } else {
               long now = System.nanoTime();
               GHOSTS.removeIf(ghostx -> ghostx.progress(now) >= 1.0F);

               for (CrystalGhost ghost : GHOSTS) {
                  renderGhost(ghost, now, camera);
               }
            }
         }
      }
   }

   public static float sampleTime(float partial, float speed) {
      double sample = (double)MC.world.getTime() + partial;
      if (!Double.isNaN(lastGameSample) && !MC.isPaused()) {
         effectTime = effectTime + MathHelper.clamp(sample - lastGameSample, 0.0, 5.0) * 0.05 * speed;
      }

      lastGameSample = sample;
      return (float)effectTime;
   }

   public static void renderGhost(CrystalGhost ghost, long now, Vec3d camera) {
      CrystalOptions options = ghost.options();
      float progress = ghost.progress(now);
      float fade = CrystalMotion.fade(progress);
      float elapsed = (float)(now - ghost.born()) / 1.0E9F;
      float time = ghost.effectTime() + elapsed * options.effectSpeed();
      float age = ghost.age() + elapsed * 20.0F;
      begin(options.throughWalls(), ghost.texture());

      for (int i = 0; i < CrystalMotion.copies(options.explosion()); i++) {
         CrystalBurst pose = CrystalMotion.burst(options.explosion(), progress, ghost.riseHeight(), ghost.seed(), i);
         if (!(pose.opacity() <= 0.0F)) {
            if (!ghost.emissive()) {
               pose = new CrystalBurst(pose.x(), pose.y(), pose.z(), pose.scale(), pose.yaw(), pose.tilt(), pose.opacity() * fade, pose.part());
            }

            captureModel(
                    ghost.x() + pose.x() * ghost.scale(),
                    ghost.y() + pose.y(),
                    ghost.z() + pose.z() * ghost.scale(),
                    age,
                    ghost.scale(),
                    ghost.rgb(),
                    time,
                    ghost.seed(),
                    options,
                    camera,
                    pose
            );
         }
      }

      flush(ghost.texture(), options.brightness() * fade, ghost.emissive());
   }

   private static final BufferAllocator ALLOCATOR = new BufferAllocator(262144);
   private static final VertexConsumerProvider.Immediate CONSUMERS = VertexConsumerProvider.immediate(ALLOCATOR);
   private static VertexConsumer vertices;
   private static final java.util.Map<String, RenderLayer> LAYERS = new java.util.HashMap<>();
   private static final RenderPipeline XRAY = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
           .withLocation(Identifier.of("elexis", "javelin_crystal_xray"))
           .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false)
           .withCull(false).withBlend(BlendFunction.TRANSLUCENT).build());

   public static void begin(boolean walls, Identifier texture) {
      RenderLayer layer = walls ? LAYERS.computeIfAbsent(texture.toString(), key -> RenderLayer.of("javelin_crystal_" + key,
              RenderSetup.builder(XRAY).texture("Sampler0", texture).translucent().expectedBufferSize(262144).build()))
              : RenderLayers.entityTranslucentEmissive(texture);
      vertices = CONSUMERS.getBuffer(layer);
   }

   public static void flush(Identifier texture, float strength, boolean emissive) { CONSUMERS.draw(); }

   public static void ensureModel() {
      Object source = MC.getLoadedEntityModels();
      if (source != null && (model == null || modelSource != source)) {
         model = new EndCrystalEntityModel(MC.getLoadedEntityModels().getModelPart(EntityModelLayers.END_CRYSTAL));
         modelSource = source;
      }
   }

   public static void captureModel(
           double x, double y, double z, float age, float scale, int rgb, float time, int seed, CrystalOptions options, Vec3d camera, CrystalBurst burst
   ) {
      float phase = time + (seed & 1023) * 0.31F;
      CrystalPose motion = CrystalMotion.motion(options.animation(), phase);
      STATE.age = age;
      STATE.baseVisible = false;
      model.setAngles(STATE);
      model.base.visible = false;
      model.outerGlass.visible = model.innerGlass.visible = true;
      model.outerGlass.hidden = options.shape() == CrystalShape.CORE || burst.part() >= 0 && burst.part() != 0;
      model.innerGlass.hidden = options.shape() == CrystalShape.CORE || burst.part() >= 0 && burst.part() != 1;
      model.cube.visible = options.shape() != CrystalShape.CONTOUR && (burst.part() < 0 || burst.part() == 2);
      applyCrystalShape(options.shape(), phase);
      model.outerGlass.originY = model.outerGlass.originY + motion.lift() * 8.0F;
      model.outerGlass.rotate(ROTATION.rotationY(motion.yaw()).rotateZ(motion.tilt()));
      model.innerGlass.rotate(ROTATION.rotationY(motion.innerYaw()).rotateX(motion.innerTilt()));
      float modelScale = motion.scale();
      model.outerGlass.xScale *= modelScale;
      model.outerGlass.yScale *= modelScale;                     // исправлено: originYScale -> yScale
      model.outerGlass.zScale *= modelScale;
      POSE.push();
      POSE.translate(x - camera.x, y - camera.y, z - camera.z);
      POSE.scale(2.0F * scale, 2.0F * scale, 2.0F * scale);
      POSE.translate(0.0F, -0.5F, 0.0F);
      float centerY = model.outerGlass.originY / 16.0F;
      POSE.translate(0.0F, centerY, 0.0F);
      POSE.multiply(ROTATION.rotationY(burst.yaw()).rotateZ(burst.tilt()));
      POSE.scale(burst.scale(), burst.scale(), burst.scale());
      POSE.translate(0.0F, -centerY, 0.0F);
      model.render(POSE, vertices, 15728880, 0, tint(rgb, motion.brightness() * options.brightness(), burst.opacity()));
      POSE.pop();
   }

   public static void applyCrystalShape(CrystalShape shape, float phase) {
      switch (shape) {
         case CLASSIC:
         default:
            break;
         case VORTEX:
            model.outerGlass.yScale *= 1.25F;                    // исправлено: originYScale -> yScale
            model.outerGlass.rotate(ROTATION.rotationY(phase * 0.65F).rotateZ(0.22F));
            model.innerGlass.rotate(ROTATION.rotationY(-phase * 1.3F));
            model.innerGlass.yScale *= 0.8F;
            break;
         case CONSTELLATION:
            model.innerGlass.rotate(ROTATION.rotationY(-phase * 0.4F).rotateX(0.3F));
            model.cube.visible = false;
            break;
         case CONTOUR:
            model.cube.visible = false;
            model.outerGlass.originY = 24.0F;
            break;
         case FACETS:
            model.outerGlass.yScale *= 1.15F;                    // исправлено: originYScale -> yScale
            model.innerGlass.rotate(ROTATION.rotationY(phase * 0.7F));
            model.cube.rotate(ROTATION.rotationZ(-phase * 0.45F));
            break;
         case CORE:
            model.cube.xScale = model.cube.yScale = model.cube.zScale = 0.95F;
            model.cube.rotate(ROTATION.rotationY(-phase * 0.6F));
      }
   }

   public static int tint(int rgb, float brightness, float opacity) {
      int r = MathHelper.clamp(Math.round((rgb >>> 16 & 0xFF) * brightness), 0, 255);
      int g = MathHelper.clamp(Math.round((rgb >>> 8 & 0xFF) * brightness), 0, 255);
      int b = MathHelper.clamp(Math.round((rgb & 0xFF) * brightness), 0, 255);
      int alpha = MathHelper.clamp(Math.round(opacity * 255.0F), 0, 255);
      return alpha << 24 | r << 16 | g << 8 | b;
   }
}