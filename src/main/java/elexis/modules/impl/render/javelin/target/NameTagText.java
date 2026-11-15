package elexis.modules.impl.render.javelin.target;

import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import java.util.Optional;

/** Javelin's server-rank and small-cap replacements, retaining styled text runs. */
public final class NameTagText {
    private record Symbol(String target, String replacement) {}
    private NameTagText() {}
   public static final Symbol[] STRING_SYMBOL_REPLACEMENTS = new Symbol[]{
      new Symbol("ꔓ", Formatting.BLUE + "ML.MODER"),
      new Symbol("ꔷ", Formatting.RED + "ADMIN"),
      new Symbol("ꔁ", Formatting.YELLOW + "MEDIA"),
      new Symbol("ꕁ", Formatting.GOLD + "GOD"),
      new Symbol("ꔗ", Formatting.BLUE + "MODER"),
      new Symbol("ꔥ", Formatting.BLUE + "ST.MODER"),
      new Symbol("ꔡ", Formatting.LIGHT_PURPLE + "MODER+"),
      new Symbol("ꔀ", Formatting.GRAY + "PLAYER"),
      new Symbol("ꔉ", Formatting.YELLOW + "HELPER"),
      new Symbol("◆", "@"),
      new Symbol("┃", "|"),
      new Symbol("ꔳ", Formatting.AQUA + "ML.ADMIN"),
      new Symbol("ꔅ", Formatting.RED + "Y" + Formatting.WHITE + "T"),
      new Symbol("ꔂ", Formatting.BLUE + "D.MODER"),
      new Symbol("ꕠ", Formatting.YELLOW + "D.HELPER"),
      new Symbol("ꕄ", Formatting.RED + "DRACULA"),
      new Symbol("ꔖ", Formatting.AQUA + "OVERLORD"),
      new Symbol("ꕈ", Formatting.GREEN + "COBRA"),
      new Symbol("ꔨ", Formatting.LIGHT_PURPLE + "DRAGON"),
      new Symbol("ꔤ", Formatting.RED + "IMPERATOR"),
      new Symbol("ꔠ", Formatting.GOLD + "MAGISTER"),
      new Symbol("ꔄ", Formatting.BLUE + "HERO"),
      new Symbol("ꔒ", Formatting.GREEN + "AVENGER"),
      new Symbol("ꕒ", Formatting.WHITE + "RABBIT"),
      new Symbol("ꔈ", Formatting.YELLOW + "TITAN"),
      new Symbol("ꕀ", Formatting.DARK_GREEN + "HYDRA"),
      new Symbol("ꕉ", Formatting.GOLD + "PEGAS"),
      new Symbol("ꔶ", Formatting.GOLD + "TIGER"),
      new Symbol("ꔲ", Formatting.DARK_PURPLE + "BULL"),
      new Symbol("ꕖ", Formatting.BLACK + "BUNNY"),
      new Symbol("ꕗꕘ", Formatting.YELLOW + "SPONSOR"),
      new Symbol("\ud83d\udd25", "@"),
      new Symbol("ᴀ", "A"),
      new Symbol("ʙ", "B"),
      new Symbol("ᴄ", "C"),
      new Symbol("ᴅ", "D"),
      new Symbol("ᴇ", "E"),
      new Symbol("ғ", "F"),
      new Symbol("ɢ", "G"),
      new Symbol("ʜ", "H"),
      new Symbol("ɪ", "I"),
      new Symbol("ᴊ", "J"),
      new Symbol("ᴋ", "K"),
      new Symbol("ʟ", "L"),
      new Symbol("ᴍ", "M"),
      new Symbol("ɴ", "N"),
      new Symbol("ꜱ", "S"),
      new Symbol("ᴏ", "O"),
      new Symbol("ᴘ", "P"),
      new Symbol("ǫ", "Q"),
      new Symbol("ʀ", "R"),
      new Symbol("ᴛ", "T"),
      new Symbol("ᴜ", "U"),
      new Symbol("ᴠ", "V"),
      new Symbol("ᴡ", "W"),
      new Symbol("ꜰ", "F"),
      new Symbol("ʏ", "Y"),
      new Symbol("ᴢ", "Z")
   };

    public static Text normalize(Text input) {
        MutableText output=Text.empty();
        input.visit((style,content)->{
            String value=content.replace('\n',' ');
            for(Symbol symbol:STRING_SYMBOL_REPLACEMENTS) value=value.replace(symbol.target(),symbol.replacement());
            output.append(Text.literal(value).setStyle(style));
            return Optional.empty();
        },Style.EMPTY);
        return output;
    }
}
