package dk.danamlund.yabuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a {@literal @}{@link Builder} parameter that it must be called.
 * <p>
 * {@literal @}Required parameters that are not set in the builder will give a compile-time error.
 *
 * @see dk.danamlund.yabuilder.Builder
 * @see dk.danamlund.yabuilder.Default
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface Required {
}
