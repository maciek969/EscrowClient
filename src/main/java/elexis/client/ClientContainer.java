package elexis.client;

import lombok.Getter;
import lombok.Setter;

public class ClientContainer {

    /*
    Dev-tools
     */
    @Getter
    @Setter
    public static boolean devMode = false;
    @Getter @Setter public static String owner = "Elexis";

    /*
    Name-tools
     */

    @Getter @Setter public static String name = "Elexis";
    @Getter @Setter public static String nameSmall = name.toLowerCase();
    @Getter @Setter public static String nameId = "1.21.11";
    @Getter @Setter public static String nameFull = name + " " + nameId;
    @Getter @Setter public static String prefixTitle = nameFull + " » ";

    /*
    User-tools
     */

    @Getter @Setter public static String uid = "1";
    @Getter @Setter public static String user = "Elexis";
    @Getter @Setter public static String role = "Admin";
    @Getter @Setter public static String hwid = "";
}