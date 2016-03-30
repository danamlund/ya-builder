import dk.danamlund.yabuilder.*;

public class Usage {
    private final int id;
    private final String name;

    @Builder("UsageBuilder")
    Usage(@Required int id, @Default("none") String name) {
        this.id = id;
        this.name = name;
    }
    
    public static void main(String[] args) {
        UsageBuilder.build(build -> build.name("foo"));
    }
}
