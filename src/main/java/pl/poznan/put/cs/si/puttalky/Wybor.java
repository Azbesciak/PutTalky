package pl.poznan.put.cs.si.puttalky;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Wybor extends Formatka {

    public static final String ZAMOWIENIE = "order";
    public static final String PORADA = "help";

    private static Predicate<String> isHelp = Pattern.compile("pomoc|porada").asPredicate();
    private static Predicate<String> isOrder = Pattern.compile("zam[oó]w(ienie)?").asPredicate();

    @Override
    public void zadajPytanie() {
        System.out.println(getMonit());
    }

    public String parseAnswer(String... tokens) {
        return Arrays.stream(tokens)
                .filter(isHelp.or(isOrder))
                .map(this::mapChoice)
                .findFirst()
                .orElse(PORADA);
    }

    private String mapChoice(String text) {
        if (isHelp.test(text)) {
            return PORADA;
        } else {
            return ZAMOWIENIE;
        }
    }
}
