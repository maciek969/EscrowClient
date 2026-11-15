package elexis.client.utils.render.main.oldrender.helper;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class TextUtility {
    ArrayList<Integer> lettersNormal = new ArrayList<>();
    ArrayList<Integer> lettersSmall = new ArrayList<>();

    public String smallToNormal(String smallCaps) {
        StringBuilder builder = new StringBuilder();
        for (char ch : smallCaps.toCharArray()) {
            builder.append(charToNormal(ch));
        }
        return builder.toString().replace('ꜱ', 's');
    }

    public char charToNormal(char ch) {
        if (lettersSmall.isEmpty() || lettersNormal.isEmpty() || !lettersSmall.contains((int) ch)) return ch;
        return (char) (int) lettersNormal.get(lettersSmall.indexOf((int) ch));
    }

    static {
        for (char ch : "abcdefghijklmnopqrstuvwxyz".toCharArray()) {
            lettersNormal.add((int) ch);
        }
        for (char ch : "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘᴏʀsᴛᴜᴠᴡxʏᴢ".toCharArray()) {
            lettersSmall.add((int) ch);
        }
    }
}
