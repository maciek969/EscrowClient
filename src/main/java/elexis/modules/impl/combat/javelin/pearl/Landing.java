package elexis.modules.impl.combat.javelin.pearl;

import java.util.UUID;

public record Landing(int id, UUID owner, Impact impact, int spawnTick, int expires) {
}
