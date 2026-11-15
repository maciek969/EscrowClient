package elexis.modules.impl.render.javelin.crystalhands;

import net.minecraft.client.texture.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.resource.Resource;
import net.minecraft.util.math.MathHelper;

public final class CrystalAstralTexture {
   public static final int W = 128;
   public static final int H = 64;
   public static final Identifier ID = Identifier.of("javelin", "dynamic/crystal_astral");
   public static final Identifier VANILLA = Identifier.of("minecraft", "textures/entity/end_crystal/end_crystal.png");
   public static final int[][] FACE_UVS = new int[][]{
      {0, 16, 16, 32},
      {16, 0, 32, 16},
      {16, 16, 32, 32},
      {32, 0, 48, 16},
      {32, 16, 48, 32},
      {48, 16, 64, 32},
      {64, 16, 80, 32},
      {80, 0, 96, 16},
      {80, 16, 96, 32},
      {96, 0, 112, 16},
      {96, 16, 112, 32},
      {112, 16, 128, 32}
   };
   public static final Map<Integer, NativeImageBackedTexture> TEXTURES = new HashMap<>();
   public static int[] alphaMask;
   public static boolean failed;

   public static Identifier identifier() {
      return ID;
   }

   public static void ensure() {
      ensure(1.0F);
   }

   public static boolean failed() {
      return failed;
   }

   public static Identifier ensure(float width) {
      return ensure(width, CrystalShape.CLASSIC);
   }

   public static Identifier ensure(float width, CrystalShape shape) {
      int profile = switch (shape) {
         case CONTOUR -> 1;
         case CONSTELLATION -> 2;
         case FACETS -> 3;
         case CORE -> 4;
         default -> 0;
      };
      int widthKey = Math.round(MathHelper.clamp(width, 0.5F, 2.0F) * 20.0F);
      int key = widthKey + profile * 64;
      Identifier id = key == 20 ? ID : Identifier.of("javelin", "dynamic/crystal_astral_" + key);
      if (TEXTURES.containsKey(key)) {
         return id;
      }

      if (failed) {
         return null;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getTextureManager() == null) {
         return null;
      }

      try {
         if (profile != 4 && alphaMask == null) {
            Optional<Resource> resource = mc.getResourceManager().getResource(VANILLA);
            if (resource.isEmpty()) {
               failed = true;
               return null;
            }

            alphaMask = new int[8192];

            try (InputStream input = resource.get().getInputStream()) {
               NativeImage vanilla = NativeImage.read(input);

               try {
                  for (int y = 0; y < 64; y++) {
                     for (int x = 0; x < 128; x++) {
                        if (x < vanilla.getWidth() && y < vanilla.getHeight()) {
                           alphaMask[y * 128 + x] = vanilla.getColorArgb(x, y) >>> 24 & 0xFF;
                        }
                     }
                  }
               } catch (Throwable var14) {
                  if (vanilla != null) {
                     try {
                        vanilla.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (vanilla != null) {
                  vanilla.close();
               }
            }
         }

         NativeImageBackedTexture texture = new NativeImageBackedTexture(id::toString, 128, 64, false);
         NativeImage image = texture.getImage();
         if (image == null) {
            failed = true;
            texture.close();
            return null;
         }

         for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 128; x++) {
               if (profile != 4 && alphaMask[y * 128 + x] == 0) {
                  image.setColorArgb(x, y, 0);
               } else {
                  int alpha = wireAlpha(x, y, profile, widthKey);
                  image.setColorArgb(x, y, alpha == 0 ? 0 : alpha << 24 | 16777215);
               }
            }
         }

         mc.getTextureManager().registerTexture(id, texture);
         texture.upload();
         TEXTURES.put(key, texture);
         return id;
      } catch (Exception ignored) {
         failed = true;
         return null;
      }
   }

   public static int wireAlpha(int x, int y, int profile, int widthKey) {
      float wire = distanceToWire(x, y, profile);
      if (wire == Float.MAX_VALUE) {
         return 0;
      } else if (profile == 4 && widthKey == 40) {
         return 255;
      } else {
         float distance = wire / (widthKey / 20.0F);
         if (distance <= 0.7F) {
            return 255;
         } else if (distance <= 1.65F) {
            return 180;
         } else {
            return distance <= 2.65F ? 72 : 0;
         }
      }
   }

   public static float distanceToWire(int x, int y, int profile) {
      float nearest = Float.MAX_VALUE;

      for (int[] face : FACE_UVS) {
         if (x >= face[0] && x < face[2] && y >= face[1] && y < face[3]) {
            float localX = x - face[0];
            float localY = y - face[1];
            float maxX = face[2] - face[0] - 1.0F;
            float maxY = face[3] - face[1] - 1.0F;
            float border = Math.min(Math.min(localX, maxX - localX), Math.min(localY, maxY - localY));
            float diagonalA = Math.abs(localX / maxX - localY / maxY) * 11.0F;
            float diagonalB = Math.abs(localX / maxX - (1.0F - localY / maxY)) * 11.0F;

            float wire = switch (profile) {
               case 1 -> border;
               case 2 -> {
                  float cornerX = Math.min(localX, maxX - localX);
                  float cornerY = Math.min(localY, maxY - localY);
                  float corner = (float)Math.sqrt(cornerX * cornerX + cornerY * cornerY);
                  float cx = localX - maxX * 0.5F;
                  float cy = localY - maxY * 0.5F;
                  yield Math.min(corner, (float)Math.sqrt(cx * cx + cy * cy));
               }
               case 3 -> Math.min(border, diagonalA);
               case 4 -> {
                  float diamond = Math.abs(Math.abs(localX - maxX * 0.5F) + Math.abs(localY - maxY * 0.5F) - maxX * 0.35F);
                  yield Math.min(border, diamond);
               }
               default -> Math.min(border, Math.min(diagonalA, diagonalB));
            };
            nearest = Math.min(nearest, wire);
         }
      }

      return nearest;
   }
}
