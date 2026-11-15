package elexis.modules.impl.combat;

import elexis.Elexis;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.mixins.accessors.ClientPlayerInteractionManagerAccessor;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.RotationProcess;
import elexis.modules.impl.combat.javelin.ExplosionDamage;
import elexis.modules.impl.render.visual.CrystalAuraRenderer;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

/** Adapted from the Astra CrystalAura source supplied by the client owner. */
@FunctionAdd(name = "CrystalAura", alias = "CrystalAura", category = Category.Combat,
description = "Кристаллы и обсидиан: расчёт урона, подкоп и режимы ротации")
public final class CrystalAura extends Function {

    private static final double PLACE_REACH = 4.5D;
    private static final double HIT_REACH = 3.0D;
    private static final double TARGET_RANGE = 8.0D;
    private static final double MAX_SELF_DAMAGE = 30.0D;
    private static final long VISUAL_MS = 300L;
    private static final int SLOT_RETURN_TICKS = 3;
    private static final int MAX_TARGET_HOLD_TICKS = 8;

    public final ModeSetting preset = new ModeSetting("Режим", "Default", "Default", "Donut");
    public final ModeSetting rotate = new ModeSetting("Ротация", "ReallyWorld", "ReallyWorld", "Плавная", "Vanilla", "Без ротации");
    public final SliderSetting smoothSpeed = new SliderSetting("Скорость ротации", .5f, .1f, 1f, .05f)
    .setVisible(() -> rotate.is("Плавная"));
    public final BooleanSetting autoKrot = new BooleanSetting("Делать подкоп", true);
    public final SliderSetting explosionDelay = new SliderSetting("Задержка", 0, 0, 500, 10);
    public final BooleanSetting render = new BooleanSetting("Рендер", true);

    private ClientWorld activeWorld;
    private ClientPlayerEntity activePlayer;
    private Vec2f serverRotation;
    private BlockPos miningPos;
    private boolean placing;
    private PlayerEntity target;

    private int ticksToBack;
    private int lastSlot = -1;
    private long lastExplosionAt;
    private PlaceData lastData;
    private float[] rotation;
    private boolean miningThisTick;
    /** Set on the ticks a crystal step actually went out. */
    private boolean acted;
    private final ConcurrentLinkedQueue<VisualData> visuals = new ConcurrentLinkedQueue<>();
    private final Map<DamageKey, Float> damageCache = new HashMap<>();

    private final Map<BlockPos, Long> placedPositions = new ConcurrentHashMap<>();
    private final Map<Integer, Long> attackedCrystals = new ConcurrentHashMap<>();
    private int targetHoldTicks;

    private float smoothedYaw;
    private float smoothedPitch;
    private float noiseYaw;
    private float noisePitch;
    private boolean rotationInitialized;

    public CrystalAura() {
        addSettings(preset, rotate, smoothSpeed, autoKrot, explosionDelay, render);
        this.autoKrot.setVisible(() -> this.preset.is("Default"));
    }

    public boolean isMiningThisTick() {
        return isState() && this.miningThisTick;
    }

    public boolean holdsSlot() {
        return isState() && this.lastSlot != -1;
    }

    public boolean usesAnchors() {
        return this.preset.is("Donut");
    }

    public boolean legit() {
        return false;
    }

    public boolean rage() {
        return false;
    }

    private boolean diggingAllowed() {
        return this.preset.is("Default") && this.autoKrot.get();
    }

    public PlaceData lastData() {
        return this.lastData;
    }

    public PlaceData getLastData() {
        return this.lastData;
    }

    public float[] rotation() {
        return this.rotation;
    }

    public float[] getRotation() {
        return this.rotation;
    }

    public BooleanSetting getAutoKrot() {
        return this.autoKrot;
    }

    public boolean actedThisTick() {
        return isState() && this.acted;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
    }

    private void reset() {
        stopMining();
        this.serverRotation = null;
        this.target = null;
        this.damageCache.clear();
        CrystalAuraRenderer.release();
        this.ticksToBack = 0;
        this.rotation = null;
        this.lastData = null;
        this.rotationInitialized = false;
        this.targetHoldTicks = 0;
        this.noiseYaw = 0.0F;
        this.noisePitch = 0.0F;
        this.miningThisTick = false;
        this.acted = false;
        this.lastExplosionAt = 0L;
        this.visuals.clear();
        this.placedPositions.clear();
        this.attackedCrystals.clear();


        if (this.lastSlot != -1 && mc.player != null && activePlayer == mc.player) {
            mc.player.getInventory().setSelectedSlot(this.lastSlot);
            syncSelectedSlot();
        }
        this.lastSlot = -1;
        this.activeWorld = null;
        this.activePlayer = null;
    }

    @EventHandler
    public void onPlayerTick(EventUpdate.Slow event) {
        if (activeWorld != mc.world || activePlayer != mc.player) {
            reset();
            activeWorld = mc.world;
            activePlayer = mc.player;
        }
        onPreTick();
    }

    public Vec2f getMovementRotation() {
        return isState() && acted && activeWorld == mc.world && activePlayer == mc.player ? serverRotation : null;
    }

    /** Called after vanilla has sent this tick's movement and look. */
    public void afterMovement() {
        if (isState() && activeWorld == mc.world && activePlayer == mc.player) onPostTick();
    }

    public boolean isPlacing() { return placing; }
    public PlayerEntity getTarget() { return target; }
    public PlaceData getPlaceData() { return lastData; }

    /** Preserve our native mining progress when the physical attack key is released. */
    public boolean handleMiningInput() {
        return isState() && activeWorld == mc.world && miningPos != null;
    }

    private void stopMining() {
        if (miningPos != null && mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
        miningPos = null;
    }

    private boolean canAct() {
        var manager = Elexis.getFunctionManager();
        return mc.currentScreen == null && !mc.player.isUsingItem() && !mc.player.hasVehicle()
        && !mc.options.attackKey.isPressed() && !mc.options.useKey.isPressed()
        && (!mc.interactionManager.isBreakingBlock() || miningPos != null)
        && !RotationProcess.isRotating()
        && (manager.getHitAura() == null || !manager.getHitAura().isState() || HitAura.target == null)
        && (manager.getAimBot() == null || manager.getAimBot().getTarget() == null)
        && TargetPearl.getMovementRotation() == null
        && (manager.getAutoExplosion() == null || !manager.getAutoExplosion().isPlacingCrystal());
    }

    private void onPreTick() {
        ClientPlayerEntity self = mc.player;
        ClientWorld world = mc.world;
        this.miningThisTick = false;
        this.acted = false;
        this.damageCache.clear();
        if (self == null || world == null || mc.interactionManager == null) {
            finishAction();
            return;
        }
        if (!canAct()) {
            finishAction();
            return;
        }

        long now = System.currentTimeMillis();
        this.attackedCrystals.entrySet().removeIf(e -> now - e.getValue() > 1500L);
        this.placedPositions.entrySet().removeIf(e -> now - e.getValue() > 400L);

        if (this.ticksToBack > 0) {
            this.ticksToBack--;
        }
        if (this.ticksToBack == 0 && this.lastSlot != -1) {
            self.getInventory().setSelectedSlot(this.lastSlot);
            syncSelectedSlot();
            this.lastSlot = -1;
        }

        this.target = findTarget(self, world);
        PlayerEntity target = this.target;

        if (!hasCrystals(self) || target == null) {
            finishAction();
            return;
        }

        List<Entity> entities = world.getEntitiesByClass(Entity.class, self.getBoundingBox().expand(16.0D),
        entity -> entity.isAlive() && !entity.isSpectator());

        for (Entity entity : entities) {
            if (entity instanceof EndCrystalEntity c && c.isAlive() && !c.isRemoved()) {
                this.placedPositions.remove(c.getBlockPos().down());
                this.placedPositions.remove(c.getBlockPos());
            }
        }

        PlaceData explosionData = findBestExplosion(target, world, self, entities);
        PlaceData obsidianData = !hasObsidian(self)
        ? emptyData(PlaceResult.PLACE_OBSIDIAN)
        : findBestObsidian(target, world, self, entities);
        PlaceData crystalData = findBestCrystal(target, world, self, entities);

        PlaceData finalData = explosionData;
        if (crystalData.damage() > finalData.damage()) {
            finalData = crystalData;
        }
        if (obsidianData.damage() > finalData.damage()) {
            finalData = obsidianData;
        }
        if (diggingAllowed()) {
            PlaceData breakData = findBestBreak(target, world, self, entities);
            if (breakData.damage() > finalData.damage()) {
                finalData = breakData;
            }
        }

        if (obsidianData.placeResult() == PlaceResult.AWAITING) {
            finalData = obsidianData;
        }

        boolean keepTarget = false;
        if (this.lastData != null && this.targetHoldTicks < MAX_TARGET_HOLD_TICKS) {
            if (this.lastData.placeResult() == PlaceResult.EXPLOSION) {
                Entity c = this.lastData.entity();
                if (c instanceof EndCrystalEntity crystal && crystal.isAlive() && !crystal.isRemoved()
                && !this.attackedCrystals.containsKey(crystal.getId())
                && self.getEyePos().squaredDistanceTo(nearestPoint(crystal, 0.0D, 0.05D)) <= HIT_REACH * HIT_REACH) {
                    keepTarget = true;
                }
            } else if (this.lastData.placeResult() == PlaceResult.PLACE_CRYSTAL) {
                BlockPos bp = this.lastData.blockPos();
                if (bp != null && (world.getBlockState(bp).isOf(Blocks.OBSIDIAN) || world.getBlockState(bp).isOf(Blocks.BEDROCK))
                && world.getBlockState(bp.up()).isAir()
                && !blockedForCrystal(entities, bp.up())) {
                    keepTarget = true;
                }
            }
        }

        if (keepTarget) {
            this.targetHoldTicks++;
        } else {
            this.targetHoldTicks = 0;
            this.lastData = finalData;
        }

        if (this.lastData == null || this.lastData.placeResult() == PlaceResult.AWAITING || this.lastData.damage() == 0.0D) {
            finishAction();
            return;
        }

        if (miningPos != null && (lastData.placeResult() != PlaceResult.BREAK || !miningPos.equals(lastData.blockPos()))) stopMining();
        this.acted = true;

        Vec3d targetPoint = null;
        if (this.lastData.placeResult() == PlaceResult.EXPLOSION) {
            Entity crystal = this.lastData.entity();
            if (crystal != null && crystal.isAlive() && !crystal.isRemoved()) {
                BlockPos pos = this.lastData.blockPos();
                Direction face = this.lastData.face();
                targetPoint = (pos != null && face != null) ? getFacePoint(pos, face) : nearestPoint(crystal, 0.0D, 0.05D);
            }
        } else {
            BlockPos pos = this.lastData.blockPos();
            Direction face = this.lastData.face();
            if (pos != null && face != null) {
                targetPoint = getFacePoint(pos, face);
            }
        }

        if (targetPoint != null) {
            float[] rot = calculate(targetPoint, self);
            applyRotation(rot);
        }
    }

    private void onPostTick() {
        ClientPlayerEntity self = mc.player;
        ClientWorld world = mc.world;
        if (self == null || world == null || mc.interactionManager == null || this.lastData == null || !this.acted) {
            return;
        }

        if (this.lastData.placeResult() == PlaceResult.EXPLOSION) {
            explodeCrystal(self);
        } else {
            interactWithBlock(self, world);
        }
    }

    private void explodeCrystal(ClientPlayerEntity self) {
        Entity crystal = this.lastData.entity();
        if (!(crystal instanceof EndCrystalEntity) || !crystal.isAlive() || crystal.isRemoved() || this.attackedCrystals.containsKey(crystal.getId())) {
            this.lastData = null;
            this.targetHoldTicks = 0;
            return;
        }

        float[] currentRot = getCurrentRotation(self);
        boolean lookingAt = isInstantRotation() || rayTrace(currentRot, HIT_REACH, crystal.getBoundingBox(), self);

        if (lookingAt) {
            attackCrystal(self, crystal);
            this.lastData = null;
            this.targetHoldTicks = 0;
        }
    }

    private void attackCrystal(ClientPlayerEntity self, Entity crystal) {
        long now = System.currentTimeMillis();
        if (now - this.lastExplosionAt < (long) this.explosionDelay.get().doubleValue()) {
            return;
        }
        if (mc.interactionManager != null) {
            this.attackedCrystals.put(crystal.getId(), now);
            BlockPos base = crystal.getBlockPos().down();
            this.placedPositions.remove(base);
            this.placedPositions.remove(crystal.getBlockPos());

            mc.interactionManager.attackEntity(self, crystal);
            self.swingHand(Hand.MAIN_HAND);
            this.lastExplosionAt = now;

            // Wait for the server to remove the crystal; the attack map prevents duplicates.
        }
    }

    private void interactWithBlock(ClientPlayerEntity self, ClientWorld world) {
        BlockPos pos = this.lastData.blockPos();
        Direction face = this.lastData.face();
        if (pos == null || face == null) {
            finishAction();
            return;
        }

        float[] currentRot = getCurrentRotation(self);
        BlockHitResult hit = raycastBlock(currentRot, pos, PLACE_REACH, self);

        boolean lookingAt;
        if (isInstantRotation()) {
            lookingAt = true;
            if (hit == null) {
                hit = new BlockHitResult(getFacePoint(pos, face), face, pos, false);
            }
        } else {
            lookingAt = (hit != null);
        }

        if (lookingAt && hit != null) {
            if (this.lastData.placeResult() == PlaceResult.BREAK) {
                breakBlock(self, world, pos, face);
            } else {
                placeBlock(self, pos, hit);
            }
            this.lastData = null;
            this.targetHoldTicks = 0;
        }
    }

    private void breakBlock(ClientPlayerEntity self, ClientWorld world, BlockPos pos, Direction face) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            if (mc.interactionManager != null) {
                mc.interactionManager.cancelBlockBreaking();
            }
            finishAction();
            return;
        }
        int hotbarTool = findBestHotbarTool(self, state);
        if (hotbarTool == -1) {
            finishAction();
            return;
        }
        if (this.lastSlot == -1) {
            this.lastSlot = self.getInventory().getSelectedSlot();
        }
        this.ticksToBack = SLOT_RETURN_TICKS;
        if (self.getInventory().getSelectedSlot() != hotbarTool) {
            self.getInventory().setSelectedSlot(hotbarTool);
            syncSelectedSlot();
        }

        this.miningThisTick = true;
        this.miningPos = pos.toImmutable();
        if (mc.interactionManager != null) {
            if (!mc.interactionManager.isBreakingBlock()) mc.interactionManager.attackBlock(pos, face);
            self.swingHand(Hand.MAIN_HAND);
            if (!world.getBlockState(pos).isAir() && mc.interactionManager.updateBlockBreakingProgress(pos, face)) {
                self.swingHand(Hand.MAIN_HAND);
            }
        }
        addVisual(pos, 0.8F, 0.1F, 0.1F);
    }

    private void placeBlock(ClientPlayerEntity self, BlockPos pos, BlockHitResult hit) {
        boolean isCrystal = this.lastData != null && this.lastData.placeResult() == PlaceResult.PLACE_CRYSTAL;
        Item item = isCrystal ? Items.END_CRYSTAL : Items.OBSIDIAN;
        Hand hand = Hand.MAIN_HAND;

        if (self.getOffHandStack().isOf(item)) {
            hand = Hand.OFF_HAND;
        } else if (self.getMainHandStack().isOf(item)) {
            hand = Hand.MAIN_HAND;
        } else {
            int slot = findHotbar(self, item);
            if (slot == -1) {
                return;
            }
            if (this.lastSlot == -1) {
                this.lastSlot = self.getInventory().getSelectedSlot();
            }
            this.ticksToBack = SLOT_RETURN_TICKS;
            self.getInventory().setSelectedSlot(slot);
            syncSelectedSlot();
            hand = Hand.MAIN_HAND;
        }

        if (mc.interactionManager != null && hit != null) {
            ActionResult result;
            placing = true;
            try {
                result = mc.interactionManager.interactBlock(self, hand, hit);
            } finally {
                placing = false;
            }
            if (!result.isAccepted()) return;
            self.swingHand(hand);
            if (isCrystal) {
                long now = System.currentTimeMillis();
                this.placedPositions.put(pos.toImmutable(), now);
                this.placedPositions.put(pos.up().toImmutable(), now);
            }
        }
        BlockPos visual = isCrystal ? pos.up() : pos.offset(hit != null ? hit.getSide() : Direction.UP);
        addVisual(visual, 0.2F, 0.8F, 1.0F);
    }

    private void finishAction() {
        stopMining();
        this.serverRotation = null;
        this.lastData = null;
        this.rotation = null;
        this.rotationInitialized = false;
        this.targetHoldTicks = 0;
        this.noiseYaw = 0.0F;
        this.noisePitch = 0.0F;


    }

    private void syncSelectedSlot() {
        if (mc.interactionManager != null) ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).elexis$syncSelectedSlot();
    }

    private float[] getCurrentRotation(ClientPlayerEntity player) {
        return serverRotation == null ? new float[]{player.getYaw(), player.getPitch()} : new float[]{serverRotation.x, serverRotation.y};
    }

    private boolean isInstantRotation() {
        return this.rotate.is("Vanilla") || this.rotate.is("Без ротации");
    }

    private void applyRotation(float[] rot) {
        this.rotation = rot;
        if (rotate.is("Без ротации")) serverRotation = null;
        if (mc.player == null) return;

        if (this.rotate.is("ReallyWorld")) {
            updateSmoothRotation(mc.player, rot[0], rot[1], 0.65F, true);
        } else if (this.rotate.is("Плавная")) {
            float speed = (float) this.smoothSpeed.get().doubleValue();
            updateSmoothRotation(mc.player, rot[0], rot[1], speed, false);
        } else if (this.rotate.is("Vanilla")) {
            serverRotation = new Vec2f(rot[0], rot[1]);
        }
    }

    private void updateSmoothRotation(ClientPlayerEntity player, float idealYaw, float idealPitch, float lerpSpeed, boolean addNoise) {
        if (!this.rotationInitialized) {
            this.smoothedYaw = serverRotation == null ? player.getYaw() : serverRotation.x;
            this.smoothedPitch = serverRotation == null ? player.getPitch() : serverRotation.y;
            this.rotationInitialized = true;
        }

        float deltaYaw = MathHelper.wrapDegrees(idealYaw - this.smoothedYaw);
        float deltaPitch = idealPitch - this.smoothedPitch;
        float totalDist = (float) Math.hypot(deltaYaw, deltaPitch);

        if (totalDist > 0.001F) {
            float minStep = 2.5F;
            float maxStep = 25.0F + 35.0F * lerpSpeed;
            float step = MathHelper.clamp(totalDist * lerpSpeed * 0.8F + minStep, minStep, maxStep);
            step = Math.min(step, totalDist);

            this.smoothedYaw = MathHelper.wrapDegrees(this.smoothedYaw + (deltaYaw / totalDist) * step);
            this.smoothedPitch = MathHelper.clamp(this.smoothedPitch + (deltaPitch / totalDist) * step, -90.0F, 90.0F);
        }

        if (addNoise) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            float targetNoiseYaw = (float) random.nextDouble(-0.35F, 0.35F);
            float targetNoisePitch = (float) random.nextDouble(-0.25F, 0.25F);
            this.noiseYaw = this.noiseYaw + (targetNoiseYaw - this.noiseYaw) * 0.15F;
            this.noisePitch = this.noisePitch + (targetNoisePitch - this.noisePitch) * 0.15F;
        } else {
            this.noiseYaw = 0.0F;
            this.noisePitch = 0.0F;
        }

        float finalYaw = this.smoothedYaw + this.noiseYaw;
        float finalPitch = MathHelper.clamp(this.smoothedPitch + this.noisePitch, -90.0F, 90.0F);

        serverRotation = new Vec2f(finalYaw, finalPitch);
    }

    private PlayerEntity findTarget(ClientPlayerEntity self, ClientWorld world) {
        Camera camera = mc.gameRenderer.getCamera();
        List<TargetCandidate> targets = new ArrayList<>();
        for (PlayerEntity candidate : world.getPlayers()) {
            if (candidate == self || !candidate.isAlive() || candidate.isSpectator() || isFriend(candidate)) {
                continue;
            }
            if (candidate.isCreative()) {
                continue;
            }
            Vec3d point = nearestTargetPoint(candidate.getBoundingBox(), self.getEyePos());
            if (point == null || self.getEyePos().distanceTo(point) > TARGET_RANGE) {
                continue;
            }
            float[] targetRotation = calculate(point, self);
            Vec3d cameraDirection = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
            Vec3d targetDirection = Vec3d.fromPolar(targetRotation[1], targetRotation[0]);
            double dot = MathHelper.clamp(cameraDirection.dotProduct(targetDirection), -1.0D, 1.0D);
            targets.add(new TargetCandidate(candidate, Math.toDegrees(Math.acos(dot))));
        }
        targets.sort(Comparator.comparingDouble(TargetCandidate::viewAngle));
        return targets.isEmpty() ? null : targets.get(0).player();
    }

    private boolean isFriend(PlayerEntity player) {
        return Elexis.getInstance().getFriendStorage().isFriend(player.getGameProfile().name());
    }

    private Vec3d nearestTargetPoint(Box box, Vec3d eye) {
        double x = MathHelper.clamp(eye.x, box.minX, box.maxX);
        double y = MathHelper.clamp(eye.y, box.minY, box.maxY);
        double z = MathHelper.clamp(eye.z, box.minZ, box.maxZ);
        return new Vec3d(x, y, z);
    }

    private static PlaceData emptyData(PlaceResult result) {
        return new PlaceData(null, null, 0.0D, result, null);
    }

    private PlaceData findBestExplosion(PlayerEntity target, ClientWorld world, ClientPlayerEntity self, Iterable<Entity> entities) {
        EndCrystalEntity bestCrystal = null;
        BlockPos crystalBase = null;
        Direction crystalFace = null;
        double bestCrystalDamage = 0.0D;
        double bestCrystalReal = 0.0D;
        double bestCrystalSelf = 0.0D;
        for (Entity entity : entities) {
            if (!(entity instanceof EndCrystalEntity crystal) || !crystal.isAlive() || crystal.isRemoved() || this.attackedCrystals.containsKey(crystal.getId())) {
                continue;
            }
            Vec3d hitPoint = nearestPoint(crystal, 0.0D, 0.05D);
            if (self.getEyePos().squaredDistanceTo(hitPoint) > HIT_REACH * HIT_REACH) {
                continue;
            }
            double real = explosionDamage(Vec3d.ofCenter(crystal.getBlockPos()), target, false);
            double damage = real + 30.0D;
            if (damage <= bestCrystalDamage || real < 1.0D) {
                continue;
            }
            double selfDamage = explosionDamage(Vec3d.ofCenter(crystal.getBlockPos()), self, false);
            if (selfDamage > MAX_SELF_DAMAGE) {
                continue;
            }

            BlockPos pos = crystal.getBlockPos().down();
            Direction face = Direction.UP;
            BlockPos hitBase = null;
            Direction hitFace = null;
            float[] blockRotation = calculate(getFacePoint(pos, face), self);
            BlockHitResult hit = raycastBlock(blockRotation, pos, PLACE_REACH, self);
            if (hit != null && hit.getSide() == face && rayTrace(blockRotation, HIT_REACH, crystal.getBoundingBox(), self)) {
                hitBase = pos;
                hitFace = face;
            }

            bestCrystalDamage = damage;
            bestCrystalReal = real;
            bestCrystalSelf = selfDamage;
            bestCrystal = crystal;
            crystalBase = hitBase;
            crystalFace = hitFace;
        }
        if (bestCrystal != null) {
            return new PlaceData(crystalBase, crystalFace, bestCrystalDamage, PlaceResult.EXPLOSION, bestCrystal,
            bestCrystalReal, bestCrystalSelf);
        }
        return emptyData(PlaceResult.EXPLOSION);
    }

    private PlaceData findBestObsidian(PlayerEntity target, ClientWorld world, ClientPlayerEntity self, Iterable<Entity> entities) {
        BlockPos bestPos = null;
        Direction bestFace = null;
        double bestDamage = 0.0D;
        double bestReal = 0.0D;
        double bestSelf = 0.0D;
        BlockPos base = self.getBlockPos();
        BlockPos.Mutable check = new BlockPos.Mutable();

        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    check.set(base.getX() + x, base.getY() + y, base.getZ() + z);
                    BlockState state = world.getBlockState(check);
                    if (!state.isSolidBlock(world, check)) {
                        continue;
                    }

                    boolean possibleSide = false;
                    for (Direction candidateFace : Direction.values()) {
                        BlockPos candidatePlace = check.offset(candidateFace);
                        if (candidatePlace.getY() >= (int) target.getY() || !world.getBlockState(candidatePlace).isAir()) {
                            continue;
                        }
                        BlockPos candidateCrystal = candidatePlace.up();
                        if (blockedForObsidian(entities, candidatePlace) == ReturnResult.PLAYER
                        || blockedForObsidian(entities, candidateCrystal) == ReturnResult.PLAYER) {
                            continue;
                        }
                        if (canReachBox(self, reachBox(candidatePlace), 3.8D)) {
                            possibleSide = true;
                            break;
                        }
                    }
                    if (!possibleSide) {
                        continue;
                    }

                    Direction face = bestVisibleFace(world, check, self, true);
                    if (face == null) {
                        continue;
                    }
                    BlockPos placePos = check.offset(face);
                    if (placePos.getY() >= (int) target.getY() || !world.getBlockState(placePos).isAir()) {
                        continue;
                    }
                    BlockPos crystalPos = placePos.up();
                    if (blockedForObsidian(entities, placePos) == ReturnResult.PLAYER
                    || blockedForObsidian(entities, crystalPos) == ReturnResult.PLAYER) {
                        continue;
                    }
                    if (!canReachBox(self, reachBox(placePos), 3.8D)) {
                        continue;
                    }

                    double real = ghostDamage(Vec3d.ofCenter(crystalPos), target, placePos);
                    double damage = real + 13.0D;
                    if (damage <= bestDamage || real < 1.5D) {
                        continue;
                    }
                    double selfDamage = ghostDamage(Vec3d.ofCenter(crystalPos), self, placePos);
                    if (selfDamage > MAX_SELF_DAMAGE) {
                        continue;
                    }
                    if (blockedForObsidian(entities, crystalPos) == ReturnResult.ITEM) {
                        return emptyData(PlaceResult.AWAITING);
                    }
                    bestDamage = damage;
                    bestReal = real;
                    bestSelf = selfDamage;
                    bestPos = check.toImmutable();
                    bestFace = face;
                }
            }
        }
        return new PlaceData(bestPos, bestFace, bestDamage, PlaceResult.PLACE_OBSIDIAN, null, bestReal, bestSelf);
    }

    private PlaceData findBestCrystal(PlayerEntity target, ClientWorld world, ClientPlayerEntity self, Iterable<Entity> entities) {
        BlockPos bestPos = null;
        Direction bestFace = null;
        double bestDamage = 0.0D;
        double bestReal = 0.0D;
        double bestSelf = 0.0D;
        BlockPos base = self.getBlockPos();
        BlockPos.Mutable check = new BlockPos.Mutable();
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    check.set(base.getX() + x, base.getY() + y, base.getZ() + z);
                    if (check.getY() >= (int) target.getY()) {
                        continue;
                    }
                    BlockState state = world.getBlockState(check);
                    if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) {
                        continue;
                    }
                    BlockPos placePos = check.up();
                    if (!world.getBlockState(placePos).isAir()) continue;
                    if (blockedForCrystal(entities, placePos)) {
                        continue;
                    }
                    Long placedAt = this.placedPositions.get(check);
                    if (placedAt == null) placedAt = this.placedPositions.get(placePos);
                    if (placedAt != null) {
                        if (System.currentTimeMillis() - placedAt < 250L) {
                            continue;
                        } else {
                            this.placedPositions.remove(check);
                            this.placedPositions.remove(placePos);
                        }
                    }
                    if (!canReachBox(self, reachBox(placePos), 3.8D)) {
                        continue;
                    }
                    Direction face = bestVisibleFace(world, check, self, false);
                    if (face == null) {
                        continue;
                    }
                    float[] topRotation = calculate(getFacePoint(check, Direction.UP), self);
                    BlockHitResult topHit = raycastBlock(topRotation, check, PLACE_REACH, self);
                    if (topHit != null && topHit.getSide() == Direction.UP) {
                        face = Direction.UP;
                    }
                    double real = explosionDamage(Vec3d.ofCenter(placePos), target, false);
                    double damage = real + 19.0D;
                    if (damage <= bestDamage || real < 1.5D) {
                        continue;
                    }
                    double selfDamage = explosionDamage(Vec3d.ofCenter(placePos), self, false);
                    if (selfDamage > MAX_SELF_DAMAGE) {
                        continue;
                    }
                    bestDamage = damage;
                    bestReal = real;
                    bestSelf = selfDamage;
                    bestPos = check.toImmutable();
                    bestFace = face;
                }
            }
        }
        return new PlaceData(bestPos, bestFace, bestDamage, PlaceResult.PLACE_CRYSTAL, null, bestReal, bestSelf);
    }

    private PlaceData findBestBreak(PlayerEntity target, ClientWorld world, ClientPlayerEntity self, Iterable<Entity> entities) {
        BlockPos bestPos = null;
        Direction bestFace = null;
        double bestDamage = 0.0D;
        double bestReal = 0.0D;
        double bestSelf = 0.0D;
        BlockPos bestBreakPos = null;
        Direction bestBreakFace = null;
        double bestBreakDamage = 0.0D;
        double bestBreakReal = 0.0D;
        double bestBreakSelf = 0.0D;
        BlockPos base = self.getBlockPos();
        BlockPos.Mutable check = new BlockPos.Mutable();

        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    check.set(base.getX() + x, base.getY() + y, base.getZ() + z);
                    if (check.getY() >= (int) target.getY() || !world.getBlockState(check).isSolidBlock(world, check)) {
                        continue;
                    }
                    BlockPos placePos = check.up();
                    if (blockedForBreak(entities, placePos)) {
                        continue;
                    }
                    if (!canReachBox(self, reachBox(placePos), 3.8D)) {
                        continue;
                    }
                    Direction face = bestVisibleFace(world, check, self, false);
                    if (face == null) {
                        continue;
                    }
                    BlockHitResult hit = raycastBlock(calculate(getFacePoint(check, face), self), check, PLACE_REACH, self);
                    if (hit == null || hit.getSide() != face) {
                        continue;
                    }

                    BlockState placeState = world.getBlockState(placePos);
                    if (!placeState.isAir()) {
                        if (findBestHotbarTool(self, placeState) == -1) {
                            continue;
                        }
                        BlockState baseState = world.getBlockState(check);
                        if (!baseState.isOf(Blocks.OBSIDIAN) && findBestHotbarTool(self, baseState) == -1) {
                            continue;
                        }
                        double real = ignoredDamage(Vec3d.ofCenter(placePos), target, placePos);
                        double damage = real + 7.0D;
                        if (damage <= bestBreakDamage || real < 1.5D) {
                            continue;
                        }
                        double selfDamage = ignoredDamage(Vec3d.ofCenter(placePos), self, placePos);
                        if (selfDamage > MAX_SELF_DAMAGE) {
                            continue;
                        }
                        bestBreakDamage = damage;
                        bestBreakReal = real;
                        bestBreakSelf = selfDamage;
                        bestBreakPos = placePos.toImmutable();
                        bestBreakFace = face;
                    } else {
                        if (findBestHotbarTool(self, world.getBlockState(check)) == -1) {
                            continue;
                        }
                        double real = explosionDamage(Vec3d.ofCenter(placePos), target, false);
                        double damage = real + 7.0D;
                        if (damage <= bestDamage || real < 1.5D) {
                            continue;
                        }
                        double selfDamage = explosionDamage(Vec3d.ofCenter(placePos), self, false);
                        if (selfDamage > MAX_SELF_DAMAGE) {
                            continue;
                        }
                        bestDamage = damage;
                        bestReal = real;
                        bestSelf = selfDamage;
                        bestPos = check.toImmutable();
                        bestFace = face;
                    }
                }
            }
        }
        if (bestBreakDamage > bestDamage) {
            return new PlaceData(bestBreakPos, bestBreakFace, bestBreakDamage, PlaceResult.BREAK, null,
            bestBreakReal, bestBreakSelf);
        }
        return new PlaceData(bestPos, bestFace, bestDamage, PlaceResult.BREAK, null, bestReal, bestSelf);
    }

    private Direction bestVisibleFace(ClientWorld world, BlockPos pos, ClientPlayerEntity self, boolean requireAirSide) {
        Direction best = null;
        double bestValue = requireAirSide ? 0.0D : Double.MAX_VALUE;
        for (Direction face : Direction.values()) {
            BlockPos side = pos.offset(face);
            if (requireAirSide && !world.getBlockState(side).isAir()) {
                continue;
            }
            Vec3d point = getFacePoint(pos, face);
            float[] rot = calculate(point, self);
            BlockHitResult hit = raycastBlock(rot, pos, 4.5D, self);
            if (hit == null) {
                continue;
            }
            if (requireAirSide) {
                double damage = ghostDamage(Vec3d.ofCenter(side.up()), self, side);
                if (damage > bestValue) {
                    bestValue = damage;
                    best = face;
                }
            } else {
                double distance = self.getEyePos().squaredDistanceTo(point);
                if (distance < bestValue) {
                    bestValue = distance;
                    best = face;
                }
            }
        }
        return best;
    }

    private static Box reachBox(BlockPos pos) {
        return new Box(pos.getX() - 0.5D, pos.getY(), pos.getZ() - 0.5D,
        pos.getX() + 2.0D, pos.getY() + 1.5D, pos.getZ() + 1.5D);
    }

    private static boolean intersectsEntityBox(BlockPos pos, Entity entity) {
        Box crystalBox = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 2.0D, pos.getZ() + 1.0D);
        return entity.getBoundingBox().intersects(crystalBox);
    }

    private ReturnResult blockedForObsidian(Iterable<Entity> entities, BlockPos pos) {
        Box blockBox = new Box(pos.getX(), pos.getY() - 0.2D, pos.getZ(),
        pos.getX() + 1.0D, pos.getY() + 2.2D, pos.getZ() + 1.0D);
        for (Entity entity : entities) {
            if (entity instanceof ItemEntity && !entity.isOnGround() && entity.getBoundingBox().intersects(blockBox)) {
                return ReturnResult.ITEM;
            }
            if ((entity instanceof PlayerEntity || entity instanceof EndCrystalEntity)
            && intersectsEntityBox(pos, entity)) {
                return ReturnResult.PLAYER;
            }
        }
        return ReturnResult.NONE;
    }

    private boolean blockedForCrystal(Iterable<Entity> entities, BlockPos pos) {
        for (Entity entity : entities) {
            if (intersectsEntityBox(pos, entity)) {
                return true;
            }
        }
        return false;
    }

    private boolean blockedForBreak(Iterable<Entity> entities, BlockPos pos) {
        for (Entity entity : entities) {
            if (!(entity instanceof ItemEntity) && intersectsEntityBox(pos, entity)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCrystals(ClientPlayerEntity self) {
        return self.getOffHandStack().isOf(Items.END_CRYSTAL)
        || self.getMainHandStack().isOf(Items.END_CRYSTAL)
        || findHotbar(self, Items.END_CRYSTAL) != -1;
    }

    private boolean hasObsidian(ClientPlayerEntity self) {
        return self.getOffHandStack().isOf(Items.OBSIDIAN)
        || self.getMainHandStack().isOf(Items.OBSIDIAN)
        || findHotbar(self, Items.OBSIDIAN) != -1;
    }

    private int findHotbar(ClientPlayerEntity player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    private int findBestHotbarTool(ClientPlayerEntity player, BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private float explosionDamage(Vec3d source, PlayerEntity player, boolean optimized) {
        DamageKey key = new DamageKey(source, player.getId(), null, optimized ? (byte) 1 : (byte) 0);
        return this.damageCache.computeIfAbsent(key, ignored ->
        ExplosionDamage.crystal(source, player));
    }

    private float ghostDamage(Vec3d source, PlayerEntity player, BlockPos ghost) {
        BlockPos stable = ghost.toImmutable();
        DamageKey key = new DamageKey(source, player.getId(), stable, (byte) 2);
        return this.damageCache.computeIfAbsent(key, ignored ->
        ExplosionDamage.damage(source, player, player.getEntityPos(), 12, List.of(), List.of(stable)));
    }

    private float ignoredDamage(Vec3d source, PlayerEntity player, BlockPos ignoredBlock) {
        BlockPos stable = ignoredBlock.toImmutable();
        DamageKey key = new DamageKey(source, player.getId(), stable, (byte) 3);
        return this.damageCache.computeIfAbsent(key, ignored ->
        ExplosionDamage.damage(source, player, player.getEntityPos(), 12, List.of(stable), List.of()));
    }

    private boolean canReachBox(ClientPlayerEntity player, Box box, double maxDistance) {
        Vec3d eye = player.getEyePos();
        double clampedX = MathHelper.clamp(eye.x, box.minX, box.maxX);
        double clampedY = MathHelper.clamp(eye.y, box.minY, box.maxY);
        double clampedZ = MathHelper.clamp(eye.z, box.minZ, box.maxZ);
        return eye.squaredDistanceTo(clampedX, clampedY, clampedZ) <= maxDistance * maxDistance;
    }

    private Vec3d getFacePoint(BlockPos pos, Direction face) {
        return new Vec3d(
        pos.getX() + 0.5D + face.getOffsetX() * 0.5D,
        pos.getY() + 0.5D + face.getOffsetY() * 0.5D,
        pos.getZ() + 0.5D + face.getOffsetZ() * 0.5D
        );
    }

    private float[] calculate(Vec3d point, ClientPlayerEntity player) {
        double dx = point.x - player.getX();
        double dy = point.y - player.getEyeY();
        double dz = point.z - player.getZ();
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));
        return new float[]{yaw, pitch};
    }

    private static Vec3d nearestPoint(Entity crystal, double offsetXZ, double offsetY) {
        if (mc.player == null) return crystal.getEntityPos();
        Box box = crystal.getBoundingBox();
        double minX = box.minX + offsetXZ;
        double maxX = box.maxX - offsetXZ;
        double minY = box.minY + offsetY;
        double maxY = box.maxY - offsetY;
        double minZ = box.minZ + offsetXZ;
        double maxZ = box.maxZ - offsetXZ;
        Vec3d eye = mc.player.getEyePos();
        double x = MathHelper.clamp(eye.x, minX, maxX);
        double y = MathHelper.clamp(eye.y, minY, maxY);
        double z = MathHelper.clamp(eye.z, minZ, maxZ);
        return new Vec3d(x, y, z);
    }

    private BlockHitResult raycastBlock(float[] rot, BlockPos targetPos, double maxDistance, ClientPlayerEntity player) {
        if (player == null || mc.world == null) return null;
        Vec3d eye = player.getEyePos();
        Vec3d look = Vec3d.fromPolar(rot[1], rot[0]);
        Vec3d end = eye.add(look.multiply(maxDistance));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
        eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player
        ));
        if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(targetPos)) {
            return hit;
        }
        return null;
    }

    private boolean rayTrace(float[] rot, double maxDistance, Box box, ClientPlayerEntity player) {
        if (player == null) return false;
        Vec3d eye = player.getEyePos();
        Vec3d look = Vec3d.fromPolar(rot[1], rot[0]);
        Vec3d end = eye.add(look.multiply(maxDistance));
        return box.raycast(eye, end).isPresent();
    }

    private boolean isLookingAtEntity(float[] rot, Entity entity, double maxDistance, ClientPlayerEntity player) {
        return rayTrace(rot, maxDistance, entity.getBoundingBox(), player);
    }

    private Direction getFaceDirection(float[] rot, ClientPlayerEntity player, BlockPos pos) {
        BlockHitResult hit = raycastBlock(rot, pos, PLACE_REACH, player);
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            return hit.getSide();
        }
        return Direction.UP;
    }

    private void addVisual(BlockPos pos, float r, float g, float b) {
        for (VisualData visual : this.visuals) {
            if (visual.pos.equals(pos)) {
                return;
            }
        }
        this.visuals.add(new VisualData(pos.toImmutable(), r, g, b));
    }

    @EventHandler
    public void onRender3D(EventRender.World event) {
        long now = System.currentTimeMillis();
        visuals.removeIf(data -> now - data.startedAt > VISUAL_MS);
        if (render.get() && mc.player != null && mc.world != null && !visuals.isEmpty()) {
            CrystalAuraRenderer.render(event, visuals, now, VISUAL_MS);
        }
    }

    public record PlaceData(BlockPos blockPos, Direction face, double damage, PlaceResult placeResult, Entity entity,
    double real, double self) {
        public PlaceData(BlockPos blockPos, Direction face, double damage, PlaceResult placeResult, Entity entity) {
            this(blockPos, face, damage, placeResult, entity, damage, 0.0D);
        }
    }

    private record TargetCandidate(PlayerEntity player, double viewAngle) {
    }

    private record DamageKey(Vec3d source, int entityId, BlockPos special, byte mode) {
    }

    public enum PlaceResult {
        PLACE_OBSIDIAN, PLACE_CRYSTAL, EXPLOSION, BREAK, AWAITING
    }

    public enum ReturnResult {
        ITEM, PLAYER, NONE
    }

    public static final class VisualData {
        private final BlockPos pos;
        private final long startedAt = System.currentTimeMillis();
        private final float r;
        private final float g;
        private final float b;

        public VisualData(BlockPos pos, float r, float g, float b) {
            this.pos = pos;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public BlockPos getBlockPos() {
            return this.pos;
        }

        public long getStartedAt() {
            return this.startedAt;
        }

        public long getCreatedAt() {
            return this.startedAt;
        }

        public float getR() {
            return this.r;
        }

        public float getG() {
            return this.g;
        }

        public float getB() {
            return this.b;
        }
    }
}
