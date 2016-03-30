import dk.danamlund.yabuilder.*;
import java.time.LocalDate;

public class Complex {

    @Builder("ComplexBuilder")
    public static String complex(@Required int id, 
                                 @Required String name,
                                 @Default("none") String comment,
                                 String comment2,
                                 @Default("java.time.LocalDate.now()") LocalDate date,
                                 @Required LocalDate birthDate,
                                 @Required String uuid,
                                 @Required String ssn,
                                 @Required int heightInCm,
                                 @Required double heightInFeet,
                                 @Required boolean isFalse) {
        return id + " " + name + " " + comment + " " + comment2 
            + " "  + date + " " + birthDate + " " + uuid + " " + ssn
            + " " + heightInCm + " " + heightInFeet + " " + isFalse;
    }
    
    public static void main(String[] args) {
        String compelx = ComplexBuilder.build(build -> build.id(42)
                                              // .birthDate(LocalDate.of(1, 1, 1970))
                                              .name("Foo")
                                              // .comment("none")
                                              // .comment2(null)
                                              // .date(LocalDate.now())
                                              .isFalse(false)
                                              .ssn("1337")
                                              .uuid("d3b07384d113edec49eaa6238ad5ff00")
                                              // .heightInCm(42)
                                              .heightInFeet(1.38));
    }
}
