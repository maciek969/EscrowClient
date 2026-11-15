package elexis.mixins.accessors;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    @Accessor(value = "lastYawClient")
    float getLastYaw();

    @Accessor(value = "lastPitchClient")
    float getLastPitch();

    @Accessor(value = "lastYawClient")
    void setLastYaw(float yaw);

    @Accessor(value = "lastPitchClient")
    void setLastPitch(float pitch);
}