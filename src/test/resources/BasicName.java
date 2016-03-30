import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class BasicName {
    private final int id;

    @Builder("BasicNameMyBuilder")
    BasicName(int id) {
        this.id = id;
    }
    
    public static void test() {
        BasicName basic = BasicNameMyBuilder.build(b -> b.id(42));
        Assert.assertEquals(42, basic.id);
    }
}
