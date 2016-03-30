import dk.danamlund.yabuilder.*;
import org.junit.Assert;

class PrivateParentClass {
    private static class PrivateClass {
        public static class Private {
            private final int id;
            
            @Builder
            Private(@Required int id) {
                this.id = id;
            }
        }
    }
}
