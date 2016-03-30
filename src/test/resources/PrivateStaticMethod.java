import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class PrivateStaticMethod {
    @Builder
    private static String foo(@Required int id, String name) {
        return id + "_" + name;
    }
}
