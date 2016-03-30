import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class PrivateClass {
    private static class Private {
        private final int id;
        
        @Builder
        Private(@Required int id) {
            this.id = id;
        }
    }
}
