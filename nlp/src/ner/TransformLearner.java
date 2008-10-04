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

	private static final String[] ATTR_TYPES = new String[] { PERS_ATTR, ORG_ATTR, LOC_ATTR };

	/* Gazeteer. Little bit of a hack on St David's, but it should be fine. */
	private static final String[][] GAZETEER = 
		new String[][] {
			{ "United", "States", "LOC" },
			{ "Britain", "LOC" },
			{ "Welsh", "Water", "ORG" },
			{ "Gwynedd", "LOC" },
			{ "Anglesey", "LOC" },
			{ "Watford", "LOC" },
			{ "Kilmarnock", "LOC" },
			{ "Professor", "John", "Mansbridge", "PERS" },
			{ "University", "of", "Edinburgh", "ORG" },
			{ "Ayr", "LOC" },
			{ "Thomas", "and", "Kassab", "ORG" },
			{ "St", "David","'s", "ORG" },
			{ "Maternity", "Hospital", "ORG" },
			{ "England", "LOC" },
			{ "Scotland", "LOC" },
			{ "North", "Western", "Region", "LOC" },
			{ "Strathclyde", "LOC" },
			{ "Yorkshire", "LOC" },
			{ "Wessex", "LOC" },
			{ "Northern", "Ireland", "LOC" },
			{ "Drinking", "Water", "Inspectorate", "ORG" },
			{ "Department", "of", "Environment", "ORG" },
			{ "Wales", "LOC" },
			{ "Sir", "Kenneth", "Bloomfield", "PERS" } };

	public TransformLearner(String trainingFile, boolean hasPOS) {
		this.trainingFile = trainingFile;
		this.hasPOS = hasPOS;
		this.tokens = new LinkedList<Token>();
	}

	public static void main(String[] args) {

		TransformLearner tl = new TransformLearner("/Users/ynadji/Documents/Homework/cs481/homework/programming/nlp/Gene.txt",
							   true);

		tl.initTokenList();
		tl.runSeedRules();
		//tl.printResults("/Users/ynadji/Documents/Homework/cs481/homework/programming/nlp/results.txt", 1000);
		tl.printNonNullTags("/Users/ynadji/Documents/Homework/cs481/homework/programming/nlp/results.txt");
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

		this.gazeteerTagging(GAZETEER);
	}

	/**
	 * gazStrings represents an array of strings that hold 
	 * gazeteer's for common phrases with their tag type included,
	 * i.e: ["United", "States", "L"] represents a gazTuple of the
	 * phrase "United States", flagging it as a location.
	 */
	private void gazeteerTagging(String[][] gazStrings) {

		TripleIterator iter = new TripleIterator(tokens);
		Token[] tokenTrip = iter.getCurrent();

		/**
		 * Always compare to the last element, as
		 * that's the only one guaranteed to hit
		 * every token with TripleIterator
		 */
		do
		{
			for (String[] gazTuple : gazStrings)
			{
				switch (gazTuple.length - 1)
				{
					case 1:
					if (gazTuple[0].equals(tokenTrip[TripleIterator.NEXT].getName()))
						tokenTrip[TripleIterator.NEXT].putAttrib(gazTuple[gazTuple.length - 1], "I");
					break;

					case 2: 
					if (tokenTrip[TripleIterator.CURR] == null)
						break;

					if (gazTuple[0].equals(tokenTrip[TripleIterator.CURR].getName()) &&
					    gazTuple[1].equals(tokenTrip[TripleIterator.NEXT].getName()))
					{
						tokenTrip[TripleIterator.CURR].putAttrib(gazTuple[gazTuple.length - 1], "I");
						tokenTrip[TripleIterator.NEXT].putAttrib(gazTuple[gazTuple.length - 1], "I");
					}
					break;

					case 3: 
					if (tokenTrip[TripleIterator.CURR] == null ||
					    tokenTrip[TripleIterator.PREV] == null)
						break;

					if (gazTuple[0].equals(tokenTrip[TripleIterator.PREV].getName()) &&
					    gazTuple[1].equals(tokenTrip[TripleIterator.CURR].getName()) &&
					    gazTuple[2].equals(tokenTrip[TripleIterator.NEXT].getName()))
					{
						tokenTrip[TripleIterator.PREV].putAttrib(gazTuple[gazTuple.length - 1], "I");
						tokenTrip[TripleIterator.CURR].putAttrib(gazTuple[gazTuple.length - 1], "I");
						tokenTrip[TripleIterator.NEXT].putAttrib(gazTuple[gazTuple.length - 1], "I");
					}
					break;
				}
			}

		} while((tokenTrip = iter.next()) != null);
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

	private void printNonNullTags(String outfile) {

		try
		{
			PrintWriter out = new PrintWriter(new FileWriter(outfile));
			ListIterator<Token> iter = tokens.listIterator();
			Token tok;
			String attr;

			out.printf("%-25s%-10s%-10s\n", "TokenName", "POS Tag", "Type");

			while (iter.hasNext())
			{
				tok = iter.next();
				attr = getEntityType(tok);

				if (attr != null)
				{
					out.printf("%-25s%-10s%-10s\n",
						   tok.getName(),
						   tok.getAttrib("POS"),
						   attr);
				}
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

	private class TripleIterator {

		private List<Token> tokens;
		private Token prev;
		private Token curr;
		private Token next;

		public static final int PREV = 0;
		public static final int CURR = 1;
		public static final int NEXT = 2;

		private int indexNext;

		public TripleIterator(List<Token> tokens) {
			this.tokens = tokens;
			prev = null;
			curr = null;
			next = tokens.get(0);
			indexNext = 1;
		}

		public Token[] getCurrent() {
			return new Token[] {prev, curr, next};
		}

		public Token[] next() {
			if (this.hasNext()) {
				prev = curr;
				curr = next;
				next = tokens.get(indexNext++);

				return new Token[] {prev, curr, next};
			}
			else
				return null;
		}

		public boolean hasNext() {
			return indexNext < tokens.size();
		}
	}
}
