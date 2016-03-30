import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class StaticMethodName {

    @Builder("StaticMethodNameMyBuilder")
    public static String staticMethod(@Required int id, String name) {
        return id + "_" + name;
    }

    public static void test() {
        Assert.assertEquals("42_foo", 
                            StaticMethodNameMyBuilder.build(b -> b.id(42).name("foo")));
        Assert.assertEquals("42_null", 
                            StaticMethodNameMyBuilder.build(b -> b.id(42)));
    }
}
