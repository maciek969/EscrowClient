package elexis.modules.impl.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elexis.Elexis;
import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.client.utils.render.main.oldrender.system.IconUse;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.Theme;
import elexis.modules.api.settings.impl.*;
import elexis.modules.impl.misc.NameProtect;
import elexis.modules.impl.render.hud.HudItemIcons;
import elexis.modules.impl.render.hud.HudRenderUtil;
import elexis.modules.impl.render.visual.VisualGeometry;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Atlases;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@FunctionAdd(name = "Interface", alias = "Interface", category = Category.Render, description = "New Elexis HUD. Open chat to move elements")
public final class Interface extends Function {
    private static final String WATERMARK_LOGO = "\uE000";
    private static final String WATERMARK_NAME = "Elexis Client";
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Pattern STAFF = Pattern.compile("(?iu)\\b(admin|mod|moder|helper|staff|админ|модер|хелпер|стажёр|куратор)");
    private static final int WHITE = 0xFFF5F2FC, MUTED = 0xFFB7ADC8;
    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);
    private static final long NOTIFICATION_ANIMATION = 200_000_000L;
    private final SliderSetting theme = new SliderSetting("Saturation", .45f, 0, 1, .01f);
    public final SliderSetting size = new SliderSetting("UI size", 1, .5f, 2, .05f);
    public final MultiBooleanSetting elements = new MultiBooleanSetting("Elements", new BooleanSetting("Watermark", true),
            new BooleanSetting("Target", true), new BooleanSetting("Binds", true), new BooleanSetting("Staff", true),
            new BooleanSetting("Effects", true), new BooleanSetting("Item binds", true), new BooleanSetting("Cooldowns", true),
            new BooleanSetting("Armor", true), new BooleanSetting("Totems", true), new BooleanSetting("Notifications", true),
            new BooleanSetting("Arraylist", true));
    private final ModeSetting targetStyle = new ModeSetting("Target style", "Elexis", "Elexis", "Default");
    private final ModeSetting armorStyle = new ModeSetting("Armor orientation", "Horizontal", "Horizontal", "Vertical");
    private final ModeSetting totemStyle = new ModeSetting("Totem style", "Counter", "Counter", "Row", "Column");
    private final BooleanSetting listVisuals = new BooleanSetting("Visuals in arraylist", false);
    private final BooleanSetting coordinates = new BooleanSetting("Coordinates", true);
    private final BooleanSetting speed = new BooleanSetting("Speed", true);
    private final BooleanSetting serverTps = new BooleanSetting("TPS", true);
    private final Map<String, Anchor> positions = new LinkedHashMap<>();
    private final LinkedHashMap<String, Rect> bounds = new LinkedHashMap<>();
    private final ArrayDeque<Notice> notices = new ArrayDeque<>();
    private final List<Row> cooldowns = new ArrayList<>();
    private final Map<Identifier, ItemStack> knownCooldownItems = new HashMap<>();
    private final StringSetting layout = new StringSetting("HUD positions", "") {
        @Override public String get() { return saveLayout(); }
        @Override public void set(String json) { super.set(json); loadLayout(json); }
    };
    private ClientWorld world;
    private LivingEntity displayedTarget;
    private float health, trailingHealth, absorption, targetAlpha;
    private long previousFrame, lastTimePacket, lastTimeTick;
    private double tps = 20;
    private float speedSample;
    private String dragged;
    private float dragOffsetX, dragOffsetY, screenWidth, screenHeight, scale;
    private boolean mouseDown;
    private record Anchor(float x, float y) {}
    private record Rect(float x, float y, float w, float h) {
        boolean contains(float px, float py) { return px >= x && px < x+w && py >= y && py < y+h; }
    }
    private record Row(String title, String value, int color, ItemStack item, Identifier sprite) {
        Row(String title, String value, int color) { this(title,value,color,ItemStack.EMPTY,null); }
        boolean hasIcon() { return !item.isEmpty() || sprite != null; }
    }
    private record Notice(Text text, int color, long start, long end, ItemStack item) {}

    public Interface() {
        layout.setVisible(() -> false);
        targetStyle.setVisible(() -> elements.get("Target"));
        armorStyle.setVisible(() -> elements.get("Armor"));
        totemStyle.setVisible(() -> elements.get("Totems"));
        listVisuals.setVisible(() -> elements.get("Arraylist"));
        coordinates.setVisible(() -> elements.get("Watermark"));
        speed.setVisible(() -> elements.get("Watermark"));
        serverTps.setVisible(() -> elements.get("Watermark"));
        addSettings(theme, size, elements, targetStyle, armorStyle, totemStyle, listVisuals,
                coordinates, speed, serverTps, new ButtonSetting("Reset positions", positions::clear), layout);
    }

    public static Interface active() {
        var manager = Elexis.getFunctionManager();
        if (manager == null) return null;
        var module = manager.getHudInterface();
        return module != null && module.isState() ? module : null;
    }

    public static void notify(Text message, int color, long duration) { notify(message, color, duration, ItemStack.EMPTY); }

    public static void notify(Text message, int color, long duration, ItemStack item) {
        if (!mc.isOnThread()) {
            mc.execute(() -> notify(message, color, duration, item));
            return;
        }
        var module = active();
        if (module == null || !module.elements.get("Notifications")) return;
        long now = System.nanoTime();
        if (module.notices.size() >= 8) module.notices.removeFirst();
        module.notices.addLast(new Notice(message, color, now, now + duration * 1_000_000L, item));
    }

    @EventHandler public void tick(EventUpdate.Fast event) {
        if (world != mc.world) { clear(); world = mc.world; }
        if (nullCheck()) return;
        float bps = (float)Math.hypot(mc.player.getX()-mc.player.lastX, mc.player.getZ()-mc.player.lastZ)*20;
        speedSample = MathHelper.lerp(.22f, speedSample, bps);
        if (elements.get("Cooldowns")) updateCooldowns();
        else cooldowns.clear();
        notices.removeIf(n -> System.nanoTime() >= n.end);
    }

    @EventHandler public void packet(EventReceivePacket event) {
        if (!(event.getPacket() instanceof WorldTimeUpdateS2CPacket time)) return;
        long now = System.nanoTime();
        var connection = mc.getNetworkHandler();
        mc.execute(() -> {
            if (!isState() || connection != mc.getNetworkHandler() || mc.world == null) return;
            if (lastTimePacket != 0 && time.time() > lastTimeTick && time.time()-lastTimeTick <= 100) {
                double sample = Math.clamp((time.time()-lastTimeTick)*1e9/(now-lastTimePacket), 0, 20);
                tps = tps * .7 + sample * .3;
            }
            lastTimePacket = now;
            lastTimeTick = time.time();
        });
    }

    private void updateCooldowns() {
        var manager = mc.player.getItemCooldownManager();
        cooldowns.clear();
        if (manager.entries.isEmpty()) { knownCooldownItems.clear(); return; }
        knownCooldownItems.keySet().retainAll(manager.entries.keySet());
        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && manager.isCoolingDown(stack)) knownCooldownItems.putIfAbsent(manager.getGroup(stack), stack.copy());
        }
        for (var entry : manager.entries.entrySet()) {
            int ticks = entry.getValue().endTick() - manager.tick;
            if (ticks <= 0 || cooldowns.size() >= 8) continue;
            Identifier group = entry.getKey();
            var item = knownCooldownItems.computeIfAbsent(group, id -> Registries.ITEM.containsId(id)
                    ? new ItemStack(Registries.ITEM.get(id)) : ItemStack.EMPTY);
            String name = item.isEmpty() ? group.getPath().replace('_', ' ') : item.getName().getString();
            cooldowns.add(new Row(name, String.format(Locale.ROOT,"%.1fs",ticks/20f), MUTED, item, null));
        }
    }

    public LivingEntity target() {
        LivingEntity target = HudRenderUtil.currentHudTarget();
        return target == null && mc.currentScreen instanceof ChatScreen ? mc.player : target;
    }

    public void collectItems(Consumer<ItemStack> add) {
        if (nullCheck()) return;
        if (elements.get("Target")) {
            var target = target();
            if (target == null) target = displayedTarget;
            if (target != null) equipment(target).forEach(add);
        }
        if (elements.get("Armor")) for (var slot : ARMOR) add.accept(mc.player.getEquippedStack(slot));
        if (elements.get("Totems")) add.accept(TOTEM);
        if (elements.get("Cooldowns")) for (var row : cooldowns) add.accept(row.item);
        if (elements.get("Notifications")) for (var notice : notices) add.accept(notice.item);
    }

    private static List<ItemStack> equipment(LivingEntity entity) {
        return List.of(entity.getEquippedStack(ARMOR[0]), entity.getEquippedStack(ARMOR[1]), entity.getEquippedStack(ARMOR[2]),
                entity.getEquippedStack(ARMOR[3]), entity.getMainHandStack(), entity.getOffHandStack());
    }

    @EventHandler public void render(EventRender.Screen.Hud event) {
        if (nullCheck() || elexis.ui.gui.ClickGui.class.isInstance(mc.currentScreen)) return;
        Renderer2D r = event.getRenderer();
        scale = Math.max(.5f, (float)mc.getWindow().getScaleFactor() * size.get() * .82f);
        screenWidth = event.getViewportWidth() / scale;
        screenHeight = event.getViewportHeight() / scale;
        boolean editor = mc.currentScreen instanceof ChatScreen;
        if (!editor) { dragged = null; mouseDown = false; }
        if (dragged != null) updateDrag();
        bounds.clear();
        long now = System.nanoTime();
        float dt = previousFrame == 0 ? .016f : Math.clamp((now-previousFrame)*1e-9f, .001f, .1f);
        previousFrame = now;
        r.pushScale(scale);
        try {
            if (elements.get("Watermark")) watermark(r);
            if (elements.get("Arraylist")) array(r, editor);
            float left = below("watermark2", below("watermark", 8));
            float right = below("arraylist", 8);
            if (elements.get("Binds")) list(r, "binds", "Binds", IconUse.INFO, binds(), editor, false, left);
            left = below("binds", left);
            if (elements.get("Item binds")) list(r, "itembinds", "Item binds", IconUse.INFO, itemBinds(), editor, false, left);
            if (elements.get("Cooldowns")) list(r, "cooldowns", "Cooldowns", IconUse.INFO, cooldowns, editor, false, below("itembinds", left));
            if (elements.get("Staff")) list(r, "staff", "Staff", IconUse.INFO, staff(), editor, true, right);
            if (elements.get("Effects")) list(r, "effects", "Effects", IconUse.POTION, effects(), editor, true, below("staff", right));
            if (elements.get("Armor")) armor(r, editor);
            if (elements.get("Totems")) totems(r, editor);
            if (elements.get("Target")) target(r, dt);
            if (elements.get("Notifications")) notifications(r, now, editor);
        } finally { r.popScale(); }
    }

    private void watermark(Renderer2D r) {
        var handler = mc.getNetworkHandler();
        var entry = handler == null ? null : handler.getPlayerListEntry(mc.player.getUuid());
        String ping = entry == null ? "0 ms" : entry.getLatency() + " ms";
        String stats = mc.getCurrentFps() + " FPS  ·  " + ping;
        float statsWidth = Math.max(width(r, stats, 10), width(r, "000 FPS  ·  000 ms", 10));
        float logoSize = 18;
        float nameOffset = 8 + r.measureText(FontRegistry.WATERMARK_ICON, WATERMARK_LOGO, logoSize).width + 5;
        float statsOffset = nameOffset + width(r, WATERMARK_NAME, 13) + 12;
        Rect p = place("watermark", statsOffset + statsWidth + 8, 24, 8, 8);
        card(r,p);
        r.text(FontRegistry.WATERMARK_ICON,p.x+8,p.y+12,logoSize,WATERMARK_LOGO,WHITE);
        label(r,WATERMARK_NAME,p.x+nameOffset,p.y+12,13,accent());
        label(r,stats,p.x+statsOffset,p.y+12,10,WHITE);
        var info = new ArrayList<String>();
        var protect = NameProtect.active();
        if (coordinates.get() && (protect == null || !protect.hideCoordinates()))
            info.add("XYZ  " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ());
        if (speed.get()) info.add(String.format(Locale.ROOT,"%.1f BPS",speedSample));
        if (serverTps.get()) info.add(String.format(Locale.ROOT,"%.1f TPS",tps));
        if (info.isEmpty()) return;
        String text = String.join("  ·  ",info);
        float w = 16+width(r,text,10);
        Rect q = place("watermark2",w,24,p.x+(p.w-w)*.5f,p.y+p.h+6);
        card(r,q);
        label(r,text,q.x+8,q.y+12,10,WHITE);
    }

    private void target(Renderer2D r, float dt) {
        LivingEntity target = target();
        float factor = 1-(float)Math.exp(-dt*12);
        targetAlpha = MathHelper.lerp(factor,targetAlpha,target == null ? 0 : 1);
        if (target != null) {
            float maximum = Math.max(1,target.getMaxHealth());
            float hp = Math.clamp(target.getHealth()/maximum,0,1);
            float shield = Math.max(0,target.getAbsorptionAmount()/maximum);
            if (target != displayedTarget) {
                displayedTarget = target;
                health = trailingHealth = hp;
                absorption = shield;
            }
            health = MathHelper.lerp(factor,health,hp);
            trailingHealth = MathHelper.lerp(1-(float)Math.exp(-dt*3),trailingHealth,hp);
            absorption = MathHelper.lerp(factor,absorption,shield);
        }
        if (displayedTarget == null || target == null && targetAlpha < .015f || displayedTarget.getEntityWorld() != mc.world) {
            displayedTarget = null;
            return;
        }
        boolean javelin = targetStyle.is("Javelin");
        String name = NameProtect.display(displayedTarget.getName()).getString();
        String hp = targetHealthLabel(displayedTarget);
        float w = 50 + Math.min(195, Math.max(width(r,name,12),width(r,hp,10)));
        var items = javelin ? equipment(displayedTarget).stream().filter(stack -> !stack.isEmpty()).toList() : List.<ItemStack>of();
        float iconSize = items.isEmpty() ? 0 : Math.min(15, (w-14-(items.size()-1))/items.size());
        float iconRise = items.isEmpty() ? 0 : iconSize+3;
        Rect p = place("targethud",w,44,(screenWidth-w)/2,screenHeight*.65f,iconRise);
        r.pushAlpha(targetAlpha);
        try {
            card(r,p);
            r.rect(p.x+6,p.y+6,32,32,5,0xFF2D2540);
            face(r,displayedTarget,p.x+6,p.y+6);
            float x = p.x+44, bar = p.w-50;
            label(r,shorten(r,name,bar,12),x,p.y+13,12,WHITE);
            label(r,hp,x,p.y+26,10,MUTED);
            r.rect(x,p.y+35,bar,2.5f,1.2f,0xFF352D44);
            float unit = bar / (1 + absorption);
            if (trailingHealth > .001f) r.rect(x,p.y+35,unit*trailingHealth,2.5f,1.2f,0xFF855C86);
            if (health > .001f) r.rect(x,p.y+35,unit*health,2.5f,1.2f,accent());
            if (absorption > .001f) r.rect(x+unit*health,p.y+35,unit*absorption,2.5f,1.2f,0xFFFFC562);
            if (!items.isEmpty()) {
                x = p.x+7;
                for (ItemStack stack : items) {
                    HudItemIcons.draw(r,stack,x,p.y-iconRise,iconSize);
                    x += iconSize+1;
                }
            }
        } finally { r.popAlpha(); }
    }

    private static String targetHealthLabel(LivingEntity target) {
        return String.format(Locale.ROOT, "%.1f HP", target.getHealth() + target.getAbsorptionAmount());
    }

    private static void face(Renderer2D r, LivingEntity entity, float x, float y) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;
        var texture = mc.getTextureManager().getTexture(player.getSkin().body().texturePath()).getGlTextureView();
        if (!(texture instanceof GlTextureView gl)) return;
        int tint = entity.hurtTime > 0 ? 0xFFFFB5B1 : WHITE;
        r.drawRgbaTextureRegionRounded(gl.texture().getGlId(),x,y,32,32,.125f,.125f,.25f,.25f,5,tint);
        r.drawRgbaTextureRegionRounded(gl.texture().getGlId(),x,y,32,32,.625f,.125f,.75f,.25f,5,tint);
    }

    private static List<Row> binds() {
        return Elexis.getFunctionManager().getFunctions().stream().filter(f -> f.isState() && f.getBind()!=0 && f.getBind()!=-1)
                .limit(12).map(f -> new Row(f.getAlias(),bind(f.getBind()),WHITE)).toList();
    }

    private List<Row> staff() {
        var network = mc.getNetworkHandler();
        if (network == null) return List.of();
        var storage = Elexis.getInstance().getStaffStorage();
        var names = new HashSet<String>();
        var rows = new ArrayList<Row>();
        for (var entry : network.getPlayerList()) {
            String name = entry.getProfile().name();
            var team = entry.getScoreboardTeam();
            if (!storage.isStaff(name) && (team == null || !STAFF.matcher(team.getPrefix().getString()).find())) continue;
            names.add(name);
            boolean hidden = entry.getGameMode() == net.minecraft.world.GameMode.SPECTATOR;
            rows.add(new Row(NameProtect.display(name),hidden ? "Vanish" : "Online",hidden ? 0xFFEEC075 : 0xFF7FE9AA));
        }
        for (var team : mc.world.getScoreboard().getTeams()) {
            boolean prefix = STAFF.matcher(team.getPrefix().getString()).find();
            for (String name : team.getPlayerList()) {
                if (name.matches("[A-Za-z0-9_]{3,16}") && !names.contains(name) && (prefix || storage.isStaff(name))) {
                    rows.add(new Row(NameProtect.display(name),"Vanish",0xFFEEC075));
                }
            }
        }
        return rows.stream().sorted(Comparator.comparing(Row::title)).limit(10).toList();
    }

    private List<Row> effects() {
        return mc.player.getStatusEffects().stream().limit(10).map(effect -> new Row(
                effect.getEffectType().value().getName().getString() + (effect.getAmplifier() > 0 ? " " + (effect.getAmplifier()+1) : ""),
                effect.isInfinite() ? "∞" : HudRenderUtil.formatDuration(effect.getDuration()),
                0xFF000000 | effect.getEffectType().value().getColor(), ItemStack.EMPTY,
                InGameHud.getEffectTexture(effect.getEffectType()))).toList();
    }

    private static List<Row> itemBinds() {
        var manager = Elexis.getFunctionManager();
        var rows = new ArrayList<Row>();
        var hw = manager.getHwHelper();
        var rw = manager.getRwHelper();
        if (hw.isState()) {
            addBind(rows,"Explosive trap",hw.trapKey);
            addBind(rows,"Snowball",hw.snowKey);
            addBind(rows,"Stun",hw.stanKey);
            addBind(rows,"Explosive gadget",hw.explosionKey);
        }
        if (rw.isState()) {
            addBind(rows,"Anti Fly",rw.antiFlyKey);
            addBind(rows,"XP scroll",rw.scrollKey);
            addBind(rows,"Trap",rw.trapKey);
        }
        return rows;
    }

    private static void addBind(List<Row> rows, String name, BindSetting setting) {
        if (setting.get()!=0 && setting.get()!=-1) rows.add(new Row(name,bind(setting.get()),WHITE));
    }

    private static String bind(int key) {
        if (key < 0) return "M" + (-key);
        String name = InputUtil.Type.KEYSYM.createFromCode(key).getTranslationKey();
        return name.replace("key.keyboard.","").toUpperCase(Locale.ROOT);
    }

    private void list(Renderer2D r, String id, String title, IconUse icon, List<Row> data, boolean editor, boolean right, float y) {
        if (data.isEmpty() && !editor) return;
        List<Row> rows = data.isEmpty() ? List.of(new Row("No data","",MUTED)) : data;
        float w = Math.max(103,36+width(r,title,11));
        for (Row row : rows) w = Math.max(w,26+width(r,row.title,10)+width(r,row.value,9)+(row.hasIcon()?16:0));
        w = Math.min(w,246);
        Rect p = place(id,w,24+rows.size()*15,right ? screenWidth-w-8 : 8,y);
        card(r,p);
        header(r,p,title,icon);
        float rowY = p.y+29;
        for (Row row : rows) {
            float textX = p.x+8;
            if (row.hasIcon()) {
                if (row.sprite != null) drawSprite(r, row.sprite, textX, rowY-7);
                else HudItemIcons.draw(r,row.item,textX,rowY-7,13);
                textX+=17;
            }
            float valueX = p.x+p.w-8-width(r,row.value,9);
            label(r,shorten(r,row.title,valueX-textX-5,10),textX,rowY,10,WHITE);
            label(r,row.value,valueX,rowY,9,row.color);
            rowY+=15;
        }
    }

    private static void drawSprite(Renderer2D renderer, Identifier id, float x, float y) {
        var atlas = mc.getAtlasManager().getAtlasTexture(Atlases.GUI);
        var sprite = atlas.getSprite(id);
        if (atlas.getGlTextureView() instanceof GlTextureView view && sprite != atlas.getMissingSprite()) {
            renderer.drawRgbaTextureRegion(view.texture().getGlId(), x, y, 13, 13,
                    sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), 0xFFFFFFFF);
        }
    }

    private void armor(Renderer2D r, boolean editor) {
        var stacks = Arrays.stream(ARMOR).map(mc.player::getEquippedStack).toList();
        if (!editor && stacks.stream().allMatch(ItemStack::isEmpty)) return;
        boolean vertical = armorStyle.is("Vertical");
        float w = vertical ? 78 : 108, h = vertical ? 24+4*23 : 55;
        Rect p = place("armor",w,h,screenWidth-w-8,screenHeight-h-42);
        card(r,p);
        header(r,p,"Armor",IconUse.INFO);
        for (int i=0;i<4;i++) {
            ItemStack stack = stacks.get(i);
            float x=p.x+4+(vertical?0:i*25), y=p.y+22+(vertical?i*23:0);
            r.rect(x,y,24,vertical?21:29,3,0x262A2141);
            HudItemIcons.draw(r,stack,x+3,y,18);
            String percent = stack.isEmpty() ? "–" : stack.isDamageable()
                    ? Math.round(100*(1-stack.getDamage()/(float)stack.getMaxDamage()))+"%" : "100%";
            label(r,percent,vertical?x+32:x+(24-width(r,percent,8))/2,vertical?y+10:y+23,8,MUTED);
        }
    }

    private void totems(Renderer2D r, boolean editor) {
        int total = 0;
        var inventory = mc.player.getInventory();
        for (int i=0;i<inventory.size();i++) if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) total+=inventory.getStack(i).getCount();
        if (total == 0 && !editor) return;
        boolean row = !totemStyle.is("Counter"), vertical = totemStyle.is("Column");
        int shown = row ? Math.min(13,Math.max(1,total)) : 1;
        float w = vertical ? 31 : row ? 12+shown*16 : 48;
        float h = vertical ? 9+shown*16 : 29;
        Rect armor = positions.containsKey("armor") ? null : bounds.get("armor");
        Rect p = place("totemcounter",w,h,screenWidth-w-8,(armor == null ? screenHeight-42 : armor.y-6)-h);
        card(r,p);
        for(int i=0;i<shown;i++) HudItemIcons.draw(r,TOTEM,p.x+5+(vertical?0:i*16),p.y+4+(vertical?i*16:0),20);
        if (!row) label(r,Integer.toString(total),p.x+28,p.y+14,11,WHITE);
        else if (total>shown) label(r,"+"+(total-shown),p.x+4,p.y+p.h-5,8,WHITE);
    }

    private void array(Renderer2D r, boolean editor) {
        var active = Elexis.getFunctionManager().getFunctions().stream().filter(f -> f!=this && f.isState() && (listVisuals.get() || f.getCategory()!=Category.Render))
                .map(Function::getAlias).sorted(Comparator.comparingDouble((String name)->width(r,name,10)).reversed()).limit(16).toList();
        if (active.isEmpty() && !editor) return;
        List<String> names = active.isEmpty() ? List.of("Arraylist") : active;
        float w = 18+width(r,names.getFirst(),10);
        Rect p = place("arraylist",w,names.size()*16,screenWidth-w-8,8);
        for (int i=0;i<names.size();i++) {
            String name=names.get(i);
            float row = width(r,name,10)+15;
            float x=p.x+p.w-row, y=p.y+i*16;
            card(r,new Rect(x,y,row,15));
            r.rect(x+row-2,y+2,1,11,.5f,accent());
            label(r,name,x+6,y+7.5f,10,WHITE);
        }
    }

    private void notifications(Renderer2D r, long now, boolean editor) {
        if (notices.isEmpty() && !editor) return;
        List<Notice> visible = notices.isEmpty() ? List.of(new Notice(Text.literal("Notifications"),accent(),
                now-NOTIFICATION_ANIMATION,now+NOTIFICATION_ANIMATION,ItemStack.EMPTY)) : List.copyOf(notices);
        float w = 140;
        for (Notice n : visible) w = Math.max(w,35+width(r,NameProtect.display(n.text).getString(),11));
        w = Math.min(295,w);
        Rect p=place("notifications",w,visible.size()*25,(screenWidth-w)/2,screenHeight*.37f);
        float y=p.y;
        for (Notice notice:visible) {
            float a=Math.clamp(Math.min(now-notice.start,notice.end-now)/(float)NOTIFICATION_ANIMATION,0,1);
            r.pushAlpha(a);
            try {
                Rect row=new Rect(p.x+(1-a)*9,y,p.w,22);
                card(r,row);
                if (notice.item.isEmpty()) icon(r,IconUse.INFO,row.x+8,y+11,notice.color);
                else HudItemIcons.draw(r,notice.item,row.x+4,y+3,16);
                label(r,shorten(r,NameProtect.display(notice.text).getString(),row.w-28,11),row.x+24,y+11,11,WHITE);
            } finally { r.popAlpha(); }
            y+=25;
        }
    }

    private Rect place(String id, float w, float h, float defaultX, float defaultY) {
        return place(id,w,h,defaultX,defaultY,0);
    }

    private Rect place(String id, float w, float h, float defaultX, float defaultY, float topInset) {
        Anchor anchor = positions.get(id);
        float x = anchor == null ? defaultX : anchor.x*screenWidth;
        float y = anchor == null ? defaultY : anchor.y*screenHeight;
        float maxY = Math.max(0,screenHeight-h);
        Rect result = new Rect(Math.clamp(x,0,Math.max(0,screenWidth-w)),Math.clamp(y,Math.min(topInset,maxY),maxY),w,h);
        bounds.put(id,result);
        return result;
    }

    private float below(String id, float fallback) {
        Rect panel = positions.containsKey(id) ? null : bounds.get(id);
        return panel == null ? fallback : panel.y+panel.h+6;
    }

    private void card(Renderer2D r, Rect p) {
        int accent = accent();
        float tint = MathHelper.clamp(theme.get(), 0, 1);
        float radius = Math.min(6, p.h * .3f);
        int topLeft = 0xFF000000 | mix(0xFF282A36, accent, tint * .22f);
        int topRight = 0xFF000000 | mix(0xFF20222D, accent, tint * .12f);
        int bottomLeft = 0xFF000000 | mix(0xFF171923, accent, tint * .08f);
        int bottomRight = 0xFF000000 | mix(0xFF12141D, accent, tint * .04f);
        int rimTop = 0xFF000000 | mix(0xFF505261, accent, .2f + tint * .2f);
        int rimBottom = 0xFF000000 | mix(0xFF292B38, accent, tint * .12f);

        r.shadow(p.x,p.y+1,p.w,p.h,radius,5,.5f,0x70060910);
        r.gradient(p.x,p.y,p.w,p.h,radius,rimTop,rimTop,rimBottom,rimBottom);
        r.gradient(p.x+.75f,p.y+.75f,p.w-1.5f,p.h-1.5f,radius-.75f,
                topLeft,topRight,bottomRight,bottomLeft);
        r.gradient(p.x+8,p.y+1,p.w-16,.6f,.3f,0x006C6280,0x406C6280,0x406C6280,0x006C6280);
    }

    private void header(Renderer2D r, Rect p, String title, IconUse icon) {
        icon(r,icon,p.x+7,p.y+10,accent());
        label(r,title,p.x+23,p.y+10,11,WHITE);
        int highlight = 0x40000000 | mix(WHITE,accent(),.35f);
        int faded = highlight & 0xFFFFFF;
        r.gradient(p.x+7,p.y+20,p.w-14,.65f,0,highlight,highlight,faded,faded);
    }

    private static int accent() {
        Theme palette = ThemeColorUtils.activeTheme();
        if (palette == Theme.RAINBOW) {
            float hue = Math.floorMod(System.nanoTime(), 8_000_000_000L) / 8_000_000_000f;
            return java.awt.Color.HSBtoRGB(hue, .8f, 1f);
        }
        return 0xFF000000 | palette.getMain().getRGB() & 0xFFFFFF;
    }
    private static int mix(int a,int b,float amount) {
        int r=(int)MathHelper.lerp(amount,a>>16&255,b>>16&255);
        int g=(int)MathHelper.lerp(amount,a>>8&255,b>>8&255);
        int bl=(int)MathHelper.lerp(amount,a&255,b&255);
        return r<<16 | g<<8 | bl;
    }
    private static float width(Renderer2D r,String text,float size) { return r.measureText(FontRegistry.SF_MEDIUM,text,size).width; }
    private static String shorten(Renderer2D r,String text,float width,float size) {
        if (width(r,text,size)<=width) return text;
        int end=text.length();
        while (end>0 && width(r,text.substring(0,end)+"…",size)>width) end--;
        return text.substring(0,end)+"…";
    }
    private static void label(Renderer2D r,String text,float x,float center,float size,int color) {
        var font=FontRegistry.SF_MEDIUM;
        r.text(font,x,HudRenderUtil.centeredBaseline(font,'A',size,center),size,text,color);
    }
    private static void icon(Renderer2D r,IconUse icon,float x,float y,int color) {
        var font=FontRegistry.ICONS;
        r.text(font,x,HudRenderUtil.centeredBaseline(font,'A',10,y),10,icon.glyph,color);
    }

    public boolean mouse(int button, int action) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !(mc.currentScreen instanceof ChatScreen) || scale <= 0) return false;
        if (action == GLFW.GLFW_RELEASE && mouseDown) {
            if (dragged != null) updateDrag();
            mouseDown=false;
            dragged=null;
            return true;
        }
        if (action != GLFW.GLFW_PRESS) return false;
        float x=mouseX(),y=mouseY();
        for (var entry : bounds.reversed().entrySet()) if (entry.getValue().contains(x,y)) {
            Rect p=entry.getValue();
            dragged=entry.getKey();
            dragOffsetX=x-p.x;
            dragOffsetY=y-p.y;
            mouseDown=true;
            return true;
        }
        return false;
    }

    private float mouseX() { return (float)(mc.mouse.getX()*mc.getWindow().getFramebufferWidth()/mc.getWindow().getWidth()/scale); }
    private float mouseY() { return (float)(mc.mouse.getY()*mc.getWindow().getFramebufferHeight()/mc.getWindow().getHeight()/scale); }
    private void updateDrag() {
        Rect p=bounds.get(dragged);
        if (p==null || screenWidth<=0 || screenHeight<=0) return;
        float x=Math.clamp(mouseX()-dragOffsetX,0,Math.max(0,screenWidth-p.w));
        float y=Math.clamp(mouseY()-dragOffsetY,0,Math.max(0,screenHeight-p.h));
        if (Math.abs(x-(screenWidth-p.w)/2)<5) x=(screenWidth-p.w)/2;
        if (Math.abs(y-(screenHeight-p.h)/2)<5) y=(screenHeight-p.h)/2;
        positions.put(dragged,new Anchor(x/screenWidth,y/screenHeight));
    }

    private String saveLayout() {
        JsonObject root=new JsonObject();
        for (var entry:positions.entrySet()) {
            JsonArray coords=new JsonArray();
            coords.add(entry.getValue().x); coords.add(entry.getValue().y);
            root.add(entry.getKey(),coords);
        }
        return root.toString();
    }

    private void loadLayout(String json) {
        positions.clear();
        if (json==null || json.isBlank()) return;
        try {
            for (var entry:JsonParser.parseString(json).getAsJsonObject().entrySet()) {
                var a=entry.getValue().getAsJsonArray();
                float x=a.get(0).getAsFloat(), y=a.get(1).getAsFloat();
                if (Float.isFinite(x) && Float.isFinite(y)) positions.put(entry.getKey(),new Anchor(Math.clamp(x,0,1),Math.clamp(y,0,1)));
            }
        } catch (RuntimeException ignored) {}
    }

    private void clear() {
        displayedTarget=null;
        targetAlpha=0;
        previousFrame=lastTimePacket=lastTimeTick=0;
        tps=20;
        speedSample=0;
        notices.clear();
        cooldowns.clear();
        knownCooldownItems.clear();
        bounds.clear();
        dragged=null;
        mouseDown=false;
        world=null;
    }

    @Override public void onDisable() { clear(); super.onDisable(); }
}
