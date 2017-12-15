package pl.poznan.put.cs.si.puttalky;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import morfologik.stemming.IStemmer;
import morfologik.stemming.WordData;
import morfologik.stemming.polish.PolishStemmer;

/** Author: agalawrynowicz<br>
 * Date: 19-Dec-2016 */

public class Parser {
	
	private String wypowiedz;
	private String[] slowaKluczowe;
	
	
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

	public String[] getSlowaKluczowe() {
		return slowaKluczowe;
	}

	public void setSlowaKluczowe(String[] slowaKluczowe) {
		this.slowaKluczowe = slowaKluczowe;
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
		setSlowaKluczowe(parsuj(buffer));
	}

	
	public String[] parsuj (String wypowiedz) {
		String[] slowa = wypowiedz.split("\\s");
		ArrayList<String> tokeny = new ArrayList<>();
		
		PolishStemmer s = new PolishStemmer();
		
		for (String slowo : slowa){ 
			String token;
			if (stem(s, slowo).length>1)
				token = stem(s, slowo)[0];
			else
				token = slowo.toLowerCase();
			tokeny.add(token);
		}
	    return tokeny.toArray(new String[tokeny.size()]);
	}
	
	public static String[] stem(IStemmer s, String slowo) {
	    ArrayList<String> result = new ArrayList<>();
	    for (WordData wd : s.lookup(slowo)) {
	      result.add(wd.getStem().toString());
	      result.add(wd.getTag().toString());
	    }
	    return result.toArray(new String[result.size()]);
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
