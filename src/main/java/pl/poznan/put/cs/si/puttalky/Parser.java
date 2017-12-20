package pl.poznan.put.cs.si.puttalky;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Stream;

import morfologik.stemming.IStemmer;
import morfologik.stemming.polish.PolishStemmer;

public class Parser {
	
	private String wypowiedz;
	private Sentence sentence;
	
	
	public Parser(){}
	
	public Parser(String wypowiedz)
	{
		this.wypowiedz=wypowiedz;
	}

	public String getWypowiedz() {
		return wypowiedz;
	}

	public void setWypowiedz(String wypowiedz) {
		this.wypowiedz = wypowiedz;
	}

	public Sentence getSentence() {
		return sentence;
	}

	public void setSentence(Sentence sentence) {
		this.sentence = sentence;
	}

	public void przetworzOdpowiedz()
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String buffer="";
		try {
			buffer = in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setSentence(new Sentence(buffer));
	}

	public String[] parsuj(String wypowiedz) {
		String[] slowa = wypowiedz.split("\\s");
	    return normalize(slowa);
	}

	public static String[] normalize(String[] toNormalize) {
        PolishStemmer s = new PolishStemmer();
        return Arrays.stream(toNormalize)
                .map(w -> stem(s, w).length>1 ? stem(s, w)[0] : w.toLowerCase())
                .toArray(String[]::new);
    }
	
	public static String[] stem(IStemmer s, String slowo) {
	    return s.lookup(slowo).stream()
                .flatMap(wd -> Stream.of(wd.getStem(), wd.getTag()))
                .map(CharSequence::toString)
                .toArray(String[]::new);
	  }
	
    public static void main(String[] args) {
        try {
        	Parser p = new Parser();
            final String[] parsed = p.parsuj("Chciałabym pizzę wegetariańską");
            System.out.println(Arrays.toString(parsed));

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
	
}
