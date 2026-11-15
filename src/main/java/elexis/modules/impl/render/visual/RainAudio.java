package elexis.modules.impl.render.visual;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Two original seamless loops crossfade at roofs; sound resources follow Minecraft's sliders. */
final class RainAudio implements IMinecraft {
    private Loop outdoor,sheltered;
    private float targetOutdoor,targetSheltered;
    private int ticks;
    static final SoundEvent DROP=SoundEvent.of(Identifier.of("elexis","weather.puddle_drop"));
    void update(float amount,float roof,float volume,boolean enabled) {
        if(!enabled || amount<=0.001f || volume<=0.001f) {stop();return;}
        targetOutdoor=amount*volume*(1-roof*0.93f);
        targetSheltered=amount*volume*roof;
        boolean retry=(++ticks%40)==0;
        if(outdoor==null || retry && !mc.getSoundManager().isPlaying(outdoor)) {
            if(outdoor!=null) mc.getSoundManager().stop(outdoor);
            outdoor=new Loop("weather.rain_outdoor",false);mc.getSoundManager().play(outdoor);
        }
        if(sheltered==null || retry && !mc.getSoundManager().isPlaying(sheltered)) {
            if(sheltered!=null) mc.getSoundManager().stop(sheltered);
            sheltered=new Loop("weather.rain_sheltered",true);mc.getSoundManager().play(sheltered);
        }
    }
    boolean isPlaying() {return outdoor!=null && mc.getSoundManager().isPlaying(outdoor);}
    void stop() {
        targetOutdoor=targetSheltered=0;
        ticks=0;
        if(outdoor!=null) {mc.getSoundManager().stop(outdoor);outdoor.finish();outdoor=null;}
        if(sheltered!=null) {mc.getSoundManager().stop(sheltered);sheltered.finish();sheltered=null;}
    }
    private final class Loop extends MovingSoundInstance {
        private final boolean roof;
        Loop(String name,boolean roof) {
            super(SoundEvent.of(Identifier.of("elexis",name)),SoundCategory.WEATHER,SoundInstance.createRandom());
            this.roof=roof;repeat=true;repeatDelay=0;relative=true;attenuationType=SoundInstance.AttenuationType.NONE;
            volume=Math.max(0.002f,roof?targetSheltered:targetOutdoor);pitch=1;
        }
        @Override public void tick() {volume+=( (roof?targetSheltered:targetOutdoor)-volume)*0.15f;}
        @Override public boolean shouldAlwaysPlay() {return true;}
        @Override public boolean canPlay() {return true;}
        void finish() {setDone();}
    }
}
