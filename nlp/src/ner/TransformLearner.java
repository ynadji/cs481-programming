package nlp.ner;

import nlp.token.*;
import nlp.diff.*;

import java.io.*;
import java.util.*;

public class TransformLearner {

	private String trainingFile;
	private boolean hasPOS;
	private List<Token> tokens;

	private static final String PERS_ATTR = "PERS";
	private static final String ORG_ATTR = "ORG";
	private static final String LOC_ATTR = "LOC";

	private static final String CHUNK_BEGIN = "B";
	private static final String CHUNK_INSIDE = "I";
	private static final String CHUNK_OUTSIDE = "O";

	public TransformLearner(String trainingFile, boolean hasPOS) {
		this.trainingFile = trainingFile;
		this.hasPOS = hasPOS;
		this.tokens = new LinkedList<Token>();
	}

	public static void main(String[] args) {
		TransformLearner tl = new TransformLearner("/Users/ynadji/Documents/Homework/cs481/homework/programming/nlp/Gene.txt",
							   true);

		tl.initTokenList();
		tl.printResults("/Users/ynadji/Documents/Homework/cs481/homework/programming/nlp/results.txt", 1000);
	}

	public void initTokenList() {
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(trainingFile));
			Tokenizer t = new Tokenizer(in, hasPOS);
			Token tok;

			while ((tok = t.getNext()) != null)
			{
				// everything is tagged, throw out newlines
				if (tok.getAttrib("POS") != null)
					tokens.add(tok);
			}

			in.close();
		} 
		catch (IOException ioe) 
		{
			ioe.printStackTrace();
		}
	}

	public void runSeedRules() {
	}

	public void printResults(String outfile, int numLastTokens) {
		try
		{
			PrintWriter out = new PrintWriter(new FileWriter(outfile));
			ListIterator<Token> iter = tokens.listIterator(tokens.size() - numLastTokens);
			Token tok;

			out.printf("%-25s%-10s%-10s\n", "TokenName", "POS Tag", "Type");

			while (iter.hasNext())
			{
				tok = iter.next();
				out.printf("%-25s%-10s%-10s\n",
					   tok.getName(),
					   tok.getAttrib("POS"),
					   getEntityType(tok));
			}

			out.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		catch (IndexOutOfBoundsException indxe)
		{
			indxe.printStackTrace();
		}
	}

	private String getEntityType(Token tok) {
		if (tok.getAttrib(PERS_ATTR) != null)
			return PERS_ATTR;
		else if (tok.getAttrib(ORG_ATTR) != null)
			return ORG_ATTR;
		else if (tok.getAttrib(LOC_ATTR) != null)
			return LOC_ATTR;

		return null;
	}
}
