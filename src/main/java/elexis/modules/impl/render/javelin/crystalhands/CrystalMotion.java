package elexis.modules.impl.render.javelin.crystalhands;

import net.minecraft.util.math.MathHelper;
public final class CrystalMotion {
   public static final CrystalPose STILL = new CrystalPose(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);

   public static CrystalPose motion(CrystalAnimation animation, float time) {
      return switch (animation) {
         case ROTATION -> STILL;
         case PULSE -> {
            float pulse = (float)Math.sin(time * 2.8F);
            yield new CrystalPose(1.0F + pulse * 0.14F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F + pulse * 0.18F, 0.0F);
         }
         case PENDULUM -> new CrystalPose(1.0F, (float)Math.sin(time * 1.1F) * 0.12F, (float)Math.sin(time * 1.8F) * 0.4F, 0.0F, 0.0F, 1.0F, 0.0F);
         case RESONANCE -> new CrystalPose(
            1.0F + (float)Math.sin(time * 4.0F) * 0.05F,
            (float)Math.sin(time * 1.5F) * 0.3F,
            0.0F,
            -time * 0.8F,
            (float)Math.sin(time * 2.1F) * 0.25F,
            1.0F + (float)Math.sin(time * 4.0F) * 0.12F,
            0.0F
         );
         case GYROSCOPE -> new CrystalPose(1.0F, time * 0.45F, time * 0.7F, -time * 0.8F, time * 0.55F, 1.0F, 0.0F);
         case FLOAT -> new CrystalPose(1.0F, (float)Math.sin(time) * 0.18F, 0.0F, 0.0F, 0.0F, 1.0F, (float)Math.sin(time * 1.6F) * 0.18F);
         case TUMBLE -> new CrystalPose(1.0F, time * 1.1F, time * 0.9F, time * 0.5F, -time * 0.6F, 1.0F, 0.0F);
      };
   }

   public static float fade(float progress) {
      return 1.0F - smooth((progress - 0.06F) / 0.94F);
   }

   public static int copies(CrystalExplosion explosion) {
      return switch (explosion) {
         case SCATTER, SPLIT -> 3;
         case TRAIL -> 4;
         default -> 1;
      };
   }

   public static CrystalBurst burst(CrystalExplosion explosion, float progress, float riseHeight, int seed, int copy) {
      float p = MathHelper.clamp(progress, 0.0F, 1.0F);
      float eased = 1.0F - (1.0F - p) * (1.0F - p) * (1.0F - p);

      return switch (explosion) {
         case RISE -> new CrystalBurst(0.0F, riseHeight * eased, 0.0F, 1.0F - p * 0.08F, 0.0F, 0.0F, 1.0F, -1);
         case SCATTER -> {
            float angle = (seed & 1023) * 0.31F + copy * (float) (Math.PI * 2.0 / 3.0);
            float radius = eased * 1.2F;
            yield new CrystalBurst(
               (float)Math.cos(angle) * radius,
               eased * (0.2F + copy * 0.22F) - p * p * 0.35F,
               (float)Math.sin(angle) * radius,
               1.0F - p * 0.45F,
               eased * (copy % 2 == 0 ? 2.4F : -2.4F),
               eased * 0.8F,
               1.0F,
               copy
            );
         }
         case IMPULSE -> new CrystalBurst(0.0F, 0.0F, 0.0F, 1.0F + (float)Math.sin(p * Math.PI) * 0.65F - p * 0.15F, 0.0F, 0.0F, 1.0F, -1);
         case COLLAPSE -> new CrystalBurst(0.0F, 0.0F, 0.0F, Math.max(0.01F, 1.0F - smooth(p)), eased * 0.8F, 0.0F, 1.0F, -1);
         case VORTEX -> {
            float angle = p * (float) (Math.PI * 2) + (seed & 1023) * 0.31F;
            float radius = (float)Math.sin(p * Math.PI) * 0.45F;
            yield new CrystalBurst(
               (float)Math.cos(angle) * radius,
               eased * riseHeight,
               (float)Math.sin(angle) * radius,
               1.0F - p * 0.55F,
               p * (float) (Math.PI * 2),
               (float)Math.sin(p * Math.PI) * 0.65F,
               1.0F,
               -1
            );
         }
         case SPLIT -> new CrystalBurst(0.0F, (copy - 1) * eased * 0.85F, 0.0F, 1.0F - p * 0.25F, (copy - 1) * eased * 1.8F, 0.0F, 1.0F, copy);
         case TRAIL -> {
            float local = MathHelper.clamp((p - copy * 0.07F) / (1.0F - copy * 0.07F), 0.0F, 1.0F);
            float rise = 1.0F - (1.0F - local) * (1.0F - local) * (1.0F - local);
            yield new CrystalBurst(0.0F, rise * riseHeight, 0.0F, 1.0F - local * 0.08F, 0.0F, 0.0F, copy == 0 ? 1.0F : smooth(p / 0.2F) * 0.22F, -1);
         }
      };
   }

   public static float smooth(float progress) {
      float p = MathHelper.clamp(progress, 0.0F, 1.0F);
      return p * p * (3.0F - 2.0F * p);
   }
}
