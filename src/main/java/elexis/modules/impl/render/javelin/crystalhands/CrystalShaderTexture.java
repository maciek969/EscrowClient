package elexis.modules.impl.render.javelin.crystalhands;

import net.minecraft.client.texture.NativeImage;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.resource.Resource;
import net.minecraft.util.math.MathHelper;
import elexis.modules.impl.render.CustomCrystal;

public final class CrystalShaderTexture {
   public static final int W = 128;
   public static final int H = 64;
   public static final long FRAME_MS = 33L;
   public static final Identifier ID = Identifier.of("javelin", "dynamic/crystal_shader");
   public static final Identifier VANILLA = Identifier.of("minecraft", "textures/entity/end_crystal/end_crystal.png");
   public static NativeImageBackedTexture texture;
   public static int[] mask;
   public static boolean maskFailed;
   public static long lastUpdate;
   public static float phase;

   public static Identifier identifier() {
      return ID;
   }

   public static void loadMask(MinecraftClient mc) {
      if (mask == null && !maskFailed) {
         try {
            Optional<Resource> res = mc.getResourceManager().getResource(VANILLA);
            if (res.isEmpty()) {
               maskFailed = true;
               return;
            }

            try (InputStream in = res.get().getInputStream()) {
               NativeImage src = NativeImage.read(in);

               try {
                  int[] m = new int[8192];

                  for (int y = 0; y < 64 && y < src.getHeight(); y++) {
                     for (int x = 0; x < 128 && x < src.getWidth(); x++) {
                        m[y * 128 + x] = src.getColorArgb(x, y) >>> 24 & 0xFF;
                     }
                  }

                  mask = m;
               } catch (Throwable var9) {
                  if (src != null) {
                     try {
                        src.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (src != null) {
                  src.close();
               }
            }
         } catch (Exception e) {
            maskFailed = true;
         }
      }
   }

   public static void update() {
      long now = System.currentTimeMillis();
      if (texture == null || now - lastUpdate >= 33L) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc.getTextureManager() != null) {
            loadMask(mc);
            if (texture == null) {
               texture = new NativeImageBackedTexture(ID::toString, 128, 64, false);
               mc.getTextureManager().registerTexture(ID, texture);
            }

            float dt = lastUpdate == 0L ? 0.0F : MathHelper.clamp((float)(now - lastUpdate) / 1000.0F, 0.0F, 0.1F);
            lastUpdate = now;
            phase = phase + dt * CustomCrystal.shaderSpeedValue();
            NativeImage img = texture.getImage();
            if (img != null) {
               int rgb = CustomCrystal.shaderRgb();
               float cr = (rgb >> 16 & 0xFF) / 255.0F;
               float cg = (rgb >> 8 & 0xFF) / 255.0F;
               float cb = (rgb & 0xFF) / 255.0F;
               String mode = CustomCrystal.shaderModeName();
               String animation = CustomCrystal.shaderAnimationName();
               float opacity = CustomCrystal.shaderOpacityValue();
               float flash = animation.equals("Вспышки") ? (float)Math.pow(Math.max(0.0F, MathHelper.sin(phase * 5.0F)), 10.0) : 0.0F;
               float rotation = animation.equals("Вращение") ? phase * 0.18F : 0.0F;
               float rotationCos = MathHelper.cos(rotation);
               float rotationSin = MathHelper.sin(rotation);
               float pulse = animation.equals("Пульсация") ? 0.72F + (0.5F + 0.5F * MathHelper.sin(phase * 2.2F)) * 0.28F : 1.0F;
               float scanOffset = animation.equals("Сканирование") ? frac(phase * 0.22F) : -1.0F;

               for (int y = 0; y < 64; y++) {
                  for (int x = 0; x < 128; x++) {
                     int a = mask != null ? mask[y * 128 + x] : 255;
                     if (a == 0) {
                        img.setColorArgb(x, y, 0);
                     } else {
                        float u = x / 128.0F;
                        float v = y / 64.0F;
                        float px = u - 0.5F;
                        float py = v - 0.5F;
                        switch (animation) {
                           case "Вращение":
                              u = 0.5F + px * rotationCos - py * rotationSin;
                              v = 0.5F + px * rotationSin + py * rotationCos;
                           default:
                              u = frac(u);
                              v = frac(v);
                              int alpha = a;
                              int g;
                              int b;
                              int r;
                              switch (mode) {
                                 case "Flat":
                                    alpha = Math.min(255, (int)(a * 1.45F));
                                    float shade = 0.9F + 0.1F * v;
                                    r = clamp(cr * shade * 255.0F);
                                    g = clamp(cg * shade * 255.0F);
                                    b = clamp(cb * shade * 255.0F);
                                    break;
                                 case "Туманность": {
                                    int c = nebula(u, v, cr, cg, cb);
                                    r = c >> 16 & 0xFF;
                                    g = c >> 8 & 0xFF;
                                    b = c & 0xFF;
                                    break;
                                 }
                                 case "Северное сияние": {
                                    int c = aurora(u, v, cr, cg, cb);
                                    r = c >> 16 & 0xFF;
                                    g = c >> 8 & 0xFF;
                                    b = c & 0xFF;
                                    break;
                                 }
                                 case "Электроразряд":
                                    float bolt = electric(u, v);
                                    float core = smoothstep(0.18F, 0.9F, bolt);
                                    r = clamp((cr * 0.35F + core * 0.65F) * 255.0F);
                                    g = clamp((cg * 0.45F + core * 0.55F) * 255.0F);
                                    b = clamp((cb * 0.7F + core * 0.3F) * 255.0F);
                                    break;
                                 case "Сетка":
                                    float line = grid(u, v);
                                    r = clamp(cr * line * 255.0F);
                                    g = clamp(cg * line * 255.0F);
                                    b = clamp(cb * line * 255.0F);
                                    alpha = clamp(a * (0.18F + line * 0.82F));
                                    break;
                                 case "Магма":
                                    float rock = fbm(u * 9.0F + phase * 0.1F, v * 9.0F - phase * 0.12F, 4);
                                    float crack = 1.0F - smoothstep(0.025F, 0.12F, Math.abs(rock - 0.5F));
                                    float heat = crack * (0.65F + 0.35F * MathHelper.sin(phase * 1.4F + v * 12.0F));
                                    r = clamp((0.07F + heat * 0.93F) * 255.0F);
                                    g = clamp((0.015F + heat * heat * 0.7F) * 255.0F);
                                    b = clamp((cb * rock * 0.08F + heat * heat * heat * 0.12F) * 255.0F);
                                    break;
                                 case "Голограмма":
                                    float stripe = 1.0F - smoothstep(0.08F, 0.3F, Math.abs(frac(v * 24.0F - phase * 0.5F) - 0.5F));
                                    float scan = 1.0F - smoothstep(0.01F, 0.1F, Math.abs(v - frac(phase * 0.2F)));
                                    r = clamp((cr * 0.25F + scan * 0.3F) * 255.0F);
                                    g = clamp((0.38F + cg * 0.35F + stripe * 0.25F) * 255.0F);
                                    b = clamp((0.65F + cb * 0.2F + scan * 0.15F) * 255.0F);
                                    alpha = clamp(a * (0.08F + stripe * 0.55F + scan * 0.35F));
                                    break;
                                 case "Сингулярность":
                                    float sx = frac(u * 4.0F) - 0.5F;
                                    float sy = frac(v * 2.0F) - 0.5F;
                                    float radius = MathHelper.sqrt(sx * sx + sy * sy);
                                    float angle = (float)Math.atan2(sy, sx);
                                    float ring = 1.0F - smoothstep(0.025F, 0.15F, Math.abs(radius - 0.25F));
                                    float streams = 0.5F + 0.5F * MathHelper.sin(angle * 6.0F + radius * 30.0F - phase * 2.4F);
                                    float energy = ring * (0.35F + streams * 0.65F);
                                    r = clamp((cr * energy + energy * energy * 0.4F) * 255.0F);
                                    g = clamp((cg * energy + energy * energy * 0.35F) * 255.0F);
                                    b = clamp((cb * energy + energy * energy * 0.3F) * 255.0F);
                                    alpha = clamp(a * (0.05F + energy * 0.95F));
                                    break;
                                 default:
                                    r = clamp(cr * 255.0F);
                                    g = clamp(cg * 255.0F);
                                    b = clamp(cb * 255.0F);
                              }

                              if (animation.equals("Пульсация")) {
                                 r = clamp(r * pulse);
                                 g = clamp(g * pulse);
                                 b = clamp(b * pulse);
                              } else if (scanOffset >= 0.0F) {
                                 float distance = Math.abs(v - scanOffset);
                                 float scan = 1.0F - smoothstep(0.015F, 0.11F, distance);
                                 float lift = scan * 0.8F;
                                 r = clamp(MathHelper.lerp(lift, r, 255.0F));
                                 g = clamp(MathHelper.lerp(lift, g, 255.0F));
                                 b = clamp(MathHelper.lerp(lift, b, 255.0F));
                              }

                              if (flash > 0.0F) {
                                 r = clamp(MathHelper.lerp(flash, r, 255.0F));
                                 g = clamp(MathHelper.lerp(flash, g, 255.0F));
                                 b = clamp(MathHelper.lerp(flash, b, 255.0F));
                              }

                              alpha = clamp(alpha * opacity);
                              img.setColorArgb(x, y, alpha << 24 | r << 16 | g << 8 | b);
                        }
                     }
                  }
               }

               texture.upload();
            }
         }
      }
   }

   public static int nebula(float u, float v, float cr, float cg, float cb) {
      float t = phase * 0.09F;
      float cloud = fbm(u * 4.2F + t, v * 4.2F - t * 0.6F, 5);
      float wisp = fbm(u * 9.5F - t * 1.3F, v * 9.5F + t * 0.4F, 3);
      float dens = MathHelper.clamp(cloud * 1.95F - 0.55F, 0.0F, 1.0F);
      dens = dens * dens * (0.65F + 0.35F * dens);
      float glow = MathHelper.clamp(wisp - 0.6F, 0.0F, 1.0F) * 1.7F;
      float r = 0.02F + cr * dens * 0.5F + glow * 0.3F;
      float g = 0.028F + cg * dens * 0.45F + glow * 0.34F;
      float b = 0.075F + cb * dens * 0.78F + glow * 0.42F;
      float star = stars(u, v);
      r += star;
      g += star;
      b += star;
      return clamp(r * 255.0F) << 16 | clamp(g * 255.0F) << 8 | clamp(b * 255.0F);
   }

   public static float stars(float u, float v) {
      float cell = 26.0F;
      float gx = u * cell;
      float gy = v * cell;
      int ix = MathHelper.floor(gx);
      int iy = MathHelper.floor(gy);
      float h = hash2(ix, iy);
      if (h < 0.8F) {
         return 0.0F;
      }

      float px = hash2(ix + 31, iy + 17);
      float py = hash2(ix + 71, iy + 53);
      float dx = gx - ix - px;
      float dy = gy - iy - py;
      float d = MathHelper.sqrt(dx * dx + dy * dy);
      float core = MathHelper.clamp(1.0F - d * 7.0F, 0.0F, 1.0F);
      float tw = 0.55F + 0.45F * MathHelper.sin(phase * 2.6F + h * 40.0F);
      return core * core * tw * 0.95F;
   }

   public static float fbm(float x, float y, int octaves) {
      float sum = 0.0F;
      float amp = 0.5F;
      float norm = 0.0F;

      for (int i = 0; i < octaves; i++) {
         sum += vnoise(x, y) * amp;
         norm += amp;
         x *= 2.03F;
         y *= 2.03F;
         amp *= 0.5F;
      }

      return sum / norm;
   }

   public static float vnoise(float x, float y) {
      int ix = MathHelper.floor(x);
      int iy = MathHelper.floor(y);
      float fx = x - ix;
      float fy = y - iy;
      fx = fx * fx * (3.0F - 2.0F * fx);
      fy = fy * fy * (3.0F - 2.0F * fy);
      float a = hash2(ix, iy);
      float b = hash2(ix + 1, iy);
      float c = hash2(ix, iy + 1);
      float d = hash2(ix + 1, iy + 1);
      return MathHelper.lerp(fy, MathHelper.lerp(fx, a, b), MathHelper.lerp(fx, c, d));
   }

   public static float hash2(int x, int y) {
      int n = x * 374761393 + y * 668265263;
      n = (n ^ n >> 13) * 1274126177;
      return ((n ^ n >> 16) & 2147483647) / 2.1474836E9F;
   }

   public static float grid(float u, float v) {
      float gx = Math.abs(frac(u * 12.0F + phase * 0.18F) - 0.5F);
      float gy = Math.abs(frac(v * 6.0F - phase * 0.12F) - 0.5F);
      float line = 1.0F - smoothstep(0.025F, 0.1F, Math.min(gx, gy));
      return 0.08F + line * 0.92F;
   }

   public static float electric(float u, float v) {
      float bend = (fbm(u * 5.0F + phase * 0.4F, v * 8.0F, 4) - 0.5F) * 0.28F;
      float bolt = Math.abs(MathHelper.sin((u + bend) * 34.0F + MathHelper.sin(v * 18.0F + phase) * 2.5F));
      float glow = 1.0F - smoothstep(0.02F, 0.2F, bolt);
      float pulse = 0.55F + 0.45F * MathHelper.sin(v * 11.0F - phase * 2.0F);
      return MathHelper.clamp(glow * pulse, 0.0F, 1.0F);
   }

   public static int aurora(float u, float v, float cr, float cg, float cb) {
      float flow = fbm(u * 5.0F + phase * 0.18F, v * 3.0F - phase * 0.08F, 4);
      float curtain = MathHelper.sin((u * 12.0F + flow * 3.0F) * (float) Math.PI + phase * 0.45F);
      float envelope = MathHelper.sin(MathHelper.clamp(v * 1.15F, 0.0F, 1.0F) * (float) Math.PI);
      float glow = MathHelper.clamp((curtain * 0.5F + 0.5F) * envelope * (0.35F + flow), 0.0F, 1.0F);
      float cool = 0.35F + 0.65F * glow;
      int r = clamp((cr * glow + 0.08F * cool) * 255.0F);
      int g = clamp((cg * glow + 0.48F * cool) * 255.0F);
      int b = clamp((cb * glow + 0.72F * cool) * 255.0F);
      return r << 16 | g << 8 | b;
   }

   public static float smoothstep(float edge0, float edge1, float x) {
      float t = MathHelper.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   public static int clamp(float v) {
      return MathHelper.clamp((int)v, 0, 255);
   }

   /** Дробная часть числа (аналог GLSL fract()). */
   public static float frac(float x) {
      return x - MathHelper.floor(x);
   }
}