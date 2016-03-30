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
 *
 * @see dk.danamlund.yabuilder.Builder
 * @see dk.danamlund.yabuilder.Required
 * @see dk.danamlund.yabuilder.Default
 */
package dk.danamlund.yabuilder;
