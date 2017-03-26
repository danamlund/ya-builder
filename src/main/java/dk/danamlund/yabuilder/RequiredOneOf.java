package dk.danamlund.yabuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a {@literal @}{@link Builder} parameter that one of must be called.
 * <p>
 * Generate compile-time error if at least 1 of parameters in same group is set.
 * <p>
 * Note: it would be beter to check for exactly 1 parameter set, but that is trouble.
 * It could be done by instead of using generics, we create classes of every valid state
 * of the builder. But that will generate a large number of classes. And it requires a 
 * fundamental differet technique than used here.
 * <p>
 * The string specifies the name of the group of parameters. There
 * must be at least 2 parameters with the same group name.
 * <p>
 * The parameters in the group that was not defined will be set to null.
 * <p>
 * Parameters of type String will automatically get surrounded by "". So 
 * {@code @Default("\"foo\"")} generates {@code String s = "\"\"foo\"\"";}.
 *
 * @see dk.danamlund.yabuilder.Builder
 * @see dk.danamlund.yabuilder.Required
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface RequiredOneOf {
    String value();
}
