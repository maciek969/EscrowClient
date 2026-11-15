package elexis.rpc;

import elexis.client.utils.client.IMinecraft;
import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.net.SocketAddress;
import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
public class RPC implements IMinecraft {
    private static final String APPLICATION_ID = "1500800489703346347";
    private static final int CLIENT_UID = ThreadLocalRandom.current().nextInt(1, 1001);

    private static DiscordRpc rpc;
    private static boolean started;
    private static Thread presenceThread;
    private static long rpcStartTimestampSeconds;

    private static boolean isRunningFromJar() {
        try {
            String path = RPC.class.getResource(RPC.class.getSimpleName() + ".class").toString();
            return path.startsWith("jar:");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public void startRpc() {
        if (started) {
            return;
        }

        rpc = new DiscordRpc();
        rpc.setDebugMode(false);

        RPCEventHandler handler = new RPCEventHandler() {
            @Override
            public void ready(User user) {
                System.out.println("[Elexis] Discord RPC connected as " + user.getUsername());
                updatePresenceSafely();
            }

            @Override
            public void disconnected(ErrorCode errorCode, String message) {
                System.err.println("[Elexis] Discord RPC disconnected: " + errorCode + " - " + message);
            }

            @Override
            public void errored(ErrorCode errorCode, String message) {
                System.err.println("[Elexis] Discord RPC error: " + errorCode + " - " + message);
            }
        };

        try {
            rpc.init(APPLICATION_ID, handler, false);
            started = true;
            rpcStartTimestampSeconds = System.currentTimeMillis() / 1000L;
            startPresenceThread();
        } catch (Throwable throwable) {
            System.err.println("[Elexis] Failed to init Discord RPC: " + throwable.getMessage());
            rpc = null;
            started = false;
        }
    }

    private static void startPresenceThread() {
        if (presenceThread != null) {
            return;
        }

        presenceThread = new Thread(() -> {
            while (started && rpc != null) {
                updatePresenceSafely();

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
        }, "Elexis-DiscordRPC");
        presenceThread.setDaemon(true);
        presenceThread.start();
    }

    private static void updatePresenceSafely() {
        try {
            updatePresence();
        } catch (Throwable throwable) {
            System.err.println("[Elexis] Discord RPC update failed: " + throwable.getMessage());
        }
    }

    private static void updatePresence() {
        if (rpc == null) {
            return;
        }

        boolean beta = isRunningFromJar();
        String details = beta ? "Role: Beta" : "Role: Developer";
        String state = getGameStateText();

        DiscordRichPresence presence = DiscordRichPresence.builder()
                .details(details)
                .state(state)
                .largeImageKey("https://i.postimg.cc/Y9D4Ksg4/Chat-GPT-Image-24-maa-2026-g-20-38-48.png")
                .largeImageText("")
                .smallImageKey("Elexis")
                .smallImageText("Elexis 1.21.11")
                .startTimestamp(rpcStartTimestampSeconds)
                .button(DiscordRichPresence.RPCButton.of("Discord", "https://discord.gg/WjhC9FMCKE"))
                .build();

        rpc.updatePresence(presence);
    }

    private static String getGameStateText() {
        try {
            if (mc == null || mc.player == null || mc.world == null) {
                return "Menu | UID: " + CLIENT_UID;
            }

            if (mc.isConnectedToLocalServer()) {
                return "Singleplayer | UID: " + CLIENT_UID;
            }

            String serverAddress = null;
            if (mc.getCurrentServerEntry() != null) {
                serverAddress = mc.getCurrentServerEntry().address;
            }

            if ((serverAddress == null || serverAddress.isEmpty()) && mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
                SocketAddress address = mc.getNetworkHandler().getConnection().getAddress();
                if (address != null) {
                    serverAddress = address.toString();
                    if (serverAddress.startsWith("/")) {
                        serverAddress = serverAddress.substring(1);
                    }
                }
            }

            if (serverAddress == null || serverAddress.isEmpty()) {
                return "Singleplayer | UID: " + CLIENT_UID;
            }

            return serverAddress + " | UID: " + CLIENT_UID;
        } catch (Throwable ignored) {
            return "Menu | UID: " + CLIENT_UID;
        }
    }

    public void stopRpc() {
        if (rpc != null) {
            rpc.shutdown();
            rpc = null;
        }

        started = false;

        if (presenceThread != null) {
            try {
                presenceThread.interrupt();
            } catch (Throwable ignored) {
            }
            presenceThread = null;
        }
    }
}
