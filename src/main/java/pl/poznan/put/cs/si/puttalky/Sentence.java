package pl.poznan.put.cs.si.puttalky;

import java.util.Arrays;

public class Sentence {
	    String[] with;
	    String[] without;

        Sentence(String sentence) {
            final Parser parser = new Parser();
            final String[] divided = sentence.split("\\s+bez\\s+");
            this.with = parser.parsuj(divided[0]);
            final String without = Arrays.stream(divided).skip(1)
                    .reduce((a, b) -> String.join(" ", a, b)).orElse("");
            this.without = parser.parsuj(without);
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