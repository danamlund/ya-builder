
import dk.danamlund.yabuilder.*;
import java.time.LocalDate;
import org.junit.Assert;

class OneOf {
    private final int id;
    private final String foo1;
    private final String foo2;
    private final String bar1;
    private final String bar2;
    
    @Builder
    OneOf(@Required int id, 
             @RequiredOneOf("foo") String foo1, 
             @RequiredOneOf("bar") String bar2, 
             String shortDescription, 
             @RequiredOneOf("bar") String bar1, 
             @RequiredOneOf("foo") String foo2) {
        this.id = id;
        this.foo1 = foo1;
        this.foo2 = foo2;
        this.bar1 = bar1;
        this.bar2 = bar2;
    }
    
    public static void test() {
        OneOf oneOf = OneOfBuilder.build(b -> b.id(42).foo2("foo").bar1("bar"));

        Assert.assertEquals(42, oneOf.id);
        Assert.assertEquals(null, oneOf.foo1);
        Assert.assertEquals("foo", oneOf.foo2);
        Assert.assertEquals("bar", oneOf.bar1);
        Assert.assertEquals(null, oneOf.bar2);
    }
}
