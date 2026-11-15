package elexis.modules.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface FunctionAdd {
    String name();
    String alias();
    int key() default 0;
    Category category();
    String description() default "No description has been added for this module yet";
}