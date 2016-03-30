import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class PrivateConstructor {
    private final int id;

    @Builder
    private PrivateConstructor(int id) {
        this.id = id;
    }
}
