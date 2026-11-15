package elexis.modules.impl.render.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

/** Client-only companion with one physics update per entity tick and vanilla collision resolution. */
public final class ClientCompanionCat extends CatEntity {
    private double verticalVelocity;
    private double followDistance=1.5;
    private double velocityX,velocityZ;
    private int stuckTicks,teleportCooldown;
    public ClientCompanionCat(ClientWorld world) {
        super(EntityType.CAT,world);
        setAiDisabled(true);setNoGravity(true);setInvulnerable(true);
        setCustomName(Text.literal("Machkin"));setCustomNameVisible(true);
    }
    public void followDistance(double distance) {followDistance=distance;}
    @Override public void tick() {
        // Vanilla advances previous position, pose, sounds and render interpolation exactly once.
        setVelocity(Vec3d.ZERO);
        super.tick();
        var owner=MinecraftClient.getInstance().player;
        if(owner==null || owner.getEntityWorld()!=getEntityWorld()) return;
        if(teleportCooldown>0) teleportCooldown--;
        double dx=owner.getX()-getX(),dz=owner.getZ()-getZ(),distance=Math.hypot(dx,dz);
        if((distance>14 || owner.getY()-getY()>12 || getY()<getEntityWorld().getBottomY()-4) && teleportCooldown==0) {
            Vec3d spot=findGround((ClientWorld)getEntityWorld(),owner.getEntityPos(),owner.getYaw());
            teleportCooldown=20;
            if(spot!=null) {
                refreshPositionAndAngles(spot.x,spot.y,spot.z,owner.getYaw(),0);
                lastX=lastRenderX=spot.x;lastY=lastRenderY=spot.y;lastZ=lastRenderZ=spot.z;
                velocityX=velocityZ=verticalVelocity=0;
            }
            return;
        }
        double desiredX=0,desiredZ=0;
        if(isOnGround() && verticalVelocity<=0) verticalVelocity=-0.08;
        else verticalVelocity=Math.max(-0.8,(verticalVelocity-0.08)*0.98);
        if(distance>followDistance+0.15) {
            double speed=Math.min(0.23,(distance-followDistance)*0.14);
            desiredX=dx/distance*speed;desiredZ=dz/distance*speed;
        }
        if(horizontalCollision && isOnGround() && distance>followDistance+0.4) {
            if(getEntityWorld().isSpaceEmpty(this,getBoundingBox().offset(velocityX*3,1.05,velocityZ*3))) verticalVelocity=0.42;
            else {stuckTicks=8;}
        }
        if(stuckTicks>0 && distance>0.01) {
            stuckTicks--;desiredX=-dz/distance*0.13;desiredZ=dx/distance*0.13;
        }
        velocityX+=(desiredX-velocityX)*0.3;velocityZ+=(desiredZ-velocityZ)*0.3;
        double previousX=getX(),previousZ=getZ();
        move(MovementType.SELF,new Vec3d(velocityX,verticalVelocity,velocityZ));
        if(verticalCollision) verticalVelocity=0;
        fallDistance=0;
        float walked=(float)Math.hypot(getX()-previousX,getZ()-previousZ);
        limbAnimator.updateLimbs(Math.min(1f,walked*4f),0.4f,1f);
        if(walked>0.005) {
            float heading=(float)Math.toDegrees(Math.atan2(-velocityX,velocityZ));
            setYaw(MathHelper.stepUnwrappedAngleTowards(getYaw(),heading,18f));
            bodyYaw=getYaw();setHeadYaw(getYaw());
        }
    }

    /** Ground search only on spawn/long-distance recovery, never every rendered frame. */
    public static Vec3d findGround(ClientWorld world,Vec3d owner,float yaw) {
        for(int i=0;i<5;i++) {
            Vec3d offset=Vec3d.fromPolar(0,yaw+140+i*45).multiply(i==4?0:1.6);
            Vec3d start=owner.add(offset).add(0,2,0);
            BlockPos column=BlockPos.ofFloored(start);
            if(!world.isChunkLoaded(column.getX()>>4,column.getZ()>>4)) continue;
            var hit=world.raycast(new RaycastContext(start,start.add(0,-40,0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,MinecraftClient.getInstance().player));
            if(hit.getType()!=HitResult.Type.BLOCK) continue;
            Vec3d spot=hit.getPos().add(0,0.02,0);
            if(world.isSpaceEmpty(new Box(spot.x-0.3,spot.y,spot.z-0.3,spot.x+0.3,spot.y+0.8,spot.z+0.3))) return spot;
        }
        return null;
    }
}
