package elexis.client.utils.render.main.oldrender.msdf;

import elexis.client.utils.render.main.oldrender.system.TextureUse;

import java.util.HashMap;
import java.util.Map;

public class Fonts {
    private final Map<String, MsdfFont> fonts = new HashMap<>();

    public MsdfFont get(TextureUse textureUse) {
        String atlasName = textureUse == TextureUse.SFPRODISPLAYMEDIUM
                ? "sfprodisplaymedium"
                : textureUse.name().toLowerCase();
        return fonts.computeIfAbsent(textureUse.name(), name -> MsdfFont.builder()
                .atlas(atlasName)
                .data(atlasName)
                .build(textureUse));
    }

    public void clearFonts() {
        fonts.clear();
    }
}
