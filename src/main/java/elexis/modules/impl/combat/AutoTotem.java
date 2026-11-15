package elexis.modules.impl.combat;

import com.google.gson.*;
import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import elexis.modules.impl.combat.javelin.ExplosionDamage;
import elexis.modules.impl.combat.javelin.JavelinCombat;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.item.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.registry.Registries;
import net.minecraft.block.*;
import net.minecraft.util.math.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.attribute.EnvironmentAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/** Port of Javelin's damage-aware, two-hand AutoTotem with per-item profiles. */
@FunctionAdd(name="AutoTotem", alias="AutoTotem", category=Category.Combat,
        description="Totem on damage, tridents and falling, two hands for dynamite")
public final class AutoTotem extends Function {
    public enum Protection { NONE, OFFHAND, BOTH_HANDS }
    public final BooleanSetting saveEnchanted = new BooleanSetting("Keep enchanted", true);
    public final MultiBooleanSetting conditions = new MultiBooleanSetting("Work on",
            new BooleanSetting("Tridents", true), new BooleanSetting("Fall", true),
            new BooleanSetting("Dynamite A", true), new BooleanSetting("Dynamite B", true));
    public final SliderSetting health = new SliderSetting("Health", 4.5f, 1, 20, .5f);
    public final SliderSetting healthElytra = new SliderSetting("Health on elytra", 4.5f, 1, 20, .5f);
    public final SliderSetting damageFromCrystal = new SliderSetting("Damage from crystal", 4.5f, 1, 100, .5f);
    public final SliderSetting damageFromAnchor = new SliderSetting("Damage from anchor", 4.5f, 1, 100, .5f);
    public final SliderSetting damageFromCart = new SliderSetting("Damage from TNT minecart", 4.5f, 1, 100, .5f);
    public final ModeSetting swapMode = new ModeSetting("Swap mode", "Default", "Default", "Ground");
    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private Profile globalProfile = new Profile();
    private String editingProfile = "General";
    private boolean loadingProfile;
    public final ModeSetting profileSelector = new ModeSetting("Item profile", "General", "General") {
        @Override public void set(String value) {
            if (!loadingProfile) storeEditor();
            super.set(value);
            editingProfile = value;
            if (!loadingProfile) loadEditor(profiles.getOrDefault(value, globalProfile));
        }
    };
    private final StringSetting profileData = new StringSetting("Profile data", "") {
        @Override public String get() { storeEditor(); return saveProfiles(); }
        @Override public void set(String value) { super.set(value); loadProfiles(value); }
    };
    public final Map<Integer, Vec3d> knownCrystals = new HashMap<>();
    public final List<BlockPos> knownAnchors = new ArrayList<>();
    public final Map<Integer, Vec3d> knownDangerousDynamites = new HashMap<>();
    public final Queue<Packet<?>> pendingPackets = new ConcurrentLinkedQueue<>();
    public final Deque<Vec3d> positionHistory = new ArrayDeque<>(3);
    public ClientWorld trackedLevel;
    public ClientPlayerEntity trackedPlayer;
    public ItemStack swapBackStack = ItemStack.EMPTY;
    public int swapBackSlot = -1;
    public ItemStack mainHandSwapBackStack = ItemStack.EMPTY;
    public int mainHandSwapBackSlot = -1;
    public int mainHandHotbarSlot = -1;
    public int cooldownTicks, safeTicks;
    private int anchorScanAge = Integer.MIN_VALUE;
    public boolean swappedByModule, mainHandSwappedByModule, offhandSwapPending, mainHandSwapPending, fallDangerActive;
    public long groundPopReadyAtNanos;
    public int groundPopCount;

    public AutoTotem() {
        profileData.setVisible(() -> false);
        addSettings(profileSelector, new ButtonSetting("Add item from hand", this::addProfile),
                new ButtonSetting("Delete profile", this::removeProfile), saveEnchanted, conditions, health,
                healthElytra, damageFromCrystal, damageFromAnchor, damageFromCart, swapMode, profileData);
    }

    @EventHandler public void onUpdate(EventUpdate.Fast event) {
        if (trackedLevel != mc.world || trackedPlayer != mc.player) {
            resetRuntimeState(); trackedLevel = mc.world; trackedPlayer = mc.player;
        }
        if (!canOperate()) { resetRuntimeState(); return; }
        processPendingPackets();
        refreshCrystalPositions();
        if (anchorScanAge == Integer.MIN_VALUE || mc.player.age - anchorScanAge >= 4) {
            refreshAnchorPositions();
            anchorScanAge = mc.player.age;
        }
        refreshDangerousDynamitePositions();
        recordPlayerPosition();
        tickAutoTotem();
    }

    @Override public void onDisable() {
        if (mc.player == trackedPlayer && mc.world == trackedLevel && canOperate()) {
            swapMainHandBack(); swapBack();
        }
        resetRuntimeState(); trackedPlayer = null; trackedLevel = null;
        super.onDisable();
    }

   public void tickAutoTotem() {
      if (!this.canOperate()) {
         this.fallDangerActive = false;
         this.safeTicks = 0;
         this.tickCooldown();
      } else {
         Protection protection = this.evaluateProtectionMode();
         if (protection == Protection.BOTH_HANDS) {
            this.safeTicks = 0;
            this.swapTotemsToBothHands();
         } else if (protection == Protection.OFFHAND) {
            this.safeTicks = 0;
            this.swapTotem();
            if (this.mainHandSwappedByModule) {
               if (this.cooldownTicks > 0) {
                  this.tickCooldown();
               } else {
                  this.swapMainHandBack();
               }
            }
         } else {
            if (!this.swappedByModule && !this.mainHandSwappedByModule) {
               this.safeTicks = 0;
            } else {
               this.safeTicks++;
            }

            if (this.cooldownTicks > 0) {
               this.tickCooldown();
            } else if (this.safeTicks >= 10) {
               if (this.mainHandSwappedByModule) {
                  this.swapMainHandBack();
               } else {
                  if (this.swappedByModule) {
                     this.swapBack();
                  }
               }
            }
         }
      }
   }

   public boolean condition() {
      if (!this.canOperate()) {
         this.fallDangerActive = false;
         return false;
      } else {
         return this.evaluateProtectionMode() != Protection.NONE;
      }
   }

   public Protection evaluateProtectionMode() {
      Profile profile = profileFor(profileStack());
      boolean fallDanger = profile.falling && mc.player.fallDistance > 10;
      if (fallDanger) {
         BlockPos.Mutable ground = mc.player.getBlockPos().mutableCopy();
         while (ground.getY() > mc.world.getBottomY() && !mc.world.getBlockState(ground).isSolidBlock(mc.world, ground)) {
            ground.move(Direction.DOWN);
         }
         fallDanger = (!mc.player.isGliding() || mc.player.getBlockY() - ground.getY() < 8)
                 && !mc.world.getBlockState(ground.up()).isOf(Blocks.WATER);
      }
      if (fallDanger && !fallDangerActive) addCooldown(2);
      fallDangerActive = fallDanger;
      if (!knownDangerousDynamites.isEmpty()) return Protection.BOTH_HANDS;
      float currentHealth = currentHealth();
      if (currentHealth <= profile.health) return Protection.OFFHAND;
      if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
              && currentHealth <= profile.elytraHealth) return Protection.OFFHAND;
      Vec3d past = positionTwoTicksAgo();
      for (Vec3d crystal : knownCrystals.values()) {
         if (ExplosionDamage.crystal(crystal, mc.player) > profile.crystalDamage
                 || past != null && ExplosionDamage.damage(crystal, mc.player, past, 12, List.of(), List.of()) > profile.crystalDamage) return Protection.OFFHAND;
      }
      for (BlockPos anchor : knownAnchors) {
         if (ExplosionDamage.anchor(anchor, mc.player) > profile.anchorDamage
                 || past != null && ExplosionDamage.damage(Vec3d.ofCenter(anchor), mc.player, past, 10, List.of(anchor), List.of()) > profile.anchorDamage) return Protection.OFFHAND;
      }
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof TntMinecartEntity && ExplosionDamage.crystal(entity.getEntityPos(), mc.player) > profile.cartDamage) return Protection.OFFHAND;
      }
      if (fallDanger || profile.tridents && tridentDanger()) return Protection.OFFHAND;
      return Protection.NONE;
   }

   public void refreshDangerousDynamitePositions() {
      this.knownDangerousDynamites.clear();
      Profile profile = profileFor(profileStack());
      boolean watchA = profile.dynamiteA;
      boolean watchB = profile.dynamiteB;
      if (watchA || watchB) {
         String normalizedA = normalizeDynamiteLabel("Dynamite A");
         String normalizedB = normalizeDynamiteLabel("Dynamite B");

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TntEntity dynamite) {
               int fuse = dynamite.getFuse();
               if (fuse > 0 && fuse <= 40) {
                  String label = dynamite.getCustomName() != null ? dynamite.getCustomName().getString() : dynamite.getName().getString();
                  String normalized = normalizeDynamiteLabel(label);
                  if (watchA && normalized.equals(normalizedA) || watchB && normalized.equals(normalizedB)) {
                     this.knownDangerousDynamites.put(dynamite.getId(), dynamite.getEntityPos());
                  }
               }
            }
         }
      }
   }

   public static String normalizeDynamiteLabel(String label) {
      return label.trim().replace(' ', ' ').replaceAll("\\s+", " ").toUpperCase(Locale.ROOT).replace('А', 'A').replace('В', 'B');
   }

   public float currentHealth() {
      return mc.player.getHealth()
         + (
            mc.player.hasStatusEffect(StatusEffects.ABSORPTION) && mc.player.getStatusEffect(StatusEffects.ABSORPTION).getDuration() > 40
               ? mc.player.getAbsorptionAmount()
               : 0.0F
         );
   }

   public ItemStack profileStack() {
      return this.swappedByModule && !this.swapBackStack.isEmpty() ? this.swapBackStack : mc.player.getOffHandStack();
   }

   public boolean swapTotem() {
      ItemStack offhand = mc.player.getOffHandStack();
      boolean offhandTotem = offhand.isOf(Items.TOTEM_OF_UNDYING);
      boolean preserveEnchanted = this.saveEnchanted.get();
      if (!offhandTotem || preserveEnchanted && this.isEnchanted(offhand) && !this.swappedByModule) {
         if (this.offhandSwapPending) {
            return true;
         }

         int slot = this.findTotemSlot(
            preserveEnchanted, this.mainHandSwappedByModule ? this.mainHandHotbarSlot : -1, this.mainHandSwappedByModule ? this.mainHandSwapBackSlot : -1
         );
         if (slot == -1) {
            return offhandTotem;
         }

         if (this.equipOffhandTotem(slot)) {
            this.addCooldown(2);
         }

         return true;
      } else {
         return true;
      }
   }

   public void swapTotemsToBothHands() {
      if (this.mainHandSwappedByModule && this.mainHandHotbarSlot >= 0 && mc.player.getInventory().getSelectedSlot() != this.mainHandHotbarSlot) {
         JavelinCombat.select(this.mainHandHotbarSlot);
      }

      boolean preserveEnchanted = this.saveEnchanted.get();
      ItemStack offhand = mc.player.getOffHandStack();
      ItemStack mainHand = mc.player.getMainHandStack();
      boolean offhandReady = offhand.isOf(Items.TOTEM_OF_UNDYING);
      boolean mainHandReady = mainHand.isOf(Items.TOTEM_OF_UNDYING);
      int selectedSlot = mc.player.getInventory().getSelectedSlot();
      int offhandSource = -1;
      if (!offhandReady && !this.offhandSwapPending) {
         offhandSource = this.findTotemSlot(
            preserveEnchanted, mainHand.isOf(Items.TOTEM_OF_UNDYING) ? selectedSlot : -1, this.mainHandSwappedByModule ? this.mainHandSwapBackSlot : -1
         );
      }

      int mainHandSource = -1;
      if (!mainHandReady && !this.mainHandSwapPending) {
         mainHandSource = this.findTotemSlot(
            preserveEnchanted, offhandSource, selectedSlot, this.protectedTotemSlot(), this.mainHandSwappedByModule ? this.mainHandSwapBackSlot : -1
         );
      }

      boolean changed = false;
      if (offhandSource != -1) {
         changed |= this.equipOffhandTotem(offhandSource);
      }

      if (mainHandSource != -1) {
         changed |= this.equipMainHandTotem(mainHandSource);
      }

      if (changed) {
         this.addCooldown(2);
      }
   }

   public boolean equipOffhandTotem(int slot) {
      if (this.offhandSwapPending || JavelinCombat.inventorySlot(slot) == null
              || !mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
         return false;
      }

      if (this.groundSwaps() && slot > 8 && System.nanoTime() < this.groundPopReadyAtNanos) {
         return false;
      }

      if (!this.swappedByModule) {
         this.swapBackStack = mc.player.getOffHandStack().copy();
         this.swapBackSlot = slot;
         this.swappedByModule = true;
      }

      return this.queueOffhandSwap(slot, null);
   }

   public boolean equipMainHandTotem(int slot) {
      if (this.mainHandSwapPending || JavelinCombat.inventorySlot(slot) == null
              || !mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
         return false;
      }

      int selectedSlot = this.mainHandSwappedByModule ? this.mainHandHotbarSlot : mc.player.getInventory().getSelectedSlot();
      if (!this.mainHandSwappedByModule) {
         this.mainHandSwapBackStack = mc.player.getMainHandStack().copy();
         this.mainHandSwapBackSlot = slot;
         this.mainHandHotbarSlot = selectedSlot;
         this.mainHandSwappedByModule = true;
      }

      if (mc.player.getInventory().getSelectedSlot() != selectedSlot) {
         JavelinCombat.select(selectedSlot);
      }

      return this.queueMainHandSwap(slot, selectedSlot, null);
   }

   public int findTotemSlot(boolean preserveEnchanted, int... excludedSlots) {
      int plain = this.findTotemSlot(false, excludedSlots, false);
      return plain != -1 ? plain : this.findTotemSlot(true, excludedSlots, preserveEnchanted);
   }

   public int findTotemSlot(boolean enchanted, int[] excludedSlots, boolean excludeProtectedTotem) {
      int protectedSlot = excludeProtectedTotem ? this.protectedTotemSlot() : -1;

      for (int slot = 0; slot < 36; slot++) {
         if (slot != protectedSlot && !isExcluded(slot, excludedSlots)) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isOf(Items.TOTEM_OF_UNDYING) && this.isEnchanted(stack) == enchanted) {
               return slot;
            }
         }
      }

      return -1;
   }

   public static boolean isExcluded(int slot, int[] excludedSlots) {
      for (int excluded : excludedSlots) {
         if (slot == excluded) {
            return true;
         }
      }

      return false;
   }

   public void swapBack() {
      if (this.swappedByModule && mc.player != null && !this.offhandSwapPending) {
         ItemStack offhand = mc.player.getOffHandStack();
         if (this.swapBackStack.isEmpty() || !offhand.isEmpty() && !offhand.isOf(Items.TOTEM_OF_UNDYING)
                 || this.matchesSwapBackStack(offhand)) {
            this.clearSwapState();
         } else {
            int oldItemSlot = this.isSwapBackStackAt(this.swapBackSlot) ? this.swapBackSlot : JavelinCombat.find(this::matchesSwapBackStack, 0, 36);
            if (oldItemSlot != -1) {
               this.queueOffhandSwap(oldItemSlot, this::clearSwapState);
            } else this.clearSwapState();
         }
      }
   }

   public void swapMainHandBack() {
      if (this.mainHandSwappedByModule && mc.player != null && !this.mainHandSwapPending) {
         if (this.mainHandHotbarSlot >= 0 && this.mainHandHotbarSlot <= 8) {
            ItemStack protectedSlotStack = mc.player.getInventory().getStack(this.mainHandHotbarSlot);
            if (this.mainHandSwapBackStack.isEmpty()
                    || !protectedSlotStack.isEmpty() && !protectedSlotStack.isOf(Items.TOTEM_OF_UNDYING)
                    || this.matchesMainHandSwapBackStack(protectedSlotStack)) {
               this.clearMainHandSwapState();
            } else {
               int oldItemSlot = this.isMainHandSwapBackStackAt(this.mainHandSwapBackSlot)
                  ? this.mainHandSwapBackSlot
                  : JavelinCombat.find(this::matchesMainHandSwapBackStack, 0, 36);
               if (oldItemSlot != -1) {
                  this.queueMainHandSwap(oldItemSlot, this.mainHandHotbarSlot, this::clearMainHandSwapState);
               } else this.clearMainHandSwapState();
            }
         } else {
            this.clearMainHandSwapState();
         }
      }
   }

   public boolean queueOffhandSwap(int inventorySlot, Runnable completion) {
      if (offhandSwapPending || !canOperate()) return false;
      offhandSwapPending = true;
      try {
         if (!JavelinCombat.swapInventory(inventorySlot, 40)) return false;
         if (completion != null) completion.run();
         return true;
      } finally { offhandSwapPending = false; }
   }

   public boolean queueMainHandSwap(int inventorySlot, int hotbarSlot, Runnable completion) {
      if (mainHandSwapPending || !canOperate() || inventorySlot == hotbarSlot
              || hotbarSlot < 0 || hotbarSlot > 8) return false;
      mainHandSwapPending = true;
      try {
         if (!JavelinCombat.swapInventory(inventorySlot, hotbarSlot)) return false;
         if (completion != null) completion.run();
         return true;
      } finally { mainHandSwapPending = false; }
   }

   public boolean groundSwaps() { return swapMode.is("Ground"); }

   public boolean isEnchanted(ItemStack stack) {
      return stack.hasEnchantments();
   }

   public boolean matchesSwapBackStack(ItemStack stack) {
      return ItemStack.areEqual(stack, this.swapBackStack);
   }

   public boolean matchesMainHandSwapBackStack(ItemStack stack) {
      return ItemStack.areEqual(stack, this.mainHandSwapBackStack);
   }

   public boolean isMainHandSwapBackStackAt(int slot) {
      return slot >= 0 && slot < 36 && slot != this.mainHandHotbarSlot && this.matchesMainHandSwapBackStack(mc.player.getInventory().getStack(slot));
   }

   public int protectedTotemSlot() {
      if (!this.swappedByModule || !this.swapBackStack.isOf(Items.TOTEM_OF_UNDYING) || !this.isEnchanted(this.swapBackStack)) {
         return -1;
      } else {
         return this.isSwapBackStackAt(this.swapBackSlot) ? this.swapBackSlot : JavelinCombat.find(this::matchesSwapBackStack, 0, 36);
      }
   }

   public boolean isSwapBackStackAt(int slot) {
      return slot >= 0 && slot < 36 && this.matchesSwapBackStack(mc.player.getInventory().getStack(slot));
   }

   @EventHandler
   public void onPacket(EventReceivePacket event) {
      if (event.getPacket() instanceof EntityStatusS2CPacket || event.getPacket() instanceof ExplosionS2CPacket) pendingPackets.add(event.getPacket());
   }

   public void processPendingPackets() {
      Packet<?> packet;
      while ((packet = this.pendingPackets.poll()) != null) {
         if (packet instanceof EntityStatusS2CPacket entityEvent && entityEvent.getStatus() == 35 && entityEvent.getEntity(mc.world) == mc.player) {
            this.addCooldown(5);
            long delayMs = (++this.groundPopCount & 1) == 0 ? 275L : 125L;
            this.groundPopReadyAtNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMs);
         } else if (packet instanceof ExplosionS2CPacket explosion) {
            if (this.isDynamiteExplosion(explosion.center())) {
               this.addCooldown(5);
            } else if (this.isAnchorExplosion(explosion.center()) && this.isNearPlayer(explosion.center(), 10.0, 10.0)) {
               this.addCooldown(5);
            } else if (this.isCrystalExplosion(explosion.center()) && this.isNearPlayer(explosion.center(), 8.0, 4.0)) {
               this.addCooldown(5);
            }
         }
      }
   }

   public boolean isDynamiteExplosion(Vec3d center) {
      for (Vec3d position : this.knownDangerousDynamites.values()) {
         if (position.squaredDistanceTo(center) <= 4.0) {
            return true;
         }
      }

      return false;
   }

   public void refreshCrystalPositions() {
      this.knownCrystals.clear();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity) {
            this.knownCrystals.put(entity.getId(), entity.getEntityPos());
         }
      }
   }

   public void refreshAnchorPositions() {
      this.knownAnchors.clear();
      BlockPos center = mc.player.getBlockPos();
      if (Boolean.TRUE.equals(mc.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, center))) return;

      for (BlockPos pos : BlockPos.iterate(center.add(-12, -12, -12), center.add(12, 12, 12))) {
         BlockState state = mc.world.getBlockState(pos);
         if (state.isOf(Blocks.RESPAWN_ANCHOR)
            && state.get(RespawnAnchorBlock.CHARGES) > 0
            && !Boolean.TRUE.equals(mc.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, pos))) {
            this.knownAnchors.add(pos.toImmutable());
         }
      }
   }

   public void recordPlayerPosition() {
      this.positionHistory.addLast(mc.player.getEntityPos());

      while (this.positionHistory.size() > 3) {
         this.positionHistory.removeFirst();
      }
   }

   public Vec3d positionTwoTicksAgo() {
      return this.positionHistory.size() < 3 ? null : this.positionHistory.peekFirst();
   }

   public boolean isCrystalExplosion(Vec3d center) {
      for (Vec3d crystalPos : this.knownCrystals.values()) {
         if (crystalPos.squaredDistanceTo(center) <= 4.0) {
            return true;
         }
      }

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity && entity.getEntityPos().squaredDistanceTo(center) <= 4.0) {
            return true;
         }
      }

      return false;
   }

   public boolean isAnchorExplosion(Vec3d center) {
      for (BlockPos anchorPos : this.knownAnchors) {
         if (Vec3d.ofCenter(anchorPos).squaredDistanceTo(center) <= 1.0) {
            return true;
         }
      }

      return false;
   }

   public boolean isNearPlayer(Vec3d position, double horizontal, double vertical) {
      double x = position.x - mc.player.getX();
      double z = position.z - mc.player.getZ();
      return Math.hypot(x, z) <= horizontal && Math.abs(position.y - mc.player.getY()) <= vertical;
   }

   public boolean canOperate() {
      return mc.player != null && mc.world != null && mc.interactionManager != null && mc.player.isAlive() && !mc.player.isCreative() && !mc.player.isSpectator();
   }

   public void addCooldown(int ticks) {
      if (ticks > 0) {
         this.cooldownTicks = this.cooldownTicks > Integer.MAX_VALUE - ticks ? Integer.MAX_VALUE : this.cooldownTicks + ticks;
      }
   }

   public void tickCooldown() {
      if (this.cooldownTicks > 0) {
         this.cooldownTicks--;
      }
   }

   public void clearSwapState() {
      this.swappedByModule = false;
      this.swapBackStack = ItemStack.EMPTY;
      this.swapBackSlot = -1;
   }

   public void clearMainHandSwapState() {
      this.mainHandSwappedByModule = false;
      this.mainHandSwapBackStack = ItemStack.EMPTY;
      this.mainHandSwapBackSlot = -1;
      this.mainHandHotbarSlot = -1;
   }

   public void resetRuntimeState() {
      this.knownCrystals.clear();
      this.knownAnchors.clear();
      this.knownDangerousDynamites.clear();
      this.pendingPackets.clear();
      this.positionHistory.clear();
      this.clearSwapState();
      this.clearMainHandSwapState();
      this.cooldownTicks = 0;
      this.safeTicks = 0;
      this.anchorScanAge = Integer.MIN_VALUE;
      this.offhandSwapPending = false;
      this.mainHandSwapPending = false;
      this.fallDangerActive = false;
      this.groundPopReadyAtNanos = 0L;
      this.groundPopCount = 0;
   }

   private static final class Profile {
      String item = "", name = "", attributes = "";
      float health = 4.5f, elytraHealth = 4.5f, crystalDamage = 4.5f, anchorDamage = 4.5f, cartDamage = 4.5f;
      boolean tridents = true, falling = true, dynamiteA = true, dynamiteB = true;
   }

   private Profile readEditor() {
      Profile profile = new Profile();
      profile.health = health.get();
      profile.elytraHealth = healthElytra.get();
      profile.crystalDamage = damageFromCrystal.get();
      profile.anchorDamage = damageFromAnchor.get();
      profile.cartDamage = damageFromCart.get();
      profile.tridents = conditions.get("Tridents");
      profile.falling = conditions.get("Fall");
      profile.dynamiteA = conditions.get("Dynamite A");
      profile.dynamiteB = conditions.get("Dynamite B");
      return profile;
   }

   private void storeEditor() {
      if (loadingProfile) return;
      Profile updated = readEditor();
      Profile previous = profiles.get(editingProfile);
      if (previous == null) globalProfile = updated;
      else {
         updated.item = previous.item;
         updated.name = previous.name;
         updated.attributes = previous.attributes;
         profiles.put(editingProfile, updated);
      }
   }

   private void loadEditor(Profile profile) {
      health.set(profile.health);
      healthElytra.set(profile.elytraHealth);
      damageFromCrystal.set(profile.crystalDamage);
      damageFromAnchor.set(profile.anchorDamage);
      damageFromCart.set(profile.cartDamage);
      conditions.getByName("Tridents").set(profile.tridents);
      conditions.getByName("Fall").set(profile.falling);
      conditions.getByName("Dynamite A").set(profile.dynamiteA);
      conditions.getByName("Dynamite B").set(profile.dynamiteB);
   }

   private static String attributes(ItemStack stack) {
      return String.valueOf(stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS));
   }

   private static boolean sameProfileItem(ItemStack first, ItemStack second) {
      return !first.isEmpty() && !second.isEmpty() && first.isOf(second.getItem())
              && first.getName().equals(second.getName()) && attributes(first).equals(attributes(second));
   }

   private Profile profileFor(ItemStack stack) {
      storeEditor();
      if (!stack.isEmpty()) {
         String item = Registries.ITEM.getId(stack.getItem()).toString();
         for (Profile profile : profiles.values()) {
            if (profile.item.equals(item) && profile.name.equals(stack.getName().getString())
                    && profile.attributes.equals(attributes(stack))) return profile;
         }
      }
      return globalProfile;
   }

   public void addProfile() {
      if (mc.player == null || mc.player.getMainHandStack().isEmpty()) return;
      ItemStack stack = mc.player.getMainHandStack();
      storeEditor();
      String itemId = Registries.ITEM.getId(stack.getItem()).toString();
      for (var entry : profiles.entrySet()) {
         Profile profile = entry.getValue();
         if (profile.item.equals(itemId) && profile.name.equals(stack.getName().getString())
                 && profile.attributes.equals(attributes(stack))) {
            profileSelector.set(entry.getKey());
            return;
         }
      }
      Profile profile = readEditor();
      profile.item = itemId;
      profile.name = stack.getName().getString();
      profile.attributes = attributes(stack);
      String label = profile.name;
      int suffix = 2;
      while (profiles.containsKey(label) || label.equals("General")) label = profile.name + " (" + suffix++ + ")";
      profiles.put(label, profile);
      refreshProfileChoices();
      profileSelector.set(label);
   }

   public void removeProfile() {
      if (!profiles.containsKey(editingProfile)) return;
      profiles.remove(editingProfile);
      loadingProfile = true;
      try {
         refreshProfileChoices();
         profileSelector.set("General");
         editingProfile = "General";
         loadEditor(globalProfile);
      } finally { loadingProfile = false; }
   }

   private void refreshProfileChoices() {
      List<String> labels = new ArrayList<>();
      labels.add("General");
      labels.addAll(profiles.keySet());
      profileSelector.strings = labels.toArray(String[]::new);
      profileSelector.modes = List.copyOf(labels);
   }

   private String saveProfiles() {
      Gson gson = new Gson();
      JsonObject data = new JsonObject();
      data.add("default", gson.toJsonTree(globalProfile));
      data.add("items", gson.toJsonTree(profiles));
      return gson.toJson(data);
   }

   private void loadProfiles(String json) {
      if (json == null || json.isBlank()) return;
      try {
         JsonObject data = JsonParser.parseString(json).getAsJsonObject();
         Gson gson = new Gson();
         Profile loadedDefault = gson.fromJson(data.get("default"), Profile.class);
         Map<String, Profile> loadedItems = new LinkedHashMap<>();
         for (var entry : data.getAsJsonObject("items").entrySet()) {
            Profile profile = gson.fromJson(entry.getValue(), Profile.class);
            if (profile != null && profile.item != null && profile.name != null && profile.attributes != null) loadedItems.put(entry.getKey(), profile);
         }
         if (loadedDefault == null) return;
         loadingProfile = true;
         try {
            profiles.clear();
            profiles.putAll(loadedItems);
            globalProfile = loadedDefault;
            refreshProfileChoices();
            String selected = profiles.containsKey(profileSelector.get()) ? profileSelector.get() : "General";
            profileSelector.set(selected);
            editingProfile = selected;
            loadEditor(profiles.getOrDefault(selected, globalProfile));
         } finally { loadingProfile = false; }
      } catch (RuntimeException ignored) {
         // An invalid optional profile does not prevent the base protection settings from loading.
      }
   }

   private boolean tridentDanger() {
      Box danger = mc.player.getBoundingBox().expand(4);
      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof TridentEntity trident) || trident.getOwner() == mc.player) continue;
         Vec3d position = trident.getEntityPos();
         Vec3d velocity = trident.getVelocity();
         for (int step = 0; step < 60 && velocity.lengthSquared() > .0001; step++) {
            Vec3d next = position.add(velocity);
            if (danger.contains(position) || danger.raycast(position, next).isPresent()) return true;
            var hit = mc.world.raycast(new RaycastContext(position, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, trident));
            if (hit.getType() != HitResult.Type.MISS) break;
            position = next;
            velocity = velocity.multiply(.99).add(0, -.05, 0);
         }
      }
      return false;
   }
}
