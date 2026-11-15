package elexis.modules.api;


import elexis.Elexis;
import elexis.client.events.impl.client.EventKey;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.impl.combat.AntiBot;
import elexis.modules.impl.combat.AutoExplosion;
import elexis.modules.impl.combat.AutoTotem;
import elexis.modules.impl.combat.BowSpammer;
import elexis.modules.impl.combat.BowAim;
import elexis.modules.impl.combat.AimBot;
import elexis.modules.impl.combat.TriggerBot;
import elexis.modules.impl.combat.HitAura;
import elexis.modules.impl.combat.NoVelocity;
import elexis.modules.impl.combat.ShiftTap;
import elexis.modules.impl.combat.CrystalAura;
import elexis.modules.impl.combat.AntiCrystal;
import elexis.modules.impl.combat.Criticals;
import elexis.modules.impl.combat.TargetPearl;
import elexis.modules.impl.combat.AutoTrap;
import elexis.modules.impl.combat.AutoWeb;
import elexis.modules.impl.player.AutoTool;
import elexis.modules.impl.misc.HWHelper;
import elexis.modules.impl.misc.ItemScroller;
import elexis.modules.impl.misc.NameProtect;
import elexis.modules.impl.misc.RWHelper;
import elexis.modules.impl.misc.ScoreboardHealth;
import elexis.modules.impl.movement.*;
import elexis.modules.impl.player.AutoSwap;
import elexis.modules.impl.player.AutoArmor;
import elexis.modules.impl.player.ChestStealer;
import elexis.modules.impl.player.ChestLoker;
import elexis.modules.impl.player.ClickPearl;
import elexis.modules.impl.player.ElytraSwap;
import elexis.modules.impl.player.EventTracker;
import elexis.modules.impl.player.NoDelay;
import elexis.modules.impl.player.NoServerRotate;
import elexis.modules.impl.player.PlayerUtilsFunction;
import lombok.Getter;
import net.minecraft.client.util.InputUtil;
import elexis.modules.impl.render.*;
import elexis.modules.impl.utils.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Getter
public class FunctionManager {
    final List<Function> functions = new CopyOnWriteArrayList<>();
    private final java.util.Map<String, Function> functionsByName = new java.util.HashMap<>();


    //COMBAT
    HitAura hitAura;
    AntiBot antiBot;
    AutoTotem autoTotem;
    CrystalAura crystalAura;
    AntiCrystal antiCrystal;
    Criticals criticals;
    TargetPearl targetPearl;
    AutoTrap autoTrap;
    AutoWeb autoWeb;
    BowSpammer bowSpammer;
    BowAim bowAim;
    AimBot aimBot;
    TriggerBot triggerBot;
    AutoExplosion autoExplosion;
    NoVelocity noVelocity;
    ShiftTap shiftTap;

    //MOVEMENT
    AutoSprint autoSprint;
    Speed speed;
    NoSlow noSlow;
    GuiMove guiMove;
    Spider spider;
    AirStuck airStuck;
    Fly fly;
    NoWeb noWeb;
    ElytraBooster elytraBooster;
    ElytraMotion elytraMotion;
    ElytraResolver elytraResolver;
    NoFallDamage noFallDamage;
    Strafe strafe;

    //PLAYER
    PlayerUtilsFunction playerUtilsFunction;
    EventTracker eventTracker;
    ElytraSwap elytraSwap;
    NoDelay noDelay;
    AutoSwap autoSwap;
    AutoArmor autoArmor;
    AutoTool autoTool;
    ChestStealer chestStealer;
    ChestLoker chestLoker;
    ClickPearl clickPearl;
    NoServerRotate noServerRotate;
//RENDER
    Interface hudInterface;
    ItemScroller itemScroller;
    HWHelper hwHelper;
    RWHelper rwHelper;
    ScoreboardHealth scoreboardHealth;
    NameProtect nameProtect;
    AppleGhost appleGhost;
    Chams chams;
    DamagePop damagePop;
    DashTrail dashTrail;
    CameraTweaks cameraTweaks;
    NoRneder noRender;
    CustomWorld customWorld;
    FullBright fullBright;
    Gamma gamma;
    Themes themes;
    Wings wings;
    Predictions predictions;
    Trails trails;
    Particles particles;
    TargetESP targetESP;
    NameTags nameTags;
    CustomCrystal customCrystal;
    ViewModel viewModel;
    FireLook fireLook;
    NoPlayer noPlayer;
    CameraSmothAI cameraSmothAI;
    JumpCircle jumpCircle;
    Arrows arrows;
    Hands hands;
    HitShot hitShot;
    elexis.modules.impl.render.ItemReplacer itemReplacer;
    ChinaHat chinaHat;
    KillEffect killEffect;
    SeeInvisible seeInvisible;
    ItemPhysic itemPhysic;
    Crosshair crosshair;
    BabyBoy babyBoy;
    CustomSwing customSwing;
    elexis.modules.impl.misc.ServerHelper serverHelper;
    elexis.modules.impl.render.BoxFly boxFly;
    elexis.modules.impl.render.Invoker invoker;
    elexis.modules.impl.render.ChinaHatCustom chinaHatCustom;


    //UTILS
    UnHook unHook;
    BetterGameplay betterGameplay;
    AutoDuel autoDuel;
    AutoJoin autoJoin;
    AutoAccept autoAccept;
    FuntimeHelper funtimeHelper;
    Globals globals;
    Optimizer optimizer;
    ClientSounds clientSounds;
    XCarry xCarry;
    WindJump windJump;
    SoundsRemove soundsRemove;
    ShulkerBypass shulkerBypass;
    AutoTpAccept autoTpAccept;
    AutoAuth autoAuth;
    ShulkerView shulkerView;


    public void init() {
        registerAll(
                betterGameplay = new BetterGameplay(),
                autoSprint = new AutoSprint(),
                speed = new Speed(),
                noSlow = new NoSlow(),
                guiMove = new GuiMove(),
                spider = new Spider(),
                airStuck = new AirStuck(),
                fly = new Fly(),
                noWeb = new NoWeb(),
                elytraBooster = new ElytraBooster(),
                elytraMotion = new ElytraMotion(),
                elytraResolver = new ElytraResolver(),
                noFallDamage = new NoFallDamage(),
                strafe = new Strafe(),
                unHook = new UnHook(),
                autoDuel = new AutoDuel(),
                autoJoin = new AutoJoin(),
                autoAccept = new AutoAccept(),
                funtimeHelper = new FuntimeHelper(),
                hudInterface = new Interface(),
                itemScroller = new ItemScroller(),
                hwHelper = new HWHelper(),
                rwHelper = new RWHelper(),
                scoreboardHealth = new ScoreboardHealth(),
                nameProtect = new NameProtect(),
                appleGhost = new AppleGhost(),
                chams = new Chams(),
                damagePop = new DamagePop(),
                dashTrail = new DashTrail(),
                cameraTweaks = new CameraTweaks(),
                playerUtilsFunction = new PlayerUtilsFunction(),
                eventTracker = new EventTracker(),
                elytraSwap = new ElytraSwap(),
                noDelay = new NoDelay(),
                autoSwap = new AutoSwap(),
                autoArmor = new AutoArmor(),
                autoTool = new AutoTool(),
                chestStealer = new ChestStealer(),
                chestLoker = new ChestLoker(),
                clickPearl = new ClickPearl(),
                noServerRotate = new NoServerRotate(),
                hitAura = new HitAura(),
                antiBot = new AntiBot(),
                autoTotem = new AutoTotem(),
                crystalAura = new CrystalAura(),
                antiCrystal = new AntiCrystal(),
                criticals = new Criticals(),
                targetPearl = new TargetPearl(),
                autoTrap = new AutoTrap(),
                autoWeb = new AutoWeb(),
                bowSpammer = new BowSpammer(),
                bowAim = new BowAim(),
                aimBot = new AimBot(),
                triggerBot = new TriggerBot(),
                autoExplosion = new AutoExplosion(),
                noVelocity = new NoVelocity(),
                shiftTap = new ShiftTap(),
                noRender = new NoRneder(),
                customWorld = new CustomWorld(),
                fullBright = new FullBright(),
                gamma = new Gamma(),
                themes = new Themes(),
                wings = new Wings(),
                predictions = new Predictions(),
                trails = new Trails(),
                particles = new Particles(),
                targetESP = new TargetESP(),
                nameTags = new NameTags(),
                customCrystal = new CustomCrystal(),
                viewModel = new ViewModel(),
                fireLook = new FireLook(),
                cameraSmothAI = new CameraSmothAI(),
                jumpCircle = new JumpCircle(),
                arrows = new Arrows(),
                hands = new Hands(),
                hitShot = new HitShot(),
                itemReplacer = new elexis.modules.impl.render.ItemReplacer(),
                chinaHat = new ChinaHat(),
                killEffect = new KillEffect(),
                seeInvisible = new SeeInvisible(),
                itemPhysic = new ItemPhysic(),
                crosshair = new Crosshair(),
                customSwing = new CustomSwing(),
                babyBoy = new BabyBoy(),
                serverHelper = new elexis.modules.impl.misc.ServerHelper(),
                boxFly = new elexis.modules.impl.render.BoxFly(),
                invoker = new elexis.modules.impl.render.Invoker(),
                chinaHatCustom = new elexis.modules.impl.render.ChinaHatCustom(),
                noPlayer = new NoPlayer(),
                globals = new Globals(),
                optimizer = new Optimizer(),
                clientSounds = new ClientSounds(),
                xCarry = new XCarry(),
                windJump = new WindJump(),
                soundsRemove = new SoundsRemove(),
                shulkerBypass = new ShulkerBypass(),
                autoTpAccept = new AutoTpAccept(),
                autoAuth = new AutoAuth(),
                shulkerView = new ShulkerView()
        );

        registerAll(elexis.modules.impl.render.visual.VisualImports.create());
        hudInterface.setState(true);
        functions.sort(Comparator.comparing(Function::getAlias));
        Elexis.getEventBus().subscribe(this);
        elexis.modules.impl.render.hud.HudRenderUtil.registerTargetTracker();
    }

    private void registerAll(Function... features) {
        Arrays.sort(features, Comparator.comparing(Function::getAlias));
        this.functions.addAll(List.of(features));
        for (Function feature : features) functionsByName.put(feature.getName(), feature);
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (UnHook.unhooked) return;

        boolean sharedCombatBind = aimBot != null && triggerBot != null
                && aimBot.getBind() != 0 && aimBot.getBind() != -1
                && aimBot.getBind() == triggerBot.getBind();
        if (sharedCombatBind && e.screen() == null && e.isKeyDown(aimBot.getBind())) {
            // One shared key must not keep swapping an enabled module with a disabled one.
            boolean enable = !aimBot.isState() || !triggerBot.isState();
            aimBot.setState(enable);
            triggerBot.setState(enable);
        }

        if (e.type() == InputUtil.Type.MOUSE) {
            if (e.getAction() == 1) {
                for (Function function : functions) {
                    if (sharedCombatBind && (function == aimBot || function == triggerBot)) continue;
                    if (e.matchesBind(function.getBind())) {
                        function.toggle();
                    }
                }
            }
        } else {
            if (e.getKey() == -1) return;
            for (Function function : functions) {
                if (sharedCombatBind && (function == aimBot || function == triggerBot)) continue;
                if (function.getBind() > 0 && e.getAction() == 1 && e.isKeyDown(function.getBind())) {
                    function.toggle();
                }
            }
        }
    }

    public List<Function> getFunctionsByCategory(Category category) {
        return functions.stream()
                .filter(function -> function.getCategory() == category)
                .collect(Collectors.toList());
    }

    public Function getFunctionByName(String name) {
        if (name == null || name.isEmpty()) return null;
        Function exact = functionsByName.get(name);
        if (exact != null) return exact;

        String lower = name.toLowerCase(Locale.US);
        lower = switch (lower) {
            case "hud" -> "interface";
            case "itemphysics" -> "item physic";
            case "prediction" -> "predictions";
            case "removals" -> "norender";
            case "swinganimations" -> "swinganimation";
            case "killeffects" -> "killeffect";
            case "hitwawe" -> "hitwave";
            default -> lower;
        };
        String lookup = lower;
        for (Function function : functions) if (function.getName().equalsIgnoreCase(lookup)) return function;
        return null;
    }
}



