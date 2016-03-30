package dk.danamlund.yabuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Automatically create builders for constructors and static methods.
 * <p>
 * Basic example usage:
 * <pre>
 * class Usage {
 *     private final int id;
 *     private final String name;
 * 
 *    {@literal @}Builder("UsageBuilder")
 *     Usage({@literal @}Required int id,{@literal @}Default("none") String name) {
 *         this.id = id;
 *         this.name = name;
 *     }
 *     
 *     public static void main(String[] args) {
 *         Usage a = UsageBuilder.build(build{@literal ->} build.id(42));
 *         Usage b = UsageBuilder.build(build{@literal ->} build.id(42)
 *                                                    .name("not empty"));
 *     }
 * }
 * </pre>
 * <p>
 * Parameters are optional by default. <br>
 * Parameters annotated with {@code @Required} will give compile-time 
 * errors if not set in the builder. <br>
 * Parameters annotated with {@code @Default("java.time.LocalDate.of(2016, 3, 21)")} will 
 * have the argument as value if not set in the builder. <br>
 * <p>
 * Constructors annotated with @Builder must:
 * <ul>
 * <li>Not be {@code private}.
 * <li>Have at least one parameter.
 * <li>Be either a constructor or a static method.
 * <li>Be in non-private classes (All nested classes and main class must be non-private)
 * <li>Not have an existing class with the same name as the generate builder 
 *     class in the same package.
 * </ul>
 *
 * @see dk.danamlund.yabuilder.Required
 * @see dk.danamlund.yabuilder.Default
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Builder {
    /**
     * The name of the generated Builder class.
     * <p>
     * The generated Builder class is put into the same package as the class it is attached to.
     * <p>
     * Must be a valid java class name.
     * <p>
     * The default name of created builder classes are
     * <ul>
     * <li>On constructor in top-level class {@code foo.bar.MyClass}: 
     *   Builder class is {@code foo.bar.MyClassBuilder}.
     * <li>On static method {@code method} in top-level class {@code foo.bar.MyClass}: 
     *   Builder class is {@code foo.bar.MyClass_methodBuilder}.
     * <li>On constructor in static inner class {@code foo.bar.ParentClass.MyClass}: 
     *   Builder class is {@code foo.bar.ParentClassMyClass}
     * <li>On static method {@code method} in static inner class {@code foo.bar.ParentClass.MyClass}: 
     *   Builder class is {@code foo.bar.ParentClassMyClass_methodBuilder}
     * </ul>
     *
     * @return The name of the generated Builder class.
     */
    String value() default "";
}
