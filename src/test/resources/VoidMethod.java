import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class VoidMethod {
    private static String tmp = null;

    @Builder
    public static void m(@Required int id, String name) {
        tmp = id + "_" + name;
    }

    public static void test() {
        VoidMethod_mBuilder.build(b -> b.id(42).name("foo"));
        Assert.assertEquals("42_foo", tmp);

        VoidMethod_mBuilder.build(b -> b.id(42));
        Assert.assertEquals("42_null", tmp);
    }
}
