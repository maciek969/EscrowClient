package elexis.client.utils.player;

import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.client.other.Script;
import elexis.client.utils.math.MathUtil;
import elexis.mixins.accessors.ClientPlayerEntityAccessor;
import elexis.modules.impl.utils.ServerAssistant;
import elexis.ui.gui.ClickGui;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class PlayerUtils implements IMinecraft {
    public final List<KeyBinding> moveKeys = List.of(
            mc.options.forwardKey,
            mc.options.backKey, mc.options.leftKey, mc.options.rightKey,
            mc.options.jumpKey, mc.options.sprintKey);
    public static final Script script = new Script(), postScript = new Script();
    public boolean canMove = true;

    final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public void tick() {
        script.update();
    }

    public void interactItem(Hand hand) {
        sendSequencedPacket(i -> new PlayerInteractItemC2SPacket(hand, i, ((ClientPlayerEntityAccessor) mc.player).getLastYaw(), ((ClientPlayerEntityAccessor) mc.player).getLastPitch()));
    }

    public void postMotion() {
        postScript.update();
    }

    public static boolean isMoving() {
        return mc.player != null && mc.world != null && mc.player.input != null
                && (mc.player.input.getMovementInput().y != 0.0 || mc.player.input.getMovementInput().x != 0.0 || mc.player.fallDistance != 0.0);
    }

    public static boolean isMovingForward() {
        return mc.player != null && mc.world != null && mc.player.input != null
                && mc.player.input.getMovementInput().y > 0.0;
    }

    public static boolean isMovingSideways() {
        return mc.player != null && mc.world != null && mc.player.input != null
                && mc.player.input.getMovementInput().x != 0.0;
    }

    public void addTask(Runnable task) {
        if (script.isFinished() && hasPlayerMovement()) {
            switch (ServerUtil.getServer()) {
                case "FunTime" -> {
                    script.cleanup().addTickStep(0, () -> {
                        PlayerUtils.disableMoveKeys();
                    }).addTickStep(1, () -> {
                        task.run();
                        enableMoveKeys();
                    });
                    return;
                }
                case "ReallyWorld" -> {
                    if (mc.player.isOnGround()) {
                        script.cleanup().addTickStep(0, PlayerUtils::disableMoveKeys)
                                .addTickStep(3, task::run)
                                .addTickStep(4, PlayerUtils::enableMoveKeys);
                        return;
                    }
                }
                case "SpookyTime" -> {
                    script.cleanup().addTickStep(0, ()-> {
                                PlayerUtils.disableMoveKeys();
                            }).addTickStep(2, task::run)
                            .addTickStep(2, PlayerUtils::enableMoveKeys);
                    return;
                }
                case "Р В РЎв„ўР В Р’В°Р РЋР С“Р РЋРІР‚С™Р В РЎвЂўР В РЎВ" -> {
                    script.cleanup().addTickStep(0, ()-> {
                                PlayerUtils.disableMoveKeys();
                            }).addTickStep(ServerAssistant.tickToRunAction.get().intValue(), task::run)
                            .addTickStep(ServerAssistant.tickToReturnKeys.get().intValue(), PlayerUtils::enableMoveKeys);
                    return;
                }

                case "Р В РІР‚ВР В Р’ВµР В Р’В· Р РЋР вЂљР В Р’В°Р В Р’В·Р В Р вЂ¦Р В РЎвЂР РЋРІР‚В Р РЋРІР‚в„–" -> {
                    script.cleanup().addTickStep(0, task::run);
                    return;
                }
            }

        }
        postScript.cleanup().addTickStep(0, () -> {
            task.run();
            PlayerInventoryUtil.closeScreen(true);
        });
    }

    public void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();
    }

    public void enableMoveKeys() {
        PlayerInventoryUtil.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
    }

    public void updateMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow(), keyBinding.getDefaultKey().getCode())));
    }

    public boolean shouldSkipExecution() {
        return mc.currentScreen != null && !isChatOpen() && !(mc.currentScreen instanceof SignEditScreen) && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof AbstractCommandBlockScreen) && !(mc.currentScreen instanceof StructureBlockScreen) && !(mc.currentScreen instanceof ClickGui);
    }

    public boolean shouldSkipExecutionGuiMove() {
        return mc.currentScreen != null && !isChatOpen() && !(mc.currentScreen instanceof SignEditScreen) && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof AbstractCommandBlockScreen) && !(mc.currentScreen instanceof StructureBlockScreen) && !(mc.currentScreen instanceof ClickGui);
    }

    public static boolean isChatOpen() {
        return mc.currentScreen instanceof ChatScreen;
    }

    public static Entity getMouseOver(Entity target, float yaw, float pitch, double distance) {
        Entity entity = mc.getCameraEntity();
        if (entity == null || mc.world == null || target == null) {
            return null;
        }

        Box playerBox = entity.getBoundingBox();
        Box targetBox = target.getBoundingBox();
        Vec3d startVec = entity.getEyePos();
        Vec3d directionVec = getVectorForRotation(pitch, yaw);
        Vec3d endVec = startVec.add(
                directionVec.x * distance,
                directionVec.y * distance,
                directionVec.z * distance
        );

        if (playerBox.intersects(targetBox)) {
            EntityHitResult hitResult = raytraceEntity(distance, yaw, pitch, (e) -> e == target && !e.isSpectator() && e.canBeHitByProjectile());
            if (hitResult != null && hitResult.getEntity() == target) {
                return target;
            }
        }

        EntityHitResult entityHitResult = rayTraceEntities(
                entity,
                startVec,
                endVec,
                targetBox,
                (e) -> e == target && !e.isSpectator() && e.canBeHitByProjectile(),
                distance
        );

        if (entityHitResult != null && startVec.distanceTo(entityHitResult.getPos()) <= distance) {
            return entityHitResult.getEntity();
        }

        return null;
    }

    public static Entity getMouseOver(Entity target, float yaw, float pitch, double distance, float hitboxScale) {
        Entity entity = mc.getCameraEntity();
        if (entity == null || mc.world == null || target == null) {
            return null;
        }

        Vec3d startVec = entity.getEyePos();
        Vec3d directionVec = Vec3d.fromPolar(pitch, yaw);
        Vec3d endVec = startVec.add(directionVec.multiply(distance));

        Box originalBox = target.getBoundingBox();

        double xShrink = (originalBox.getLengthX() * (1.0F - hitboxScale)) / 2.0D;
        double zShrink = (originalBox.getLengthZ() * (1.0F - hitboxScale)) / 2.0D;

        Box scaledBox = originalBox.expand(-xShrink, 0, -zShrink);

        var hitResult = scaledBox.raycast(startVec, endVec);

        if (hitResult.isPresent()) {
            return target;
        }

        return null;
    }

    public static Entity getRayTraceResult(Vec3d targetPos, Entity entity, double maxDistance, RotateVector rotateVector) {
        Entity cameraEntity = mc.getCameraEntity();
        if (mc.world == null || cameraEntity == null || entity == null
                || entity.isSpectator() || !entity.canBeHitByProjectile()) {
            return null;
        }

        Vec3d eyePos = cameraEntity.getEyePos();
        float yaw = rotateVector.getYaw();
        float pitch = rotateVector.getPitch();

        Vec3d direction = getVectorForRotation(pitch, yaw);
        Vec3d endPos = eyePos.add(
                direction.x * maxDistance,
                direction.y * maxDistance,
                direction.z * maxDistance
        );

        Box originalBox = entity.getBoundingBox();

        Vec3d originalEyePos = entity.getEyePos();
        Vec3d offset = targetPos.subtract(originalEyePos);

        Box predictedBox = originalBox.offset(offset);
        Box playerBox = entity.getBoundingBox();

        if (playerBox.intersects(predictedBox)) {
            EntityHitResult hitResult = raytraceEntity(maxDistance, yaw, pitch, (e) -> e == entity && !e.isSpectator() && e.canBeHitByProjectile());
            if (hitResult != null && hitResult.getEntity() == entity) {
                return entity;
            }
        }

        EntityHitResult hitResult = rayTraceEntities(
                cameraEntity,
                eyePos,
                endPos,
                predictedBox,
                e -> e == entity,
                maxDistance
        );

        if (hitResult != null && hitResult.getEntity() == entity) {
            return entity;
        }

        return null;
    }

    static float getEntityHealth(Entity target) {
        if (target == null || mc.world == null || !(target instanceof LivingEntity e)) {
            return mc.player.getHealth();
        }

        if (!(target instanceof PlayerEntity)) {
            return e.getHealth();
        }

        if (target == mc.player) return mc.player.getHealth();
        if (target.isInvisible()) return 1000.0f;

        Scoreboard scoreboard = mc.world.getScoreboard();

        // todo
      //  if (scoreboard != null && (FixHP.mode.is("FunTime") || Elexis.getFunctionManager().getFixHP().isState())) {
      //      ScoreHolder scoreHolder = ScoreHolder.fromName(target.getName().getString());
      //      for (ScoreboardObjective objective : scoreboard.getObjectives()) {
      //          ReadableScoreboardScore score = scoreboard.getScore(scoreHolder, objective);
      //          if (score != null) {
      //              return score.getScore();
      //          }
      //      }
      //  } else {
        //    return e.getHealth();
      //  }

        return 1000;
    }

    public static float getHealthFloat(LivingEntity target) {
        float health = getEntityHealth(target);
        if (!(target instanceof PlayerEntity)) {
            return health;
        }

        // todo
      //  if (!Elexis.getFunctionManager().getFixHP().isState()) return target.getHealth();

        return health == 1000 ? 20 : health;
    }

    public static String getHealthString(LivingEntity target) {
        float health = getEntityHealth(target);
        if (!(target instanceof PlayerEntity)) {
            return Math.round(health) + "";
        }

        return health == 1000 ? "? " : Math.round(health) + "";
    }

    private static Vec3d getVectorForRotation(float pitch, float yaw) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float cosPitch = (float) Math.cos(-pitchRad);
        return new Vec3d(
                -Math.sin(yawRad) * cosPitch,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosPitch
        );
    }

    private static EntityHitResult rayTraceEntities(Entity source, Vec3d start, Vec3d end, Box boundingBox, Predicate<Entity> predicate, double maxDistance) {
        World world = source.getEntityWorld();
        double closestDistance = maxDistance;
        Entity closestEntity = null;
        Vec3d closestHitPos = null;

        for (Entity entity : world.getEntitiesByClass(Entity.class, boundingBox, predicate)) {
            if (entity == source) continue;

            Box entityBox = entity.getBoundingBox();
            var hit = entityBox.raycast(start, end);

            if (hit.isPresent()) {
                Vec3d hitPos = hit.get();
                double distance = start.distanceTo(hitPos);

                if (distance < closestDistance) {
                    closestEntity = entity;
                    closestHitPos = hitPos;
                    closestDistance = distance;
                }
            }
        }

        if (closestEntity != null) {
            return new EntityHitResult(closestEntity, closestHitPos);
        }
        return null;
    }

    public static BlockHitResult raycast(double range, float yaw, float pitch, boolean includeFluids) {
        Entity entity = mc.getCameraEntity();
        if (entity == null || mc.world == null) {
            return null;
        }

        Vec3d start = entity.getCameraPosVec(1.0F);
        float pitchRad = pitch * 0.017453292F;
        float yawRad = -yaw * 0.017453292F;
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        Vec3d rotationVec = new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        Vec3d end = start.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);

        World world = mc.world;
        RaycastContext.FluidHandling fluidHandling = includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE;
        RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, fluidHandling, entity);

        return world.raycast(context);
    }

    public static EntityHitResult raytraceEntity(double range, float yaw, float pitch, Predicate<Entity> filter) {
        Entity entity = mc.getCameraEntity();
        if (entity == null || mc.world == null) {
            return null;
        }

        Vec3d cameraVec = entity.getCameraPosVec(1.0F);
        float pitchRad = pitch * 0.017453292F;
        float yawRad = -yaw * 0.017453292F;
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        Vec3d rotationVec = new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        Vec3d end = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
        Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0);

        return ProjectileUtil.raycast(
                entity,
                cameraVec,
                end,
                box,
                filter,
                range * range
        );
    }

    public static double[] forward(final double d) {
        float f = mc.player.input.getMovementInput().y;
        float f2 = mc.player.input.getMovementInput().x;
        float f3 = getGlobalYaw();
        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += ((f > 0.0f) ? -45 : 45);
            } else if (f2 < 0.0f) {
                f3 += ((f > 0.0f) ? 45 : -45);
            }
            f2 = 0.0f;
            if (f > 0.0f) {
                f = 1.0f;
            } else if (f < 0.0f) {
                f = -1.0f;
            }
        }
        final double d2 = Math.sin(Math.toRadians(f3 + 90.0f));
        final double d3 = Math.cos(Math.toRadians(f3 + 90.0f));
        final double d4 = f * d * d3 + f2 * d * d2;
        final double d5 = f * d * d2 - f2 * d * d3;
        return new double[]{d4, d5};
    }

    public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        mc.interactionManager.sendSequencedPacket(mc.world, packetCreator);
    }

    public void sendPacketWithOutEvent(Packet<?> packet) {
        mc.getNetworkHandler().getConnection().send(packet, null);
    }

    public static void jump(float yaw) {
        if (mc.player.isSprinting()) {
            float g = yaw * ((float)Math.PI / 180F);
            mc.player.addVelocityInternal(new Vec3d(-MathHelper.sin(g) * 0.2F, 0.0F, MathHelper.cos(g) * 0.2F));
        }
        mc.player.velocityDirty = true;
    }

    public Stream<Entity> streamEntities() {
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false);
    }

    public boolean isBox(Box box, Predicate<BlockPos> pos) {
        return BlockPos.stream(box).anyMatch(pos);
    }

    public boolean hasPlayerMovement() {
        return mc.player.input.getMovementInput().y != 0f || mc.player.input.getMovementInput().x != 0f || mc.options.jumpKey.isPressed();
    }

    public double[] calculateDirection(double distance) {
        return calculateDirection(mc.player.input.getMovementInput().y, mc.player.input.getMovementInput().x, distance);
    }

    public double[] calculateDirection(float forward, float sideways, double distance) {
        float yaw = mc.player.getYaw();
        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw += (forward > 0.0f) ? -45 : 45;
            } else if (sideways < 0.0f) {
                yaw += (forward > 0.0f) ? 45 : -45;
            }
            sideways = 0.0f;
            forward = (forward > 0.0f) ? 1.0f : -1.0f;
        }

        double sinYaw = Math.sin(Math.toRadians(yaw + 90.0f));
        double cosYaw = Math.cos(Math.toRadians(yaw + 90.0f));
        double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
        double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;

        return new double[]{xMovement, zMovement};
    }

    public void setVelocity(double velocity) {
        final double[] direction = calculateDirection(velocity);
        Objects.requireNonNull(mc.player).setVelocity(direction[0], mc.player.getVelocity().getY(), direction[1]);
    }

    public void setVelocity(double velocity, double y) {
        final double[] direction = calculateDirection(velocity);
        Objects.requireNonNull(mc.player).setVelocity(direction[0], y, direction[1]);
    }

    public PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
        boolean forwards = input.forward();
        boolean backwards = input.backward();
        boolean left = input.left();
        boolean right = input.right();

        if (dgs >= (-90.0F + deadAngle) && dgs <= (90.0F - deadAngle)) {
            forwards = true;
        } else if (dgs < (-90.0F - deadAngle) || dgs > (90.0F + deadAngle)) {
            backwards = true;
        }

        if (dgs >= (0.0F + deadAngle) && dgs <= (180.0F - deadAngle)) {
            right = true;
        } else if (dgs >= (-180.0F + deadAngle) && dgs <= (0.0F - deadAngle)) {
            left = true;
        }

        return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
    }

    public static Vec3d toVector(float yaw, float pitch) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public float getGlobalYaw() {
        return ((ClientPlayerEntityAccessor) mc.player).getLastYaw();
    }

    public float getGlobalPitch() {
        return ((ClientPlayerEntityAccessor) mc.player).getLastPitch();
    }

    static final Pattern NAME_REGEX = Pattern.compile("^[A-Za-z\\p{IsCyrillic}0-9_]{3,16}$");

    public static boolean isNameValid(String name) {
        return NAME_REGEX.matcher(name).matches();
    }

    public static boolean isBlockUnder(float under) {
        if (mc.player.getY() < 0.0) {
            return false;
        } else {
            Iterator<VoxelShape> collisions = mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, -under, 0.0)).iterator();
            return !collisions.hasNext();
        }
    }

    public void updateKeyBindingsState(KeyBinding[] keyBindings) {
        if (keyBindings == null) return;
        for (KeyBinding keyBinding : keyBindings) {
            boolean isKeyPressed = InputUtil.isKeyPressed(mc.getWindow(), keyBinding.boundKey.getCode());
            keyBinding.setPressed(isKeyPressed);
        }
    }

    public void updateKeyBindingState(KeyBinding keyBinding) {
        if (keyBinding == null) return;

        boolean isKeyPressed = InputUtil.isKeyPressed(mc.getWindow(), keyBinding.boundKey.getCode());
        keyBinding.setPressed(isKeyPressed);
    }

    public static Text normalizeName(Text input) {
        if (input == null) return null;
        return Text.of(normalizeName(input.getString()));
    }

    public static String normalizeName(String input) {
        if (input == null || input.isEmpty()) return input;

        String cleanedInput = input
                .replace("В§", "")
                .replaceAll("(?i)&[0-9A-FK-OR]", "")
                .replaceAll("\\s+", " ")
                .trim();

        StringBuilder sb = new StringBuilder(cleanedInput.length());
        for (char c : cleanedInput.toCharArray()) {
            char normalized = normalizeChar(c);
            if (normalized != 0) {
                sb.append(normalized);
            }
        }
        return sb.toString().trim();
    }

    public static char normalizeChar(char c) {
        if (Character.isLetterOrDigit(c) || c == '_' || c == ' ') {
            return c;
        }

        return switch (c) {
            case '.', ',', ':', '-', '+', '/', '\\', '&', '|', '*', '[', ']', '(', ')', '#' -> c;
            default -> 0;
        };
    }

    public String getBindName(int key) {
        if (key == 0) {
            return "NONE";
        }
        if (key < 0) {
            int btn = -(key + 1);
            return switch (btn) {
                case 0 -> "MOUSE1";
                case 1 -> "MOUSE2";
                case 2 -> "MOUSE3";
                case 3 -> "MOUSE4";
                case 4 -> "MOUSE5";
                default -> "MOUSE" + (btn + 1);
            };
        }
        if (key == GLFW.GLFW_KEY_GRAVE_ACCENT) {
            return "`";
        }
        return getKeyType(key).createFromCode(key).getTranslationKey().replace("key.keyboard.", "")
                .replace("key.mouse.", "mouse ").replace(".", " ").toUpperCase();
    }

    public InputUtil.Type getKeyType(int key) {
        return key < 0 ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
    }

    public double getSpeedSqrt(Entity entity) {
        return Math.sqrt(entity.squaredDistanceTo(new Vec3d(entity.lastX, entity.lastY, entity.lastZ)));
    }

    public double getBPS() {
        if (mc.player == null) return 0;
        return MathUtil.round(getSpeedSqrt(mc.player) * 20.0F, 0.1F);
    }

    public float getCooldownProgress(Item item) {
        ItemCooldownManager cooldownManager = mc.player.getItemCooldownManager();
        ItemCooldownManager.Entry entry = cooldownManager.entries.get(item);
        if (entry == null) return 0;
        return Math.max(0, (entry.endTick - cooldownManager.tick) / 20F);
    }
}


