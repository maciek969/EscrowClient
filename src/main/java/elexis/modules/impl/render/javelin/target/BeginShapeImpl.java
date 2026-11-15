package elexis.modules.impl.render.javelin.target;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public final class BeginShapeImpl {
   public static final int MAX_SAMPLES = 1200;
   public static final int MAX_FRAGMENTS = 1600;
   public final List<Sample> samples = new ArrayList<>(1200);
   public final List<Fragment> fragments = new ArrayList<>(1600);
   public int sampleLimit = 1200;
   public int seen;

   public void beginShape() {
      this.beginShape(1200);
   }

   public void beginShape(int sampleLimit) {
      this.samples.clear();
      this.sampleLimit = MathHelper.clamp(sampleLimit, 1, 1200);
      this.seen = 0;
   }

   public void sample(double x, double y, double z, float size, int color) {
      if (color >>> 24 >= 12) {
         int index = this.seen++;
         int slot = index < this.sampleLimit ? index : ThreadLocalRandom.current().nextInt(index + 1);
         if (slot < this.sampleLimit) {
            Sample sample = new Sample(new Vec3d(x, y, z), MathHelper.clamp(size, 0.018F, 0.065F), color);
            if (index < this.sampleLimit) {
               this.samples.add(sample);
            } else {
               this.samples.set(slot, sample);
            }
         }
      }
   }

   public void burst(Vec3d center, long now) {
      ThreadLocalRandom random = ThreadLocalRandom.current();

      for (Sample sample : this.samples) {
         if (this.fragments.size() >= 1600) {
            break;
         }

         Vec3d radial = sample.position().subtract(center);
         double length = Math.hypot(radial.x, radial.z);
         double speed = random.nextDouble(0.25, 0.85);
         double jitter = 0.15;
         Vec3d velocity = new Vec3d(
            (length > 1.0E-5 ? radial.x / length * speed : 0.0) + random.nextDouble(-jitter, jitter),
            random.nextDouble(0.25, 0.85),
            (length > 1.0E-5 ? radial.z / length * speed : 0.0) + random.nextDouble(-jitter, jitter)
         );
         this.fragments.add(new Fragment(sample, velocity, now, random.nextLong(750L, 1350L)));
      }

      this.beginShape();
   }

   public void render(long now, boolean throughWalls) {
      if (!this.fragments.isEmpty()) {
         RenderGlowingUtil.begin();
         this.fragments.removeIf(fragmentx -> now - fragmentx.born() >= fragmentx.life());

         for (Fragment fragment : this.fragments) {
            float t = MathHelper.clamp((float)(now - fragment.born()) / (float)fragment.life(), 0.0F, 1.0F);
            double seconds = Math.max(0L, now - fragment.born()) / 1000.0;
            double travel = (1.0 - Math.exp(-1.65 * seconds)) / 1.65;
            Vec3d p = fragment.sample().position().add(fragment.velocity().multiply(travel)).add(0.0, seconds * seconds * 0.11, 0.0);
            int color = fragment.sample().color();
            float alpha = (color >>> 24) / 255.0F * (1.0F - t) * (1.0F - t);
            float size = fragment.sample().size() * (1.0F - t * 0.6F);
            RenderGlowingUtil.glowPoint(p.x, p.y, p.z, size * 2.3F, alpha(color, alpha * 0.4F));
            RenderGlowingUtil.glowPoint(p.x, p.y, p.z, size, alpha(color, Math.min(1.0F, alpha * 1.15F)));
         }

         RenderGlowingUtil.flush(RenderGlowingUtil.BLOOM, throughWalls);
      }
   }

   public boolean hasActive() {
      return !this.fragments.isEmpty();
   }

   public void clear() {
      this.beginShape();
      this.fragments.clear();
   }

   public static int alpha(int color, float alpha) {
      return MathHelper.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | color & 16777215;
   }
}
