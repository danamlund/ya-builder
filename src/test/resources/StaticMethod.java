import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class StaticMethod {

    @Builder
    public static String staticMethod(@Required int id, String name) {
        return id + "_" + name;
    }

    public static void test() {
        Assert.assertEquals("42_foo", 
                            StaticMethod_staticMethodBuilder.build(b -> b.id(42).name("foo")));
        Assert.assertEquals("42_null", 
                            StaticMethod_staticMethodBuilder.build(b -> b.id(42)));
    }
}
