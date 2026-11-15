package elexis.modules.impl.render.visual;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.client.IMinecraft;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import java.util.Random;

/** Bounded physical droplets, surface impacts and water ripples. All motion is tick based. */
public final class RainWeather implements IMinecraft {
    private static final int MAX_DROPS=448,MAX_SPLASHES=104;
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("elexis","weather/water"))
            .withVertexFormat(VertexFormats.POSITION_COLOR,DrawMode.QUADS).withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withDepthWrite(false).withBlend(BlendFunction.TRANSLUCENT).build());
    private static final RenderLayer LAYER=RenderLayer.of("elexis_weather_water",RenderSetup.builder(PIPELINE).expectedBufferSize(131072).build());
    private static final double[] SIN=new double[17],COS=new double[17];
    static {for(int i=0;i<=16;i++){SIN[i]=Math.sin(i*Math.PI/8);COS[i]=Math.cos(i*Math.PI/8);}}
    private static final class Drop {
        double x,y,z,px,py,pz,ground;float speed,size;int age;
        int columnX,columnZ;boolean surface,water;
    }
    private static final class Splash {double x,y,z;int age,life;float radius;boolean water;}
    private final SceneVisuals.Rain module;
    private final Drop[] drops=new Drop[MAX_DROPS];
    private final Splash[] splashes=new Splash[MAX_SPLASHES];
    private final Random random=new Random();
    private final RainAudio audio=new RainAudio();
    private final BlockPos.Mutable column=new BlockPos.Mutable();
    private ClientWorld world;
    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate consumers;
    private int dropCount,splashCount,tick,soundCooldown;
    private float roof,amount;
    public long totalImpacts,renderedFrames;
    RainWeather(SceneVisuals.Rain module) {
        this.module=module;for(int i=0;i<drops.length;i++) drops[i]=new Drop();for(int i=0;i<splashes.length;i++) splashes[i]=new Splash();
    }
    public static boolean replacesVanilla() {
        var rain=SceneVisuals.active("Rain",SceneVisuals.Rain.class);
        return rain!=null && rain.customRain.get();
    }
    public void tick() {
        if(world!=mc.world) {clear();world=mc.world;}
        if(world==null || mc.player==null || !module.customRain.get()) {clear();return;}
        amount=module.rainOnly.get()?world.getRainGradient(1):1;
        if(amount<0.01f) {dropCount=splashCount=0;audio.stop();return;}
        tick++;if(soundCooldown>0) soundCooldown--;
        var eye=mc.player.getEyePos();
        boolean covered=world.getTopY(Heightmap.Type.MOTION_BLOCKING,MathHelper.floor(eye.x),MathHelper.floor(eye.z))>eye.y;
        roof+=((covered?1f:0f)-roof)*0.12f;
        audio.update(amount,roof,module.volume.get(),module.sound.get());
        double windX=Math.sin(tick*0.008)*0.045+0.025,windZ=Math.cos(tick*0.006)*0.035;
        for(int i=0;i<dropCount;) {
            Drop d=drops[i];d.px=d.x;d.py=d.y;d.pz=d.z;
            d.x+=windX;d.z+=windZ;d.y-=d.speed;d.speed=Math.min(0.7f,d.speed+0.02f);d.age++;
            int x=MathHelper.floor(d.x),z=MathHelper.floor(d.z);
            if(x!=d.columnX || z!=d.columnZ) surface(d,x,z);
            if(d.surface && d.y<=d.ground && d.py>d.ground && d.ground>eye.y-10) {impact(d);removeDrop(i);continue;}
            if(d.y<eye.y-10 || Math.abs(d.x-eye.x)>21 || Math.abs(d.z-eye.z)>21 || d.age>40) {removeDrop(i);continue;}
            i++;
        }
        for(int i=0;i<splashCount;) {
            if(++splashes[i].age>=splashes[i].life) {Splash s=splashes[i];splashes[i]=splashes[--splashCount];splashes[splashCount]=s;}else i++;
        }
        int cap=module.quality.is("Fast")?112:module.quality.is("High")?MAX_DROPS:280;
        if(elexis.modules.impl.utils.Optimizer.fewerParticles()) cap=Math.min(cap,160);
        while(dropCount>cap) dropCount--;
        int births=Math.max(1,Math.round((cap/18f)*amount*module.density.get()));
        double facingYaw=Math.toRadians(mc.player.getYaw()),forwardX=-Math.sin(facingYaw),forwardZ=Math.cos(facingYaw);
        for(int n=0;n<births && dropCount<cap;n++) {
            Drop d=drops[dropCount];
            double angle=random.nextDouble()*Math.PI*2,radius=Math.sqrt(random.nextDouble())*16;
            d.x=eye.x+Math.cos(angle)*radius;d.z=eye.z+Math.sin(angle)*radius;d.y=eye.y+3+random.nextDouble()*11;
            if(random.nextFloat()<0.6f) {
                double ahead=2+random.nextDouble()*11,side=(random.nextDouble()-0.5)*ahead*1.4;
                d.x=eye.x+forwardX*ahead+forwardZ*side;d.z=eye.z+forwardZ*ahead-forwardX*side;
            }
            surface(d,MathHelper.floor(d.x),MathHelper.floor(d.z));
            if(d.surface && d.y<=d.ground+0.1) continue;
            d.px=d.x;d.py=d.y;d.pz=d.z;d.speed=0.48f+random.nextFloat()*0.17f;d.size=0.75f+random.nextFloat()*0.5f;d.age=0;dropCount++;
        }
    }
    private void surface(Drop d,int x,int z) {
        d.columnX=x;d.columnZ=z;d.surface=false;
        if(!world.isChunkLoaded(x>>4,z>>4)) {d.ground=world.getBottomY()-1;return;}
        int top=world.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z);column.set(x,top-1,z);
        var state=world.getBlockState(column);var shape=state.getCollisionShape(world,column);var fluid=state.getFluidState();
        double height=shape.isEmpty()?0:shape.getMax(Direction.Axis.Y);
        if(!fluid.isEmpty()) height=Math.max(height,fluid.getHeight(world,column));
        d.ground=top-1+height;d.surface=height>0;d.water=!fluid.isEmpty();
    }
    private void impact(Drop d) {
        totalImpacts++;
        int max=module.quality.is("Fast")?32:module.quality.is("High")?MAX_SPLASHES:64;
        if(splashCount>=max) return;
        Splash s=splashes[splashCount++];s.x=d.x;s.y=d.ground+0.012;s.z=d.z;s.age=0;
        s.life=9+random.nextInt(8);s.radius=(d.water?0.32f:0.2f)*(0.8f+random.nextFloat()*0.4f);s.water=d.water;
        if(module.sound.get() && soundCooldown==0 && mc.player.getEyePos().squaredDistanceTo(new Vec3d(s.x,s.y,s.z))<36) {
            world.playSoundClient(s.x,s.y,s.z,RainAudio.DROP,SoundCategory.WEATHER,module.volume.get()*0.26f,0.88f+random.nextFloat()*0.24f,false);soundCooldown=5+random.nextInt(5);
        }
    }
    private void removeDrop(int index) {Drop d=drops[index];drops[index]=drops[--dropCount];drops[dropCount]=d;}
    public void render(EventRender.World event) {
        if(world!=mc.world || world==null || mc.player==null || !module.customRain.get() || dropCount+splashCount==0) return;
        if(allocator==null) {allocator=new BufferAllocator(131072);consumers=VertexConsumerProvider.immediate(allocator);}
        var vertices=consumers.getBuffer(LAYER);var matrix=event.getMatrixStack().peek().getPositionMatrix();
        Vec3d camera=mc.gameRenderer.getCamera().getCameraPos();float partial=event.getTicks();
        double yaw=Math.toRadians(mc.gameRenderer.getCamera().getYaw());double rightX=Math.cos(yaw),rightZ=Math.sin(yaw);
        for(int i=0;i<dropCount;i++) {
            Drop d=drops[i];double x=MathHelper.lerp(partial,d.px,d.x)-camera.x,y=MathHelper.lerp(partial,d.py,d.y)-camera.y,z=MathHelper.lerp(partial,d.pz,d.z)-camera.z;
            double width=0.012*d.size,length=(0.28+d.speed*0.16)*d.size;
            double dx=rightX*width,dz=rightZ*width;
            vertex(vertices,matrix,x-dx,y,z-dz,0.38f,0.54f,0.66f,0.55f*amount);
            vertex(vertices,matrix,x+dx,y,z+dz,0.38f,0.54f,0.66f,0.55f*amount);
            vertex(vertices,matrix,x+dx-0.015,y+length,z+dz-0.008,0.38f,0.54f,0.66f,0.09f*amount);
            vertex(vertices,matrix,x-dx-0.015,y+length,z-dz-0.008,0.38f,0.54f,0.66f,0.09f*amount);
        }
        for(int i=0;i<splashCount;i++) {
            Splash s=splashes[i];float t=(s.age+partial)/(float)s.life;float alpha=(1-t)*(1-t)*0.62f*amount;
            double radius=0.025+s.radius*t,width=0.006+(1-t)*0.009;
            double x=s.x-camera.x,y=s.y-camera.y,z=s.z-camera.z;
            ring(vertices,matrix,x,y,z,radius,width,alpha);
            if(t<0.55f) {
                // Four small crown droplets rise briefly before falling back into the surface.
                double height=Math.sin(t/0.55*Math.PI)*0.13,spread=t*0.12;
                for(int j=0;j<4;j++) {
                    double a=COS[j*4]*spread,b=SIN[j*4]*spread,wx=rightX*0.009,wz=rightZ*0.009;
                    vertex(vertices,matrix,x+a-wx,y+height,z+b-wz,0.28f,0.47f,0.61f,alpha);
                    vertex(vertices,matrix,x+a+wx,y+height,z+b+wz,0.28f,0.47f,0.61f,alpha);
                    vertex(vertices,matrix,x+a+wx,y+height+0.035,z+b+wz,0.28f,0.47f,0.61f,alpha*0.3f);
                    vertex(vertices,matrix,x+a-wx,y+height+0.035,z+b-wz,0.28f,0.47f,0.61f,alpha*0.3f);
                }
            }
        }
        try {consumers.draw();renderedFrames++;} finally {allocator.clear();}
    }
    private static void ring(VertexConsumer v,Matrix4f m,double x,double y,double z,double radius,double width,float alpha) {
        for(int i=0;i<16;i++) {
            vertex(v,m,x+COS[i]*(radius-width),y,z+SIN[i]*(radius-width),0.25f,0.43f,0.56f,alpha);
            vertex(v,m,x+COS[i]*radius,y,z+SIN[i]*radius,0.25f,0.43f,0.56f,alpha*0.5f);
            vertex(v,m,x+COS[i+1]*radius,y,z+SIN[i+1]*radius,0.25f,0.43f,0.56f,alpha*0.5f);
            vertex(v,m,x+COS[i+1]*(radius-width),y,z+SIN[i+1]*(radius-width),0.25f,0.43f,0.56f,alpha);
        }
    }
    private static void vertex(VertexConsumer v,Matrix4f m,double x,double y,double z,float r,float g,float b,float a) {v.vertex(m,(float)x,(float)y,(float)z).color(r,g,b,Math.max(0,a));}
    public void clear() {
        dropCount=splashCount=tick=soundCooldown=0;amount=roof=0;audio.stop();world=null;
        if(allocator!=null) {allocator.close();allocator=null;consumers=null;}
    }
    public int dropCount() {return dropCount;}
    public int splashCount() {return splashCount;}
    public float shelter() {return roof;}
    public boolean soundPlaying() {return audio.isPlaying();}
}
