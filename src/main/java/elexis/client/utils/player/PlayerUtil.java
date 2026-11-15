package elexis.client.utils.player;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.util.math.MathHelper;

import java.net.SocketAddress;

public final class PlayerUtil implements IMinecraft {
    private PlayerUtil() {
    }

    public static float calculateCorrectYawOffset(float yaw) {
        double xDiff = mc.player.getX() - mc.player.lastRenderX;
        double zDiff = mc.player.getZ() - mc.player.lastRenderZ;
        float distSquared = (float) (xDiff * xDiff + zDiff * zDiff);
        float renderYawOffset = mc.player.lastBodyYaw;
        float offset = renderYawOffset;

        if (distSquared > 0.0025000002F) {
            offset = (float) MathHelper.atan2(zDiff, xDiff) * 180.0F / (float) Math.PI - 90.0F;
        }

        if (mc.player.handSwingProgress > 0.0F) {
            offset = yaw;
        }

        float yawOffsetDiff = MathHelper.wrapDegrees(yaw - (renderYawOffset + MathHelper.wrapDegrees(offset - renderYawOffset) * 0.3F));
        yawOffsetDiff = MathHelper.clamp(yawOffsetDiff, -32.0F, 32.0F);
        renderYawOffset = yaw - yawOffsetDiff;
        if (yawOffsetDiff * yawOffsetDiff > 2500.0F) {
            renderYawOffset += yawOffsetDiff * 0.2F;
        }

        return renderYawOffset;
    }

    public static boolean isServerContains(String searchString) {
        if (searchString == null || searchString.isEmpty() || mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return false;
        }

        String serverAddress = null;
        if (mc.getCurrentServerEntry() != null) {
            serverAddress = mc.getCurrentServerEntry().address;
        }

        if ((serverAddress == null || serverAddress.isEmpty()) && mc.getNetworkHandler().getConnection() != null) {
            SocketAddress address = mc.getNetworkHandler().getConnection().getAddress();
            if (address != null) {
                serverAddress = address.toString();
                if (serverAddress.startsWith("/")) {
                    serverAddress = serverAddress.substring(1);
                }
            }
        }

        if (mc.isConnectedToLocalServer()) {
            serverAddress = "localhost";
        }

        return serverAddress != null && serverAddress.toLowerCase().contains(searchString.toLowerCase());
    }
}
