package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@FunctionAdd(name = "Spider", alias = "Spider", category = Category.Movement, description = "Wall climb")
@Environment(EnvType.CLIENT)
public class Spider extends Function {
    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "AresMine", "Grim", "CakeWorld");
    private final SliderSetting speed = new SliderSetting("Speed", 0.24F, 0.05F, 0.6F, 0.01F)
            .hidden(() -> !mode.is("Vanilla"));
    private final SliderSetting grimSpeed = new SliderSetting("Grim Speed", 0.18F, 0.08F, 0.32F, 0.01F)
            .hidden(() -> mode.is("Vanilla") || mode.is("CakeWorld"));
    private final SliderSetting cakeSpeed = new SliderSetting("Cake Speed", 0.24F, 0.12F, 0.42F, 0.01F)
            .hidden(() -> !mode.is("CakeWorld"));

    private int packetTicks;

    public Spider() {
        addSettings(mode, speed, grimSpeed, cakeSpeed);
    }

    @Override
    public void onDisable() {
        packetTicks = 0;
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck()) return;

        if (mode.is("Vanilla")) {
            if (mc.player.horizontalCollision) {
                mc.player.setVelocity(mc.player.getVelocity().x, speed.get(), mc.player.getVelocity().z);
            }
            return;
        }

        if (mode.is("CakeWorld")) {
            handleCakeWorld();
            return;
        }

        if (mc.player.horizontalCollision) {
            handlePacketSpider();
        } else {
            packetTicks = 0;
        }
    }

    // ── CakeWorld: check blocks above, build column without placing blocks ──────
    private void handleCakeWorld() {
        if (!mc.player.horizontalCollision) {
            packetTicks = 0;
            return;
        }

        boolean movingForward = mc.player.input != null
                && mc.player.input.getMovementInput().y > 0.0F;
        if (!movingForward) return;

        // Check that there is a solid block above to climb on
        BlockPos above = mc.player.getBlockPos().up();
        boolean hasBlockAbove = !mc.world.getBlockState(above).isAir();
        if (!hasBlockAbove) return;

        mc.player.fallDistance = 0.0F;
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x, Math.max(vel.y, cakeSpeed.get()), vel.z);

        if (mc.player.isOnGround()) {
            mc.player.jump();
        }

        // Fake ground packet: simulate placing blocks under without actually placing
        if (mc.getNetworkHandler() != null) {
            packetTicks++;
            if (packetTicks % 2 == 0) {
                double px = mc.player.getX();
                double py = mc.player.getY();
                double pz = mc.player.getZ();
                // claim onGround=true one block higher (simulate block under)
                sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px, py + 1.0D, pz, true, false));
                // then real position
                sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(px, py, pz, false, mc.player.horizontalCollision));
            }
        }
    }

    // ── AresMine / Grim packet spider ─────────────────────────────────────
    private void handlePacketSpider() {
        Vec3d velocity = mc.player.getVelocity();
        double climb = mode.is("Grim")
                ? grimSpeed.get()
                : Math.max(0.11D, grimSpeed.get() * 0.75D);
        mc.player.setVelocity(velocity.x, climb, velocity.z);
        mc.player.fallDistance = 0.0F;

        if (mc.getNetworkHandler() == null) return;

        packetTicks++;
        if (packetTicks % 5 != 0) return;

        double packetY = mc.player.getY() + (mode.is("Grim") ? 0.0625D : 0.03125D);
        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), packetY, mc.player.getZ(),
                false, mc.player.horizontalCollision));
    }
}
