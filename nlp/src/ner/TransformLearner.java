package nlp.ner;

import nlp.token.*;
import nlp.diff.*;

import java.io.*;
import java.util.*;

public class TransformLearner {

	// This needs to be changed to point to the directory
	// containing the sample files and the Gene.txt file
	private static final String MAIN_DIR = "/Users/ynadji/Documents/Homework/cs481/homework/programming/nlp/";

	private String trainingFile;

	// P values
	private int orgP;
	private int locP;
	private int perP;

	private boolean hasPOS;
	private List<Token> tokens;
	private List<Token> sampleTokens;
	private Map<String, List> nameToTokenList;

	private static int numRules = 20;
	private static int numRulesInc = 20;

	// flag for debugging
	private static final boolean debug = false;

	/* nice constants */
	private static final String PERS_ATTR = "PERS";
	private static final String ORG_ATTR = "ORG";
	private static final String LOC_ATTR = "LOC";

	private static final String CHUNK_BEGIN = "B";
	private static final String CHUNK_INSIDE = "I";
	private static final String CHUNK_OUTSIDE = "O";

	private static final String[] ATTR_TYPES = new String[] { PERS_ATTR, ORG_ATTR, LOC_ATTR };

	/* Gazeteer. Little bit of a hack on St David's, but it should be fine. */
	private static final String[][] GAZETEER = 
		new String[][] 
		{
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
			{ "Sir", "Kenneth", "Bloomfield", "PERS" },
			{ "NHS", "ORG" },
			{ "London", "LOC" },
			{ "Wales", "LOC" },
			{ "Edinburgh", "LOC" }
		};

	public TransformLearner(String trainingFile, String[][] sampleSets, boolean hasPOS) {
		this.trainingFile = trainingFile;
		this.hasPOS = hasPOS;
		this.tokens = new LinkedList<Token>();
		this.nameToTokenList = new HashMap<String, List>();

		this.orgP = 0;
		this.locP = 0;
		this.perP = 0;

		this.initSampleSets(sampleSets);
	}

	/**
	 * Main. Runs the NER algorithm.
	 */
	public static void main(String[] args) {

		String[][] sampleSets = 
				new String[][]
				{
					{ MAIN_DIR+"GeneStart.txt", MAIN_DIR+"GeneStartEntities.txt" },
					{ MAIN_DIR+"GeneEnd.txt", MAIN_DIR+"GeneEndEntities.txt" },
					{ MAIN_DIR+"GeneMid.txt", MAIN_DIR+"GeneMidEntities.txt" }
				};

		TransformLearner tl = new TransformLearner(MAIN_DIR + "Gene.txt",
							   sampleSets,
							   true);

		tl.initTokenList();
		tl.runSeedRules();
		for (int times = 0; times < 5; times++)
		{
			tl.mostFrequentSetDefault();
			tl.learnNewRules();
			numRules += numRulesInc;
		}

		tl.printResults(MAIN_DIR + "report-results.txt", 1000);
	}

	/**
	 * Initializes the main token list by reading in the tokens
	 * from the supplied input file. Assumes the file has POS tagging.
	 */
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
				{
					// set initial chunks
					tok.putAttrib(PERS_ATTR, CHUNK_OUTSIDE);
					tok.putAttrib(ORG_ATTR, CHUNK_OUTSIDE);
					tok.putAttrib(LOC_ATTR, CHUNK_OUTSIDE);

					tokens.add(tok);

					// add token to name->token hash to make
					// updates by token easier
					if (nameToTokenList.containsKey(tok.getName()))
						nameToTokenList.get(tok.getName()).add(tok);
					else
					{
						this.printDebug("Creating new List of Tokens for name->token hash: " + tok.getName());
						List nlist = new LinkedList();
						nlist.add(tok);
						nameToTokenList.put(tok.getName(), nlist);
						nlist = null;
					}
				}
			}

			in.close();
		} 
		catch (IOException ioe) 
		{
			ioe.printStackTrace();
		}
	}

	/**
	 * Initializes the sample sets used to test against when creating new rules.
	 */
	private void initSampleSets(String[][] sampleSets) {

		BufferedReader in;
		Tokenizer t;
		Token tok;

		sampleTokens = new LinkedList<Token>();

		try
		{
			for (int i = 0; i < sampleSets.length; i++)
			{
				in = new BufferedReader(new FileReader(sampleSets[i][0]));
				t = new Tokenizer(in, hasPOS);

				while ((tok = t.getNext()) != null)
				{
					if (tok.getAttrib("POS") != null)
						sampleTokens.add(tok);
				}

				in = new BufferedReader(new FileReader(sampleSets[i][1]));

				String line; 

				while ((line = in.readLine()) != null)
				{
					if (line.contains("ORG"))
						this.orgP++;
					else if (line.contains("LOC"))
						this.locP++;
					else if (line.contains("PER"))
						this.perP++;
				}
			}
		}
		catch (IOException ioe)
		{ ioe.printStackTrace(); }

		printDebug("ORG N: " + orgP);
		printDebug("PER N: " + perP);
		printDebug("LOC N: " + locP);
	}

	/**
	 * This runs the default seed rules. After applying the individually
	 * tagged rules, it tags consistent rules we found in the first portion of the text.
	 */
	public void runSeedRules() {

		this.gazeteerTagging(GAZETEER);

		TripleIterator iter = new TripleIterator(tokens);
		Token[] tokenTrip;

		// we want to compare three at a time
		iter.next();

		while (iter.hasNext())
		{
			tokenTrip = iter.next();

			// "Person" of "Organization"
			if (tokenTrip[TripleIterator.CURR].getName().equalsIgnoreCase("of"))
			{
				if (tokenTrip[TripleIterator.PREV].getAttrib(PERS_ATTR).equals(CHUNK_INSIDE))
				{
					this.printDebug("PERS of ORG RULE FIRED FOR PREV ATTR: " + tokenTrip[TripleIterator.PREV].getName());
					tokenTrip[TripleIterator.NEXT].putAttrib(ORG_ATTR, CHUNK_INSIDE);
					tokenTrip[TripleIterator.NEXT].putAttrib("SEED", "T");
				}
				else if (tokenTrip[TripleIterator.NEXT].getAttrib(ORG_ATTR).equals(CHUNK_INSIDE))
				{
					this.printDebug("PERS of ORG RULE FIRED FOR NEXT ATTR: " + tokenTrip[TripleIterator.NEXT].getName());
					tokenTrip[TripleIterator.PREV].putAttrib(PERS_ATTR, CHUNK_INSIDE);
					tokenTrip[TripleIterator.PREV].putAttrib("SEED", "T");
				}
			}

			// "the" ORG1 ORG2
			if (tokenTrip[TripleIterator.PREV].getName().equalsIgnoreCase("the"))
			{
				if (Character.isUpperCase(tokenTrip[TripleIterator.CURR].getName().charAt(0)))
				{
					this.printDebug("THE ORG FIRED FROM CURR ATTR: " + tokenTrip[TripleIterator.CURR].getName());
					tokenTrip[TripleIterator.CURR].putAttrib(ORG_ATTR, CHUNK_INSIDE);
					tokenTrip[TripleIterator.CURR].putAttrib("SEED", "T");
				}

				if (Character.isUpperCase(tokenTrip[TripleIterator.NEXT].getName().charAt(0)))
				{
					this.printDebug("THE ORG FIRED FROM CURR ATTR: " + tokenTrip[TripleIterator.NEXT].getName());
					tokenTrip[TripleIterator.NEXT].putAttrib(ORG_ATTR, CHUNK_INSIDE);
					tokenTrip[TripleIterator.NEXT].putAttrib("SEED", "T");
				}
			}

			// Mr/Dr/Mrs
			if (tokenTrip[TripleIterator.PREV].getName().equals("Mr") ||
			    tokenTrip[TripleIterator.PREV].getName().equals("Dr") ||
			    tokenTrip[TripleIterator.PREV].getName().equals("Mrs"))
			{
				tokenTrip[TripleIterator.PREV].putAttrib(PERS_ATTR, CHUNK_INSIDE);
				tokenTrip[TripleIterator.PREV].putAttrib("SEED", "T");

				if (tokenTrip[TripleIterator.CURR].getAttrib("POS").equals("NP0"))
				{
					this.printDebug("Mr/Dr/Mrs rule fired for CURR ATTR: " + tokenTrip[TripleIterator.CURR].getName());
					tokenTrip[TripleIterator.CURR].putAttrib(PERS_ATTR, CHUNK_INSIDE);
					tokenTrip[TripleIterator.CURR].putAttrib("SEED", "T");
				}
				if (tokenTrip[TripleIterator.NEXT].getAttrib("POS").equals("NP0"))
				{
					this.printDebug("Mr/Dr/Mrs rule fired for NEXT ATTR: " + tokenTrip[TripleIterator.NEXT].getName());
					tokenTrip[TripleIterator.NEXT].putAttrib(PERS_ATTR, CHUNK_INSIDE);
					tokenTrip[TripleIterator.NEXT].putAttrib("SEED", "T");
				}
			}
		}
	}

	/**
	 * Set default tags by calculating the most frequent
	 * chunk tag by token.
	 */
	public void mostFrequentSetDefault() {

		Iterator<String> iter = nameToTokenList.keySet().iterator();
		Token tok;
		List tokList;
		String tokName = "";

token_loop:
		while (iter.hasNext())
		{
			int person = 0,
			    org	   = 0,
			    loc	   = 0;

			tokName = iter.next();
			tokList = nameToTokenList.get(tokName);
			Iterator<Token> tokIter = tokList.iterator();

			while (tokIter.hasNext())
			{
				tok = tokIter.next();

				// we don't want to change these here
				// due to their high term frequency
				if (tok.getName().equals("of") ||
				    tok.getName().equals("the") ||
				    tok.getName().equals("and"))
					continue token_loop;

				String ent = this.getEntityType(tok);

				if (ent != null)
				{
					if (ent.equals(LOC_ATTR))
						loc++;
					else if (ent.equals(PERS_ATTR))
						person++;
					else if (ent.equals(ORG_ATTR))
						org++;
				}
			}

			String maxEnt = null;

			if (person > org && person > loc)
				maxEnt = PERS_ATTR;
			else if (org > person && org > loc)
				maxEnt = ORG_ATTR;
			else if (loc > person && loc > org)
				maxEnt = LOC_ATTR;

			// if maxEnt is null, they were all the same
			// in which case, we'll leave all the tokens as-is for now
			if (maxEnt != null)
			{
				printDebug("Setting token " + tokName + " to default tag: " + maxEnt);
				tokIter = tokList.iterator();

				while (tokIter.hasNext())
				{
					tok = tokIter.next();
					tok.putAttrib(maxEnt, CHUNK_INSIDE);
				}
			}
		}
	}

	/**
	 * Find new rules, and apply them to the SEED tags.
	 */
	private void learnNewRules() {

		SortedSet<Rule> ruleSet = new TreeSet<Rule>(new Rule());
		Set<String> usedTokens = new HashSet<String>();
		int count = 0;

		TripleIterator iter = new TripleIterator(tokens);
		Token[] tokenTrip;

		iter.next();

		// find all the new rules
		while (iter.hasNext())
		{
			tokenTrip = iter.next();
			if (tokenTrip[1].getAttrib("SEED") != null && !usedTokens.contains(tokenTrip[1].getName()))
			{
				printDebug("Found SEED tag: " + tokenTrip[1].getName() + ", generating rules...");
				count += addAllRules(tokenTrip, ruleSet);
				printDebug(count + " rules found!");
				usedTokens.add(tokenTrip[1].getName());
			}
		}

		// apply all the new rules
		iter = new TripleIterator(tokens);
		iter.next();
		int ruleApplication = 1;
		Rule rule;
		PriorityQueue<Rule> ruleQueue = new PriorityQueue<Rule>(ruleSet);

		while (ruleApplication <= numRules)
		{
			rule = ruleQueue.poll();

			if (rule == null)
			{
				System.err.println("Out of rules to apply!");
				return;
			}

			printDebug("Applying rule #" + ruleApplication + "\n" + rule.toString());

			while (iter.hasNext())
			{
				tokenTrip = iter.next();

				if (tokenTrip[1].getAttrib("SEED") != null)
				{
					printDebug("Found SEED tag: " + tokenTrip[1].getName() + ", applying rules...");
					rule.applyRule(tokenTrip, false);
				}
			}

			ruleApplication++;
		}
	}

	/**
	 * Returns # of rule firings found and the rules added to the priority queue.
	 * Actually adds individual rules, including the ones required by the assignment.
	 */
	private int addAllRules(Token[] tokenTrip, SortedSet<Rule> ruleSet) {

		int initSize = ruleSet.size();

		Token prev = tokenTrip[0];
		Token curr = tokenTrip[1];
		Token next = tokenTrip[2];

		// POS
		Rule tmpRule = new Rule(Rule.POS_TYPE, (String) prev.getAttrib("POS"), null, null,
			       null, this.getEntityType(curr), null);
		this.addRule(tmpRule, ruleSet, this.getEntityType(curr));

		tmpRule = new Rule(Rule.POS_TYPE, null, (String) curr.getAttrib("POS"), null,
			  null, this.getEntityType(curr), null);
		this.addRule(tmpRule, ruleSet, this.getEntityType(curr));

		tmpRule = new Rule(Rule.POS_TYPE, null, null, (String) next.getAttrib("POS"),
			  null, this.getEntityType(curr), null);
		this.addRule(tmpRule, ruleSet, this.getEntityType(curr));

		// Word
		tmpRule = new Rule(Rule.WORD_TYPE, prev.getName(), null, null,
	      		  null, this.getEntityType(curr), null);
		this.addRule(tmpRule, ruleSet, this.getEntityType(curr));

		tmpRule = new Rule(Rule.WORD_TYPE, null, curr.getName(), null,
		          null, this.getEntityType(curr), null);
		this.addRule(tmpRule, ruleSet, this.getEntityType(curr));

		tmpRule = new Rule(Rule.WORD_TYPE, null, null, next.getName(),
		          null, this.getEntityType(curr), null);
		this.addRule(tmpRule, ruleSet, this.getEntityType(curr));

		// ATTR TAG
		tmpRule = new Rule(Rule.CHUNK_TYPE, this.getEntityType(prev), null, null,
		          null, this.getEntityType(curr), null);
		this.addRule(tmpRule, ruleSet, this.getEntityType(curr));

		tmpRule = new Rule(Rule.CHUNK_TYPE, null, null, this.getEntityType(next),
		          null, this.getEntityType(curr), null);
		this.addRule(tmpRule, ruleSet, this.getEntityType(curr));

		// Other Rules
		if (this.getEntityType(prev) != null)
		{
			tmpRule = new Rule(Rule.CHUNK_TYPE, this.getEntityType(prev), null, this.getEntityType(prev),
				  null, this.getEntityType(prev), null);
			this.addRule(tmpRule, ruleSet, this.getEntityType(prev));
		}

		return (ruleSet.size() - initSize);
	}

	private void addRule(Rule rule, SortedSet<Rule> ruleSet, String ent) {
		if (!ruleSet.contains(rule))
		{
			rule.setRuleProb(this.getPNP(rule, ent));
			ruleSet.add(rule);
		}
	}

	/**
	 * Returns the value for P/(P+N) as specified using
	 * the sample set to increase speed.
	 */
	private double getPNP(Rule rule, String ent) {

		double np = 0.0;
		double p = 0.0;

		if (ent.equals("LOC"))
			p = locP;
		else if (ent.equals("ORG"))
			p = orgP;
		else if (ent.equals("PER"))
			p = perP;

		TripleIterator iter = new TripleIterator(sampleTokens);
		iter.next();
		
		while (iter.hasNext())
			if (rule.applyRule(iter.next(), true))
				np++;

		printDebug("P value: " + p);
		printDebug("P+N value: " + np);

		return p / np;
	}

	/**
	 * gazStrings represents an array of strings that hold 
	 * gazeteer's for common phrases with their tag type included,
	 * i.e: ["United", "States", "L"] represents a gazTuple of the
	 * phrase "United States", with "L" flagging it as a location.
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
					{
						tokenTrip[TripleIterator.NEXT].putAttrib(gazTuple[gazTuple.length - 1], CHUNK_INSIDE);
						tokenTrip[TripleIterator.NEXT].putAttrib("SEED", "T");
					}
					break;

					case 2: 
					if (tokenTrip[TripleIterator.CURR] == null)
						break;

					if (gazTuple[0].equals(tokenTrip[TripleIterator.CURR].getName()) &&
					    gazTuple[1].equals(tokenTrip[TripleIterator.NEXT].getName()))
					{
						tokenTrip[TripleIterator.CURR].putAttrib(gazTuple[gazTuple.length - 1], CHUNK_INSIDE);
						tokenTrip[TripleIterator.NEXT].putAttrib(gazTuple[gazTuple.length - 1], CHUNK_INSIDE);

						tokenTrip[TripleIterator.CURR].putAttrib("SEED", "T");
						tokenTrip[TripleIterator.NEXT].putAttrib("SEED", "T");
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
						tokenTrip[TripleIterator.PREV].putAttrib(gazTuple[gazTuple.length - 1], CHUNK_INSIDE);
						tokenTrip[TripleIterator.CURR].putAttrib(gazTuple[gazTuple.length - 1], CHUNK_INSIDE);
						tokenTrip[TripleIterator.NEXT].putAttrib(gazTuple[gazTuple.length - 1], CHUNK_INSIDE);

						tokenTrip[TripleIterator.PREV].putAttrib("SEED", "T");
						tokenTrip[TripleIterator.CURR].putAttrib("SEED", "T");
						tokenTrip[TripleIterator.NEXT].putAttrib("SEED", "T");
					}
					break;
				}
			}

		} while ((tokenTrip = iter.next()) != null);
	}

	/**
	 * Prints the results to a file of the last 1000 tokens.
	 */
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
					   this.getEntityType(tok));
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

	/**
	 * Returns the entity type of a specified token.
	 */
	public static String getEntityType(Token tok) {

		if (tok.getAttrib(PERS_ATTR).equals(CHUNK_INSIDE))
			return PERS_ATTR;
		else if (tok.getAttrib(ORG_ATTR).equals(CHUNK_INSIDE))
			return ORG_ATTR;
		else if (tok.getAttrib(LOC_ATTR).equals(CHUNK_INSIDE))
			return LOC_ATTR;

		return null;
	}

	/**
	 * Prints debugging statements if debug is set to true.
	 * Assignment is turned in with debugging off.
	 */
	protected static void printDebug(String debugString) {
		if (debug)
		{
			System.out.println(debugString);
		}
	}

	/**
	 * Utility class to iterate through the tokens 3 tokens at a time.
	 * This makes neighborhood comparison easier.
	 */
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

		public TripleIterator(List<Token> tokens, int startVal) {
			this.tokens = tokens;
			prev = null;
			curr = null;
			next = tokens.get(startVal);
			indexNext = startVal + 1;
		}

		public Token[] getCurrent() {
			return new Token[] {prev, curr, next};
		}

		public Token[] next() {
			if (this.hasNext()) 
			{
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
