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
        Usage a = UsageBuilder.build(build -> build.id(42));
        Usage b = UsageBuilder.build(build -> build.id(42)
                                                   .name("not empty"));
    }
}
