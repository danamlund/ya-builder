package dk.danamlund.yabuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set the default value of a {@literal @}{@link Builder} parameter.
 * <p>
 * The string value must be a valid java right-hand side expression.
 * <p>
 * Parameters of type String will automatically get surrounded by "". So 
 * {@code @Default("\"foo\"")} generates {@code String s = "\"\"foo\"\"";}.
 *
 * @see dk.danamlund.yabuilder.Builder
 * @see dk.danamlund.yabuilder.Required
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface Default {
    String value();
}
