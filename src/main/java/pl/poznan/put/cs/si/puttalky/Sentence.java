package pl.poznan.put.cs.si.puttalky;

import java.util.Arrays;
import java.util.List;

public class Sentence {
    private final String[] with;
    private final String[] without;

    Sentence(String sentence) {
        final Parser parser = new Parser();
        final List<String> divided = Arrays.asList(sentence.split("\\s+bez\\s+"));
        final boolean isJustWithout = sentence.indexOf("bez") == 0;
        with = isJustWithout ? new String[0] : parser.parsuj(divided.get(0));
        final String without = divided.stream().skip(isJustWithout ? 0 : 1)
                .reduce((a, b) -> String.join(" ", a, b)).orElse("");
        this.without = parser.parsuj(without);
    }

    public String[] getWith() {
        return with;
    }

    public String[] getWithout() {
        return without;
    }

    @Override
    public String toString() {
        return "Sentence{" +
                "with=" + Arrays.toString(with) +
                ", without=" + Arrays.toString(without) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sentence sentence = (Sentence) o;
        return Arrays.equals(with, sentence.with) &&
                Arrays.equals(without, sentence.without);
    }

    @Override
    public int hashCode() {

        int result = Arrays.hashCode(with);
        result = 31 * result + Arrays.hashCode(without);
        return result;
    }
}