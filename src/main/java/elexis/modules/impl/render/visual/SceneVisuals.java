package elexis.modules.impl.render.visual;

import elexis.Elexis;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import net.minecraft.particle.ParticleTypes;
import java.util.Random;

public final class SceneVisuals {
    private SceneVisuals() {}
    public static <T extends Function> T active(String name,Class<T> type) {
        if(Elexis.getFunctionManager()==null) return null;
        Function f=Elexis.getFunctionManager().getFunctionByName(name);
        return type.isInstance(f) && f.isState()?type.cast(f):null;
    }

    @FunctionAdd(name="Saturation",alias="Saturation",category=Category.Render,description="World color grading")
    public static final class Saturation extends Function {
        public final SliderSetting strength=new SliderSetting("Strength",1f,0f,1f,0.05f);
        public final SliderSetting exposure=new SliderSetting("Exposure",0f,-2f,2f,0.05f);
        public final SliderSetting contrast=new SliderSetting("Contrast",1f,0.5f,2f,0.05f);
        public final SliderSetting saturation=new SliderSetting("Saturation",1.2f,0f,3f,0.05f);
        public final SliderSetting gamma=new SliderSetting("Gamma",1f,0.5f,2f,0.05f);
        public final SliderSetting temperature=new SliderSetting("Temperature",0f,-1f,1f,0.05f);
        public final SliderSetting vignette=new SliderSetting("Vignette",0f,0f,1f,0.05f);
        public Saturation() { addSettings(strength,exposure,contrast,saturation,gamma,temperature,vignette); }
    }

    @FunctionAdd(name="MotionBlur",alias="Motion Blur",category=Category.Render,description="Camera motion blur by scene depth")
    public static final class MotionBlur extends Function {
        public final SliderSetting strength=new SliderSetting("Strength",0.35f,0.05f,1f,0.05f);
        public MotionBlur() { addSettings(strength); }
    }

    @FunctionAdd(name="FogAtmosphere",alias="Fog Atmosphere",category=Category.Render,description="Volumetric fog based on depth and height")
    public static final class FogAtmosphere extends Function {
        public final SliderSetting density=new SliderSetting("Density",0.025f,0.001f,0.1f,0.001f);
        public final SliderSetting height=new SliderSetting("Height",70f,-64f,320f,1f);
        public final ColorSetting color=new ColorSetting("Color",0xFFAAA1D6);
        public FogAtmosphere() { addSettings(density,height,color); }
    }

    @FunctionAdd(name="ShaderMist",alias="Shader Mist",category=Category.Render,description="Animated Pulsar sky")
    public static final class ShaderMist extends Function {
        public final SliderSetting speed=new SliderSetting("Speed",1f,0.1f,3f,0.1f);
        public final SliderSetting intensity=new SliderSetting("Intensity",0.6f,0f,1f,0.05f);
        public final ColorSetting color=new ColorSetting("Color",0xFF9164FF);
        public ShaderMist() { addSettings(speed,intensity,color); }
    }

    @FunctionAdd(name="Rain",alias="Rain",category=Category.Render,description="Wet surfaces and screen-space reflections")
    public static final class Rain extends Function {
        public final BooleanSetting customRain=new BooleanSetting("Custom rain",true);
        public final SliderSetting density=new SliderSetting("Rain density",1f,0.1f,2f,0.1f);
        public final BooleanSetting sound=new BooleanSetting("Rain sounds",true);
        public final SliderSetting volume=new SliderSetting("Rain volume",0.75f,0f,1f,0.05f);
        public final SliderSetting strength=new SliderSetting("Reflections",0.8f,0f,1f,0.05f);
        public final SliderSetting wetness=new SliderSetting("Wetness",0.85f,0f,1f,0.05f);
        public final BooleanSetting puddles=new BooleanSetting("Puddles",true);
        public final SliderSetting puddleAmount=new SliderSetting("Puddle amount",0.75f,0.1f,1f,0.05f);
        public final SliderSetting ripples=new SliderSetting("Ripples",0.65f,0f,1f,0.05f);
        public final ModeSetting quality=new ModeSetting("Quality","Balanced","Fast","Balanced","High");
        public final BooleanSetting rainOnly=new BooleanSetting("Only while raining",false);
        public final RainWeather weather=new RainWeather(this);
        public Rain() { addSettings(customRain,density,sound,volume,strength,wetness,puddles,puddleAmount,ripples,quality,rainOnly); }
        @EventHandler public void update(EventUpdate.Fast event) {weather.tick();}
        @EventHandler public void render(elexis.client.events.impl.render.EventRender.World event) {weather.render(event);}
        @Override public void onDisable() {weather.clear();super.onDisable();}
    }

    @FunctionAdd(name="Beautiful",alias="Beautiful",category=Category.Render,description="Wet world, reflections and rain particles")
    public static final class Beautiful extends Function {
        public final SliderSetting reflections=new SliderSetting("Reflections",0.3f,0f,1f,0.05f);
        private final Random random=new Random();
        public Beautiful() { addSettings(reflections); }
        @EventHandler public void update(EventUpdate.Slow event) {
            if(nullCheck() || !mc.world.isRaining()) return;
            for(int i=0;i<3;i++) {
                var pos=mc.player.getEntityPos().add((random.nextDouble()*2-1)*5,random.nextDouble()*3,(random.nextDouble()*2-1)*5);
                mc.world.addParticleClient(ParticleTypes.FALLING_WATER,pos.x,pos.y,pos.z,0,-0.15,0);
            }
        }
    }

    @FunctionAdd(name="Ambience",alias="Ambience",category=Category.Render,description="Local weather, time and atmospheric particles")
    public static final class Ambience extends Function {
        public final ModeSetting weather=new ModeSetting("Weather","Rain","Rain","Snow","Clear","Thunder");
        public final SliderSetting time=new SliderSetting("Time",18000f,0f,24000f,100f);
        public final BooleanSetting changeTime=new BooleanSetting("Change time",false);
        private final Random random=new Random();
        public Ambience() { addSettings(weather,changeTime,time); }
        @EventHandler public void update(EventUpdate.Slow event) {
            if(nullCheck() || !weather.is("Snow")) return;
            for(int i=0;i<6;i++) {
                var pos=mc.player.getEntityPos().add((random.nextDouble()*2-1)*8,3+random.nextDouble()*5,(random.nextDouble()*2-1)*8);
                mc.world.addParticleClient(ParticleTypes.SNOWFLAKE,pos.x,pos.y,pos.z,0,-0.04,0);
            }
        }
        public float rain() { return weather.is("Clear") || weather.is("Snow")?0f:1f; }
        public float thunder() { return weather.is("Thunder")?1f:0f; }
    }
}
