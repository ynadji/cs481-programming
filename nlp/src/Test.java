import nlp.token.*;
import nlp.diff.*;

import java.io.*;

public class Test {

	private static final String PROGRAM_DIR = "/Users/ynadji/Documents/Homework/cs481/homework/programming/nlp";

	public static void main(String[] args) throws IOException {

		Tokenizer t = new Tokenizer(new BufferedReader(new FileReader(PROGRAM_DIR + "/Gene.txt")), true);
		Token token;
		int numTokens = 100;

		while (numTokens > 0) 
		{
			token = t.getNext();
			if (token.getAttrib("POS") != null)
			{
				System.out.println("Token name: " + token.getName());
				System.out.println("Token attr: " + token.getAttrib("POS") + "\n");

				System.out.println("Attr HashMap: " + token.getHashMap().toString() + "\n\n");

				numTokens--;
			}
		}

		System.out.println("Done");
	}
}
