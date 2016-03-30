import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class NonStaticMethod {
    @Builder
    public String m(@Required int id, String name) {
        return id + "_" + name;
    }
}
