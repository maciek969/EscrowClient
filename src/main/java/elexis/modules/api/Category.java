package elexis.modules.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Category {
    Combat("b"),       // FIGHT
    Movement("c"),     // MOVEMENT
    Render("d"),       // RENDER
    Player("e"),       // PLAYER
    Utilities("o");    // GEAR

    final String icon;
}