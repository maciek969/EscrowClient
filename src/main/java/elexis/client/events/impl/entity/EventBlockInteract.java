package elexis.client.events.impl.entity;

import elexis.client.events.api.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

@Getter
@AllArgsConstructor
public class EventBlockInteract extends Event {
    private final Hand hand;
    private final ItemStack stack;
    private final BlockHitResult hitResult;
    private final BlockPos placePos;
}