package elexis.modules.impl.render.javelin.target;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Original seeded recursive lightning branches and analytic lightning fragment shader. */
public final class Lightning implements AutoCloseable {
    private static final RenderLayer DEPTH = layer(false);
    private static final RenderLayer XRAY = layer(true);
    private final Vector3f direction = new Vector3f();
    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate immediate;
    private VertexConsumer vertices;
    private float lightningTime;
    private int segments;

    private static RenderLayer layer(boolean throughWalls) {
        RenderPipeline pipeline = RenderPipelines.register(RenderPipeline.builder(
                        RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(Identifier.of("elexis", "javelin_target/lightning_" + throughWalls))
                .withVertexShader(Identifier.of("javelin", "core/target_lightning/line"))
                .withFragmentShader(Identifier.of("javelin", "core/target_lightning/lightning"))
                .withDepthTestFunction(throughWalls ? DepthTestFunction.NO_DEPTH_TEST : DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false).withCull(false).withBlend(BlendFunction.LIGHTNING)
                .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.DrawMode.LINES).build());
        return RenderLayer.of("javelin_target_lightning_" + throughWalls,
                RenderSetup.builder(pipeline).expectedBufferSize(262144).translucent().build());
    }

    public void render(double x,double y,double z,float height,float width,Vec3d camera,
                       float dt,float alpha,int color,boolean throughWalls) {
        if(alpha <= 0.0078f) return;
        if(allocator == null) {
            allocator = new BufferAllocator(1048576);
            immediate = VertexConsumerProvider.immediate(allocator);
        }
        lightningTime += MathHelper.clamp(dt, 0, 0.05f);
        vertices = immediate.getBuffer(throughWalls ? XRAY : DEPTH);
        float radius = width * 0.75f + 0.16f;
        segments = 0;
        buildLightningBolts((float)(x-camera.x),(float)(y-camera.y),height,(float)(z-camera.z),radius,
                color,alpha,18f,0.16f);
        buildLightningBolts((float)(x-camera.x),(float)(y-camera.y),height,(float)(z-camera.z),radius,
                color,alpha,4f,1f);
        immediate.draw();
        vertices = null;
    }

    public void emitLine(float x1,float y1,float z1,float x2,float y2,float z2,int color1,int color2,float width) {
        if(segments >= 2048) return;
        direction.set(x2-x1,y2-y1,z2-z1);
        if(direction.lengthSquared() < 1e-10f) return;
        direction.normalize();
        TargetRender.MATRIX.transformDirection(direction);
        vertices.vertex(TargetRender.MATRIX,x1,y1,z1).color(color1)
                .normal(direction.x,direction.y,direction.z).lineWidth(width);
        vertices.vertex(TargetRender.MATRIX,x2,y2,z2).color(color2)
                .normal(direction.x,direction.y,direction.z).lineWidth(width);
        segments++;
    }

    @Override public void close() {
        lightningTime = 0;
        vertices = null;
        immediate = null;
        if(allocator != null) allocator.close();
        allocator = null;
    }

   public static int multiplyAlpha(int color, float factor) {
      return MathHelper.clamp(Math.round((color >>> 24) * factor), 0, 255) << 24 | color & 16777215;
   }

   public void buildLightningBolts(float originX, float baseY, float height, float originZ, float radius, int color, float alpha, float width, float layerAlpha) {
      float noiseTime = this.lightningTime * 6.0F;
      int noiseFrame = (int)noiseTime;
      float noiseBlend = noiseTime - noiseFrame;

      for (int bolt = 0; bolt < 3; bolt++) {
         float cycleTime = this.lightningTime * 1.3F + bolt / 3.0F;
         int cycle = (int)Math.floor(cycleTime);
         float phase = cycleTime - cycle;
         float fadeIn = MathHelper.clamp(phase / 0.08F, 0.0F, 1.0F);
         float fadeOut = 1.0F - MathHelper.clamp((phase - 0.42F) / 0.3F, 0.0F, 1.0F);
         float flicker = MathHelper.clamp(0.55F + 0.45F * smoothNoise(bolt, 900, 0, noiseFrame, noiseBlend), 0.0F, 1.0F);
         float visibility = fadeIn * fadeOut * flicker;
         if (!(visibility <= 0.01F)) {
            float boltAlpha = alpha * visibility * layerAlpha;
            float rotation = this.lightningTime * 0.9F;
            float bob = height * 0.16F * (float)Math.sin(this.lightningTime * 1.7F + bolt * 2.1F);
            float startAngle = hashNoise(bolt, 11, cycle) * (float) Math.PI;
            float turns = 0.6F + 0.5F * Math.abs(hashNoise(bolt, 12, cycle));
            float direction = hashNoise(bolt, 13, cycle) >= 0.0F ? 1.0F : -1.0F;
            float top = baseY + height * (0.85F + 0.05F * hashNoise(bolt, 14, cycle));
            float drop = height * 0.67F;
            float boltRadius = radius * (1.0F + 0.12F * hashNoise(bolt, 15, cycle));
            float previousX = 0.0F;
            float previousY = 0.0F;
            float previousZ = 0.0F;

            for (int anchor = 0; anchor <= 3; anchor++) {
               float progress = anchor / 3.0F;
               float angle = startAngle + rotation + direction * turns * (float) (Math.PI * 2) * progress;
               float x = originX + (float)Math.cos(angle) * boltRadius;
               float z = originZ + (float)Math.sin(angle) * boltRadius;
               float y = top - drop * progress + bob;
               if (anchor > 0) {
                  int seed = (bolt * 131 + anchor) * 17 + cycle * 101 | 1;
                  this.buildBoltSegment(previousX, previousY, previousZ, x, y, z, 4, width, boltAlpha, boltAlpha, color, seed, 0, noiseFrame, noiseBlend);
               }

               previousX = x;
               previousY = y;
               previousZ = z;
            }
         }
      }
   }

   public void buildBoltSegment(
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      int depth,
      float width,
      float alpha1,
      float alpha2,
      int color,
      int seed,
      int branchDepth,
      int noiseFrame,
      float noiseBlend
   ) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float dz = z2 - z1;
      float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (depth > 0 && !(length < 1.0E-4F)) {
         float dirX = dx / length;
         float dirY = dy / length;
         float dirZ = dz / length;
         float refX = Math.abs(dirY) < 0.9F ? 0.0F : 1.0F;
         float refY = Math.abs(dirY) < 0.9F ? 1.0F : 0.0F;
         float crossX = -dirZ * refY;
         float crossY = dirZ * refX;
         float crossZ = dirX * refY - dirY * refX;
         float crossLength = (float)Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
         if (crossLength < 1.0E-5F) {
            crossLength = 1.0F;
         }

         float sideX = crossX / crossLength;
         float sideY = crossY / crossLength;
         float sideZ = crossZ / crossLength;
         float upX = dirY * sideZ - dirZ * sideY;
         float upY = dirZ * sideX - dirX * sideZ;
         float upZ = dirX * sideY - dirY * sideX;
         float offsetA = smoothNoise(seed, 0, 0, noiseFrame, noiseBlend);
         float offsetB = smoothNoise(seed, 1, 0, noiseFrame, noiseBlend);
         float jitter = length * 0.3F;
         float midX = (x1 + x2) * 0.5F + (sideX * offsetA + upX * offsetB) * jitter;
         float midY = (y1 + y2) * 0.5F + (sideY * offsetA + upY * offsetB) * jitter;
         float midZ = (z1 + z2) * 0.5F + (sideZ * offsetA + upZ * offsetB) * jitter;
         float midAlpha = (alpha1 + alpha2) * 0.5F;
         this.buildBoltSegment(x1, y1, z1, midX, midY, midZ, depth - 1, width, alpha1, midAlpha, color, seed * 2 + 7, branchDepth, noiseFrame, noiseBlend);
         this.buildBoltSegment(midX, midY, midZ, x2, y2, z2, depth - 1, width, midAlpha, alpha2, color, seed * 2 + 9, branchDepth, noiseFrame, noiseBlend);
         if (branchDepth < 2 && depth >= 2 && smoothNoise(seed, 2, 0, noiseFrame, noiseBlend) > 0.45F) {
            float branchA = smoothNoise(seed, 3, 0, noiseFrame, noiseBlend);
            float branchB = smoothNoise(seed, 4, 0, noiseFrame, noiseBlend);
            float branchX = dirX * 0.4F + sideX * branchA + upX * branchB;
            float branchY = dirY * 0.4F + sideY * branchA + upY * branchB;
            float branchZ = dirZ * 0.4F + sideZ * branchA + upZ * branchB;
            float branchLength = (float)Math.sqrt(branchX * branchX + branchY * branchY + branchZ * branchZ);
            if (branchLength < 1.0E-5F) {
               branchLength = 1.0F;
            }

            float branchScale = length * 0.75F;
            this.buildBoltSegment(
               midX,
               midY,
               midZ,
               midX + branchX / branchLength * branchScale,
               midY + branchY / branchLength * branchScale,
               midZ + branchZ / branchLength * branchScale,
               Math.min(depth - 1, 3),
               width * 0.55F,
               midAlpha * 0.7F,
               0.0F,
               color,
               seed * 3 + 13,
               branchDepth + 1,
               noiseFrame,
               noiseBlend
            );
         }
      } else {
         this.emitLine(x1, y1, z1, x2, y2, z2, multiplyAlpha(color, alpha1), multiplyAlpha(color, alpha2), width);
      }
   }

   public static float smoothNoise(int seed, int channel, int octave, int frame, float blend) {
      float current = hashNoise(seed, channel, frame * 4 + octave);
      float next = hashNoise(seed, channel, (frame + 1) * 4 + octave);
      float t = blend * blend * (3.0F - 2.0F * blend);
      return current + (next - current) * t;
   }

   public static float hashNoise(int seed, int channel, int frame) {
      int hash = seed * 374761393 + channel * 668265263 + frame * 1274126177;
      int mixed = (hash ^ hash >>> 13) * 1274126177;
      int value = mixed ^ mixed >>> 16;
      return (value & 65535) / 32768.0F - 1.0F;
   }
}
