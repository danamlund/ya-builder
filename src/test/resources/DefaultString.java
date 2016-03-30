import dk.danamlund.yabuilder.*;
import java.time.LocalDate;
import org.junit.Assert;

class DefaultString {
    private final String stringNoDefault;
    private final String stringDefault;
    private final String stringDefaultNull;
    private final LocalDate dateNoDefault;
    private final LocalDate dateDefault;
    private final LocalDate dateDefaultNull;
    
    @Builder
    DefaultString(String stringNoDefault, 
                  @Default("foo") String stringDefault, 
                  @Default("null") String stringDefaultNull,
                  LocalDate dateNoDefault, 
                  @Default("java.time.LocalDate.of(2016, 3, 21)") LocalDate dateDefault, 
                  @Default("null") LocalDate dateDefaultNull) {
        this.stringNoDefault = stringNoDefault;
        this.stringDefault = stringDefault;
        this.stringDefaultNull = stringDefaultNull;
        this.dateNoDefault = dateNoDefault;
        this.dateDefault = dateDefault; 
        this.dateDefaultNull = dateDefaultNull;
    }
    
    public static void test() {
        {
            DefaultString none = DefaultStringBuilder.build(b -> b);
            Assert.assertEquals(null, none.stringNoDefault);
            Assert.assertEquals("foo", none.stringDefault);
            Assert.assertEquals("null", none.stringDefaultNull);
            Assert.assertEquals(null, none.dateNoDefault);
            Assert.assertEquals(LocalDate.of(2016, 3, 21), none.dateDefault);
            Assert.assertEquals(null, none.dateDefaultNull);
        }
        {
            DefaultString none = 
                DefaultStringBuilder.build(b -> b.stringNoDefault(null)
                                                   .stringDefault(null)
                                                   .stringDefaultNull(null)
                                                   .dateNoDefault(null)
                                                   .dateDefault(null)
                                                   .dateDefaultNull(null));
            Assert.assertEquals(null, none.stringNoDefault);
            Assert.assertEquals(null, none.stringDefault);
            Assert.assertEquals(null, none.stringDefaultNull);
            Assert.assertEquals(null, none.dateNoDefault);
            Assert.assertEquals(null, none.dateDefault);
            Assert.assertEquals(null, none.dateDefaultNull);
        }
    }
}
