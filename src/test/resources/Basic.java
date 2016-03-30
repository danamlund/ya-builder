import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class Basic {
    private final int id;

    @Builder
    Basic(int id) {
        this.id = id;
    }
    
    public static void test() {
        Basic basic = BasicBuilder.build(b -> b.id(42));
        Assert.assertEquals(42, basic.id);
    }
}
