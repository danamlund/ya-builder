
import dk.danamlund.yabuilder.*;
import java.time.LocalDate;
import org.junit.Assert;

class Advanced {
    private final int id;
    private final String advancedName;
    private final String shortDescription;
    private final String longDescription;
    private final LocalDate date;
    
    @Builder
    Advanced(@Required int id, 
             @Required String advancedName, 
             String shortDescription, 
             @Default("null") String longDescription,
             @Default("java.time.LocalDate.of(2016, 3, 21)") LocalDate date) {
        this.id = id;
        this.advancedName = advancedName;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.date = date;
    }
    
    public static void test() {
        Advanced required = AdvancedBuilder.build(b -> b.id(42).advancedName("name"));
        Assert.assertEquals(42, required.id);
        Assert.assertEquals("name", required.advancedName);
        Assert.assertEquals(null, required.shortDescription);
        Assert.assertEquals("null", required.longDescription);
        Assert.assertEquals(LocalDate.of(2016, 3, 21), required.date);
    }
}
