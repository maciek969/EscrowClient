package elexis.modules.impl.player;

import com.mojang.authlib.GameProfile;
import elexis.client.events.impl.entity.EventAttack;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@FunctionAdd(name = "FakePlayer", alias = "FakePlayer", category = Category.Player, description = "Spawns a client-side fake player")
@Environment(EnvType.CLIENT)
public class FakePlayer extends Function {
    private static final int FAKE_ID = -745812;
    private static final float MAX_HEALTH = 20.0F;
    private static final float DAMAGE_PER_HIT = 4.0F;
    private static final long HIT_COOLDOWN_MS = 220L;
    private static final long RESPAWN_DELAY_MS = 2000L;

    private OtherClientPlayerEntity fake;
    private Vec3d velocity = Vec3d.ZERO;
    private long respawnAt;
    private long lastHitAt;
    private boolean waitingRespawn;
    private float fakeHealth = MAX_HEALTH;
    private double spawnY;

    @Override
    public void onEnable() {
        super.onEnable();
        spawnFake();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        removeFake();
        resetState();
    }

    @EventHandler
    public void onAttack(EventAttack event) {
        if (fake == null || event.getTarget() != fake || waitingRespawn || mc.player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastHitAt < HIT_COOLDOWN_MS) {
            return;
        }
        lastHitAt = now;

        Vec3d direction = new Vec3d(fake.getX() - mc.player.getX(), 0.0D, fake.getZ() - mc.player.getZ());
        if (direction.lengthSquared() < 0.001D) {
            direction = Vec3d.fromPolar(0.0F, mc.player.getYaw());
        }
        direction = direction.normalize();

        double strength = mc.player.isSprinting() ? 0.95D : 0.72D;
        velocity = new Vec3d(direction.x * strength, 0.36D, direction.z * strength);
        fake.setVelocity(velocity);
        fake.animateDamage(0.0F);
        fakeHealth = Math.max(0.0F, fakeHealth - DAMAGE_PER_HIT);
        fake.setHealth(fakeHealth);
        fake.playSound(SoundEvents.ENTITY_PLAYER_HURT, 1.0F, 1.0F);

        if (fakeHealth <= 0.0F) {
            killFake(now);
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (nullCheck()) {
            removeFake();
            resetState();
            return;
        }

        long now = System.currentTimeMillis();
        if (fake != null && fake.getEntityWorld() != mc.world) {
            removeFake();
            resetState();
        }
        if (fake == null) {
            if (!waitingRespawn || now >= respawnAt) {
                waitingRespawn = false;
                spawnFake();
            }
            return;
        }

        if (velocity.lengthSquared() > 0.0004D) {
            double nextY = fake.getY() + velocity.y;
            if (nextY < spawnY) {
                nextY = spawnY;
                velocity = new Vec3d(velocity.x, 0.0D, velocity.z);
            }
            fake.setPosition(fake.getX() + velocity.x, nextY, fake.getZ() + velocity.z);
            velocity = new Vec3d(velocity.x * 0.82D, Math.max(velocity.y - 0.055D, -0.50D), velocity.z * 0.82D);
        } else {
            velocity = Vec3d.ZERO;
        }
    }

    private void killFake(long now) {
        if (fake == null) {
            return;
        }
        fake.setHealth(0.0F);
        fake.playSound(SoundEvents.ENTITY_PLAYER_DEATH, 1.0F, 1.0F);
        removeFake();
        waitingRespawn = true;
        respawnAt = now + RESPAWN_DELAY_MS;
        velocity = Vec3d.ZERO;
    }

    private void spawnFake() {
        if (mc.world == null || mc.player == null || fake != null) {
            return;
        }

        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("ElexisFakePlayer".getBytes(StandardCharsets.UTF_8)), "FakePlayer", mc.player.getGameProfile().properties());

        OtherClientPlayerEntity player = new OtherClientPlayerEntity(mc.world, profile);
        Vec3d forward = Vec3d.fromPolar(0.0F, mc.player.getYaw()).normalize();
        Vec3d pos = position(mc.player).add(forward.multiply(3.0D));
        spawnY = mc.player.getY();

        player.setId(FAKE_ID);
        player.copyPositionAndRotation(mc.player);
        player.setPosition(pos.x, spawnY, pos.z);
        player.setYaw(mc.player.getYaw() + 180.0F);
        player.setPitch(0.0F);
        fakeHealth = MAX_HEALTH;
        player.setHealth(fakeHealth);
        equipArmor(player);
        mc.world.addEntity(player);
        fake = player;
        velocity = Vec3d.ZERO;
        lastHitAt = 0L;
    }

    private void equipArmor(OtherClientPlayerEntity player) {
        player.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        player.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        player.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
    }

    private Vec3d position(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    private void removeFake() {
        if (fake != null) {
            fake.remove(Entity.RemovalReason.DISCARDED);
            fake = null;
        }
    }

    private void resetState() {
        velocity = Vec3d.ZERO;
        respawnAt = 0L;
        lastHitAt = 0L;
        waitingRespawn = false;
        fakeHealth = MAX_HEALTH;
        spawnY = 0.0D;
    }
}
