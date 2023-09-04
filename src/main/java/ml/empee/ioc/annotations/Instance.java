package ml.empee.ioc.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to inject the bean instance into a static field
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface Instance {
}
