package yabloko.client.modules.api;

import yabloko.base.animations.base.Animation;

public interface Module {
    boolean isEnabled();
    int getKeyCode();
    String getName();
    Category getCategory();
    Animation getAnimation();
    int getBind();
}

