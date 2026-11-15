package elexis.modules.impl.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@FunctionAdd(name = "Globals", alias = "Globals", category = Category.Utilities, description = "Shows Elexis users")
public class Globals extends Function {
    public static final Identifier TAG_ICON = Identifier.of("elexis", "textures/hud/globals.png");

    private static final String CLIENT_VERSION = "1.4.3";
    private static final int PORT = 13599;
    private static final int UPDATE_RATE_TICKS = 30;

    private final Map<UUID, Boolean> isClientUserCache = new ConcurrentHashMap<>();
    private final Set<String> pendingLookups = ConcurrentHashMap.newKeySet();

    private ClientApiBridge clientApi;
    private String playerName;
    private boolean connectionWarningShown;
    private int lastRegistrationTick = Integer.MIN_VALUE;

    @Override
    public void onEnable() {
        super.onEnable();
        EmbeddedApiServer.ensureStarted(PORT);
        this.clientApi = new ClientApiBridge(CLIENT_VERSION, PORT);
        this.isClientUserCache.clear();
        this.pendingLookups.clear();
        this.connectionWarningShown = false;
        this.lastRegistrationTick = Integer.MIN_VALUE;
        this.ensureLocalPlayerRegistered();
        this.sendStatusMessage(true);
    }

    @Override
    public void onDisable() {
        this.clear();
        super.onDisable();
        this.sendStatusMessage(false);
    }

    @EventHandler
    public void onTick(EventUpdate.Slow event) {
        if (mc == null || this.clientApi == null) {
            return;
        }

        if (mc.player == null || mc.world == null) {
            this.removePlayer();
            this.isClientUserCache.clear();
            this.pendingLookups.clear();
            this.connectionWarningShown = false;
            this.lastRegistrationTick = Integer.MIN_VALUE;
            return;
        }

        this.ensureLocalPlayerRegistered();

        if (!this.connectionWarningShown
                && mc.player.age >= UPDATE_RATE_TICKS * 3
                && !this.clientApi.hasSuccessfulConnection()) {
            this.connectionWarningShown = true;
            this.sendApiUnavailableMessage();
        }

        if (mc.player.age % UPDATE_RATE_TICKS != 0) {
            return;
        }

        Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) {
                continue;
            }

            UUID uuid = player.getUuid();
            activePlayers.add(uuid);

            if (mc.player.getUuid().equals(uuid)) {
                this.isClientUserCache.put(uuid, true);
                continue;
            }

            String name = player.getName().getString();
            if (name == null || name.isBlank()) {
                continue;
            }

            String lookupKey = name.toLowerCase(Locale.ROOT);
            if (!this.pendingLookups.add(lookupKey)) {
                continue;
            }

            this.clientApi.isClientUserAsync(name, result -> {
                this.pendingLookups.remove(lookupKey);
                if (result) {
                    this.isClientUserCache.put(uuid, true);
                } else {
                    this.isClientUserCache.remove(uuid);
                }
            });
        }

        this.isClientUserCache.keySet().removeIf(uuid -> !activePlayers.contains(uuid));
    }

    public boolean shouldRenderClientIcon(PlayerEntity player) {
        return isState() && this.isClientUser(player);
    }

    public boolean isClientUser(PlayerEntity player) {
        if (player == null || !isState()) {
            return false;
        }

        if (mc.player != null && Objects.equals(mc.player.getUuid(), player.getUuid())) {
            return true;
        }

        return this.isClientUserCache.containsKey(player.getUuid());
    }

    public void clear() {
        this.removePlayer();
        if (this.clientApi != null) {
            this.clientApi.shutdown();
        }
        this.clientApi = null;
        this.isClientUserCache.clear();
        this.pendingLookups.clear();
        this.connectionWarningShown = false;
        this.lastRegistrationTick = Integer.MIN_VALUE;
    }

    private void ensureLocalPlayerRegistered() {
        if (this.clientApi == null || mc == null || mc.player == null) {
            return;
        }

        String currentName = mc.player.getName().getString();
        if (currentName == null || currentName.isBlank()) {
            return;
        }

        this.isClientUserCache.put(mc.player.getUuid(), true);

        int currentAge = mc.player.age;
        if (Objects.equals(this.playerName, currentName)
                && currentAge - this.lastRegistrationTick < UPDATE_RATE_TICKS) {
            return;
        }

        if (!Objects.equals(this.playerName, currentName)) {
            this.removePlayer();
            this.playerName = currentName;
        }

        this.lastRegistrationTick = currentAge;
        this.clientApi.addPlayer(currentName);
    }

    private void removePlayer() {
        if (this.clientApi != null && this.playerName != null && !this.playerName.isBlank()) {
            this.clientApi.removePlayer(this.playerName);
        }
        this.playerName = null;
        this.lastRegistrationTick = Integer.MIN_VALUE;
    }

    private void sendStatusMessage(boolean enabled) {
        if (mc == null || mc.player == null) {
            return;
        }

        MutableText message = Text.empty()
                .append(Text.literal("Elexis Client").formatted(Formatting.DARK_GRAY, Formatting.BOLD))
                .append(Text.literal(" > ").formatted(Formatting.GRAY))
                .append(Text.literal(enabled ? "Globals enabled" : "Globals disabled")
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED));

        mc.player.sendMessage(message, false);
    }

    private void sendApiUnavailableMessage() {
        if (mc == null || mc.player == null) {
            return;
        }

        MutableText message = Text.empty()
                .append(Text.literal("Elexis Client").formatted(Formatting.DARK_GRAY, Formatting.BOLD))
                .append(Text.literal(" > ").formatted(Formatting.GRAY))
                .append(Text.literal("Globals API unavailable: 127.0.0.1:" + PORT).formatted(Formatting.RED));

        mc.player.sendMessage(message, false);
    }

    private static final class ClientApiBridge {
        private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(1500L);

        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        private final String baseUrl;
        private final int port;
        private final String version;
        private volatile boolean successfulConnection;

        private volatile int addEndpointIndex = -1;
        private volatile int removeEndpointIndex = -1;
        private volatile int checkEndpointIndex = -1;
        private volatile boolean shutdown;

        private ClientApiBridge(String version, int port) {
            this.version = version;
            this.port = port;
            this.baseUrl = "http://127.0.0.1:" + port;
            EmbeddedApiServer.ensureStarted(port);
        }

        public void addPlayer(String name) {
            this.runAsync(() -> this.executeAction(ActionType.ADD, name));
        }

        public void removePlayer(String name) {
            this.runAsync(() -> this.executeAction(ActionType.REMOVE, name));
        }

        public void isClientUserAsync(String name, Consumer<Boolean> callback) {
            if (callback == null) {
                return;
            }

            if (this.shutdown || name == null || name.isBlank()) {
                callback.accept(false);
                return;
            }

            this.runAsync(() -> this.executeCheck(name), callback);
        }

        public void shutdown() {
            this.shutdown = true;
        }

        public boolean hasSuccessfulConnection() {
            return this.successfulConnection;
        }

        private void runAsync(Runnable task) {
            if (this.shutdown || task == null) {
                return;
            }

            CompletableFuture.runAsync(() -> {
                if (!this.shutdown) {
                    task.run();
                }
            }).exceptionally(throwable -> null);
        }

        private void runAsync(BooleanSupplier task, Consumer<Boolean> callback) {
            if (this.shutdown || task == null || callback == null) {
                return;
            }

            CompletableFuture.supplyAsync(() -> this.shutdown ? false : task.getAsBoolean())
                    .exceptionally(throwable -> false)
                    .thenAccept(callback);
        }

        private boolean executeAction(ActionType type, String name) {
            if (name == null || name.isBlank()) {
                return false;
            }

            List<EndpointDescriptor> candidates = this.buildActionCandidates(type, name);
            int cachedIndex = type == ActionType.ADD ? this.addEndpointIndex : this.removeEndpointIndex;
            if (isValidEndpointIndex(cachedIndex, candidates)) {
                ActionResult cachedResult = this.tryAction(candidates.get(cachedIndex));
                if (cachedResult.supported()) {
                    return cachedResult.success();
                }
                this.setEndpointIndex(type, -1);
            }

            for (int i = 0; i < candidates.size(); i++) {
                if (i == cachedIndex) {
                    continue;
                }

                ActionResult result = this.tryAction(candidates.get(i));
                if (!result.supported()) {
                    continue;
                }

                this.setEndpointIndex(type, i);
                return result.success();
            }

            return false;
        }

        private boolean executeCheck(String name) {
            if (name == null || name.isBlank()) {
                return false;
            }

            List<EndpointDescriptor> candidates = this.buildCheckCandidates(name);
            int cachedIndex = this.checkEndpointIndex;
            if (isValidEndpointIndex(cachedIndex, candidates)) {
                CheckResult cachedResult = this.tryCheck(candidates.get(cachedIndex));
                if (cachedResult.supported()) {
                    return cachedResult.value();
                }
                this.checkEndpointIndex = -1;
            }

            for (int i = 0; i < candidates.size(); i++) {
                if (i == cachedIndex) {
                    continue;
                }

                CheckResult result = this.tryCheck(candidates.get(i));
                if (!result.supported()) {
                    continue;
                }

                this.checkEndpointIndex = i;
                return result.value();
            }

            return false;
        }

        private List<EndpointDescriptor> buildActionCandidates(ActionType type, String name) {
            String encodedName = encode(name);
            String encodedVersion = encode(this.version);
            String jsonBodyWithoutVersion = "{\"name\":\"" + escapeJson(name) + "\"}";
            String jsonBody = "{\"name\":\"" + escapeJson(name) + "\",\"version\":\"" + escapeJson(this.version) + "\"}";
            String queryWithVersion = "?name=" + encodedName + "&version=" + encodedVersion;
            String queryWithoutVersion = "?name=" + encodedName;

            List<EndpointDescriptor> candidates = new ArrayList<>();
            if (type == ActionType.ADD) {
                addActionCandidates(candidates, HttpMethod.POST, "/addPlayer" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.POST, "/add" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.POST, "/player/add" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.POST, "/players/add" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.POST, "/addPlayer" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.POST, "/add" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.POST, "/player/add" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.POST, "/players/add" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.POST, "/addPlayer", jsonBody);
                addActionCandidates(candidates, HttpMethod.POST, "/add", jsonBody);
                addActionCandidates(candidates, HttpMethod.POST, "/player/add", jsonBody);
                addActionCandidates(candidates, HttpMethod.POST, "/players/add", jsonBody);
                addActionCandidates(candidates, HttpMethod.POST, "/addPlayer", jsonBodyWithoutVersion);
                addActionCandidates(candidates, HttpMethod.POST, "/add", jsonBodyWithoutVersion);
                addActionCandidates(candidates, HttpMethod.POST, "/player/add", jsonBodyWithoutVersion);
                addActionCandidates(candidates, HttpMethod.POST, "/players/add", jsonBodyWithoutVersion);
                addActionCandidates(candidates, HttpMethod.GET, "/addPlayer" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/add" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/player/add" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/players/add" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/addPlayer" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/add" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/player/add" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/players/add" + queryWithoutVersion, null);
            } else {
                addActionCandidates(candidates, HttpMethod.DELETE, "/removePlayer" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.DELETE, "/remove" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.DELETE, "/player/remove" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.DELETE, "/players/remove" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.DELETE, "/removePlayer" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.DELETE, "/remove" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.DELETE, "/player/remove" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.DELETE, "/players/remove" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.POST, "/removePlayer", jsonBody);
                addActionCandidates(candidates, HttpMethod.POST, "/remove", jsonBody);
                addActionCandidates(candidates, HttpMethod.POST, "/player/remove", jsonBody);
                addActionCandidates(candidates, HttpMethod.POST, "/players/remove", jsonBody);
                addActionCandidates(candidates, HttpMethod.POST, "/removePlayer", jsonBodyWithoutVersion);
                addActionCandidates(candidates, HttpMethod.POST, "/remove", jsonBodyWithoutVersion);
                addActionCandidates(candidates, HttpMethod.POST, "/player/remove", jsonBodyWithoutVersion);
                addActionCandidates(candidates, HttpMethod.POST, "/players/remove", jsonBodyWithoutVersion);
                addActionCandidates(candidates, HttpMethod.GET, "/removePlayer" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/remove" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/player/remove" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/players/remove" + queryWithVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/removePlayer" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/remove" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/player/remove" + queryWithoutVersion, null);
                addActionCandidates(candidates, HttpMethod.GET, "/players/remove" + queryWithoutVersion, null);
            }

            return candidates;
        }

        private List<EndpointDescriptor> buildCheckCandidates(String name) {
            String encodedName = encode(name);
            String encodedVersion = encode(this.version);
            String jsonBodyWithoutVersion = "{\"name\":\"" + escapeJson(name) + "\"}";
            String jsonBody = "{\"name\":\"" + escapeJson(name) + "\",\"version\":\"" + escapeJson(this.version) + "\"}";

            List<EndpointDescriptor> candidates = new ArrayList<>();
            String[] paths = new String[]{
                    "/isClientUser",
                    "/is-client-user",
                    "/check",
                    "/player/isClientUser",
                    "/player/is-client-user",
                    "/players/isClientUser",
                    "/players/is-client-user"
            };
            String[] params = new String[]{"name", "player", "nickname", "username"};

            for (String path : paths) {
                for (String param : params) {
                    String queryWithVersion = "?" + param + "=" + encodedName + "&version=" + encodedVersion;
                    String queryWithoutVersion = "?" + param + "=" + encodedName;
                    candidates.add(new EndpointDescriptor(HttpMethod.GET, path + queryWithVersion, null));
                    candidates.add(new EndpointDescriptor(HttpMethod.GET, path + queryWithoutVersion, null));
                }
                candidates.add(new EndpointDescriptor(HttpMethod.POST, path, jsonBody));
                candidates.add(new EndpointDescriptor(HttpMethod.POST, path, jsonBodyWithoutVersion));
            }

            return candidates;
        }

        private ActionResult tryAction(EndpointDescriptor endpoint) {
            HttpResponse<String> response = this.send(endpoint);
            if (response == null) {
                return ActionResult.unsupported();
            }

            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                return ActionResult.unsupported();
            }

            this.successfulConnection = true;
            return new ActionResult(true, true);
        }

        private CheckResult tryCheck(EndpointDescriptor endpoint) {
            HttpResponse<String> response = this.send(endpoint);
            if (response == null) {
                return CheckResult.unsupported();
            }

            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                return CheckResult.unsupported();
            }

            Boolean parsed = parseBoolean(response.body());
            if (parsed == null) {
                return CheckResult.unsupported();
            }

            this.successfulConnection = true;
            return new CheckResult(true, parsed);
        }

        private HttpResponse<String> send(EndpointDescriptor endpoint) {
            if (this.shutdown || endpoint == null) {
                return null;
            }

            HttpResponse<String> response = this.sendOnce(endpoint);
            if (response != null) {
                return response;
            }

            EmbeddedApiServer.ensureStarted(this.port);
            return this.sendOnce(endpoint);
        }

        private HttpResponse<String> sendOnce(EndpointDescriptor endpoint) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(this.baseUrl + endpoint.path()))
                        .timeout(REQUEST_TIMEOUT)
                        .header("Accept", "application/json");

                if (endpoint.method() == HttpMethod.POST) {
                    String body = endpoint.body() == null ? "" : endpoint.body();
                    builder.header("Content-Type", "application/json");
                    builder.POST(HttpRequest.BodyPublishers.ofString(body));
                } else if (endpoint.method() == HttpMethod.DELETE) {
                    builder.DELETE();
                } else {
                    builder.GET();
                }

                return this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {
                return null;
            }
        }

        private void setEndpointIndex(ActionType type, int endpointIndex) {
            if (type == ActionType.ADD) {
                this.addEndpointIndex = endpointIndex;
            } else {
                this.removeEndpointIndex = endpointIndex;
            }
        }

        private static void addActionCandidates(List<EndpointDescriptor> out, HttpMethod method, String path, String body) {
            out.add(new EndpointDescriptor(method, path, body));
        }

        private static boolean isValidEndpointIndex(int endpointIndex, List<EndpointDescriptor> candidates) {
            return endpointIndex >= 0 && endpointIndex < candidates.size();
        }

        private static Boolean parseBoolean(String body) {
            if (body == null) {
                return null;
            }

            String trimmed = body.trim();
            if (trimmed.isEmpty()) {
                return null;
            }

            if ("true".equalsIgnoreCase(trimmed)) {
                return true;
            }
            if ("false".equalsIgnoreCase(trimmed)) {
                return false;
            }

            String normalized = trimmed.toLowerCase(Locale.ROOT);
            if ("1".equals(normalized) || "\"1\"".equals(normalized)) {
                return true;
            }
            if ("0".equals(normalized) || "\"0\"".equals(normalized)) {
                return false;
            }

            if (normalized.contains("\"true\"") || normalized.contains(":true")) {
                return true;
            }
            if (normalized.contains("\"false\"") || normalized.contains(":false")) {
                return false;
            }

            try {
                JsonElement element = JsonParser.parseString(trimmed);
                return extractBoolean(element);
            } catch (Exception ignored) {
                return null;
            }
        }

        private static Boolean extractBoolean(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return null;
            }

            if (element.isJsonPrimitive()) {
                try {
                    return element.getAsBoolean();
                } catch (Exception ignored) {
                }

                String primitive = element.getAsString();
                if ("1".equals(primitive)) {
                    return true;
                }
                if ("0".equals(primitive)) {
                    return false;
                }
                if ("true".equalsIgnoreCase(primitive)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(primitive)) {
                    return false;
                }
                return null;
            }

            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                String[] keys = new String[]{"isClientUser", "clientUser", "result", "value", "exists", "data", "success"};
                for (String key : keys) {
                    if (!object.has(key)) {
                        continue;
                    }

                    Boolean extracted = extractBoolean(object.get(key));
                    if (extracted != null) {
                        return extracted;
                    }
                }
            }

            return null;
        }

        private static String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        private static String escapeJson(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    private static final class EmbeddedApiServer {
        private static final long ENTRY_TTL_MILLIS = 90_000L;
        private static final Map<String, Long> REGISTRY = new ConcurrentHashMap<>();
        private static volatile HttpServer server;

        private EmbeddedApiServer() {
        }

        public static synchronized boolean ensureStarted(int port) {
            if (server != null) {
                return true;
            }

            try {
                HttpServer httpServer = HttpServer.create(
                        new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port),
                        0
                );

                registerContext(httpServer, "/", EmbeddedApiServer::handleRoot);
                registerActionContexts(httpServer, ActionType.ADD,
                        "/addPlayer", "/add", "/player/add", "/players/add");
                registerActionContexts(httpServer, ActionType.REMOVE,
                        "/removePlayer", "/remove", "/player/remove", "/players/remove");
                registerCheckContexts(httpServer,
                        "/isClientUser", "/is-client-user", "/check",
                        "/player/isClientUser", "/player/is-client-user",
                        "/players/isClientUser", "/players/is-client-user");

                httpServer.setExecutor(Executors.newCachedThreadPool(task -> {
                    Thread thread = new Thread(task, "Elexis-Globals-API");
                    thread.setDaemon(true);
                    return thread;
                }));
                httpServer.start();
                server = httpServer;
                return true;
            } catch (BindException ignored) {
                return false;
            } catch (Exception ignored) {
                return false;
            }
        }

        private static void registerActionContexts(HttpServer server, ActionType type, String... paths) {
            for (String path : paths) {
                registerContext(server, path, exchange -> handleAction(exchange, type));
            }
        }

        private static void registerCheckContexts(HttpServer server, String... paths) {
            for (String path : paths) {
                registerContext(server, path, EmbeddedApiServer::handleCheck);
            }
        }

        private static void registerContext(HttpServer server, String path, ExchangeConsumer consumer) {
            server.createContext(path, exchange -> {
                try {
                    consumer.accept(exchange);
                } catch (Exception ignored) {
                    sendResponse(exchange, 500, "false");
                } finally {
                    exchange.close();
                }
            });
        }

        private static void handleRoot(HttpExchange exchange) {
            if (!isLoopback(exchange)) {
                sendResponse(exchange, 403, "forbidden");
                return;
            }

            cleanupExpired();
            sendResponse(exchange, 200, "{\"service\":\"Elexis-globals\",\"online\":true,\"players\":" + REGISTRY.size() + "}");
        }

        private static void handleAction(HttpExchange exchange, ActionType type) {
            if (!isLoopback(exchange)) {
                sendResponse(exchange, 403, "forbidden");
                return;
            }

            String name = extractName(exchange);
            if (name == null || name.isBlank()) {
                sendResponse(exchange, 400, "missing name");
                return;
            }

            cleanupExpired();
            if (type == ActionType.ADD) {
                REGISTRY.put(normalizeName(name), System.currentTimeMillis());
            } else {
                REGISTRY.remove(normalizeName(name));
            }

            sendResponse(exchange, 204, "");
        }

        private static void handleCheck(HttpExchange exchange) {
            if (!isLoopback(exchange)) {
                sendResponse(exchange, 403, "false");
                return;
            }

            String name = extractName(exchange);
            if (name == null || name.isBlank()) {
                sendResponse(exchange, 400, "false");
                return;
            }

            cleanupExpired();
            boolean exists = REGISTRY.containsKey(normalizeName(name));
            sendResponse(exchange, 200, Boolean.toString(exists));
        }

        private static boolean isLoopback(HttpExchange exchange) {
            if (exchange == null || exchange.getRemoteAddress() == null) {
                return false;
            }

            InetAddress address = exchange.getRemoteAddress().getAddress();
            return address != null && address.isLoopbackAddress();
        }

        private static String extractName(HttpExchange exchange) {
            Map<String, String> values = new HashMap<>();
            readQueryParameters(exchange, values);
            readJsonBody(exchange, values);

            for (String key : new String[]{"name", "player", "nickname", "username"}) {
                String value = values.get(key);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }

            return null;
        }

        private static void readQueryParameters(HttpExchange exchange, Map<String, String> values) {
            if (exchange == null || exchange.getRequestURI() == null) {
                return;
            }

            String query = exchange.getRequestURI().getRawQuery();
            if (query == null || query.isBlank()) {
                return;
            }

            for (String part : query.split("&")) {
                if (part.isBlank()) {
                    continue;
                }

                int equalsIndex = part.indexOf('=');
                String key = equalsIndex >= 0 ? part.substring(0, equalsIndex) : part;
                String value = equalsIndex >= 0 ? part.substring(equalsIndex + 1) : "";
                values.put(
                        decodeComponent(key).toLowerCase(Locale.ROOT),
                        decodeComponent(value)
                );
            }
        }

        private static void readJsonBody(HttpExchange exchange, Map<String, String> values) {
            if (exchange == null) {
                return;
            }

            try (InputStream inputStream = exchange.getRequestBody()) {
                byte[] bytes = inputStream.readAllBytes();
                if (bytes.length == 0) {
                    return;
                }

                String body = new String(bytes, StandardCharsets.UTF_8).trim();
                if (body.isEmpty()) {
                    return;
                }

                JsonElement element = JsonParser.parseString(body);
                if (!element.isJsonObject()) {
                    return;
                }

                JsonObject object = element.getAsJsonObject();
                for (String key : new String[]{"name", "player", "nickname", "username", "version"}) {
                    if (object.has(key) && !object.get(key).isJsonNull()) {
                        values.put(key, object.get(key).getAsString());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        private static void cleanupExpired() {
            long now = System.currentTimeMillis();
            REGISTRY.entrySet().removeIf(entry -> now - entry.getValue() > ENTRY_TTL_MILLIS);
        }

        private static String normalizeName(String name) {
            return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        }

        private static String decodeComponent(String value) {
            return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
        }

        private static void sendResponse(HttpExchange exchange, int status, String body) {
            if (exchange == null) {
                return;
            }

            byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            try {
                if (status == 204) {
                    exchange.sendResponseHeaders(status, -1);
                    return;
                }

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(bytes);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private enum ActionType {
        ADD,
        REMOVE
    }

    private enum HttpMethod {
        GET,
        POST,
        DELETE
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    private record EndpointDescriptor(HttpMethod method, String path, String body) {
    }

    private record ActionResult(boolean supported, boolean success) {
        private static ActionResult unsupported() {
            return new ActionResult(false, false);
        }
    }

    private record CheckResult(boolean supported, boolean value) {
        private static CheckResult unsupported() {
            return new CheckResult(false, false);
        }
    }

    @FunctionalInterface
    private interface ExchangeConsumer {
        void accept(HttpExchange exchange) throws Exception;
    }
}
