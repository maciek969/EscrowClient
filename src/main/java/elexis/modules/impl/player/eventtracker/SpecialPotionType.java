package elexis.modules.impl.player.eventtracker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum SpecialPotionType {
    FLASH(16777215, "Вспышка", Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)),
    KILLER(13369344, "Зелье Киллера", Arrays.asList(12582912, 15007744, 14024704)),
    KILLER2(11796480, "Зелье Киллера", Arrays.asList(12582912, 15007744, 14024704)),
    BURP(16737792, "Зелье Отрыжки", Arrays.asList(16727040, 16733184, 16739072)),
    SULFURIC_ACID(10092339, "Серная кислота", Arrays.asList(9961472, 10223411, 10027007)),
    MEDIC(16711935, "Зелье Медика", Arrays.asList(16716287, 16707583, 14680063)),
    AGENT(16776960, "Зелье агента", Arrays.asList(16772608, 16773376, 16775892, 15663104)),
    WINNER(65280, "Зелье Победителя", Arrays.asList(59136, 57088, 63232, 65310, 65350)),
    URINE(65535, "Моча Флеша", List.of(65535));

    private final int baseColor;
    private final String displayName;
    private final List<Integer> colorVariations;
}
