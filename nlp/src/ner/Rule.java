package nlp.ner;

import nlp.token.*;
import java.util.*;

public class Rule implements Comparator {

	public static final int WORD_TYPE = 0;
	public static final int POS_TYPE = 1;
	public static final int CHUNK_TYPE = 2;

	public int ruleType;

	private double ruleProb;

	public String prev;
	public String curr;
	public String next;

	public String altPrev;
	public String altCurr;
	public String altNext;

	public Rule(int ruleType, String prev, String curr, String next,
				  String altPrev, String altCurr, String altNext)
	{
		this.ruleType = ruleType;
		this.prev = prev;
		this.curr = curr;
		this.next = next;
		this.altPrev = altPrev;
		this.altCurr = altCurr;
		this.altNext = altNext;

		this.ruleProb = 0.0;
	}

	/**
	 * Applies a rule to the specified tokenTrip if it fits.
	 * bool pretend allows us to quickly check over the document
	 * for values of p to determine if the rule would fire.
	 */
	public boolean applyRule(Token[] tokenTrip, boolean pretend) {

		Token prevTok = tokenTrip[0];
		Token currTok = tokenTrip[1];
		Token nextTok = tokenTrip[2];

		if (this.ruleType != CHUNK_TYPE)
		{
			String attrib = "";

			switch (this.ruleType)
			{
				case WORD_TYPE:
					attrib = "NAME";
					break;

				case POS_TYPE:
					attrib = "POS";
					break;
			}

			if ( (prev == null || prevTok.getAttrib(attrib).equals(prev)) &&
			     (curr == null || currTok.getAttrib(attrib).equals(curr)) &&
			     (next == null || nextTok.getAttrib(attrib).equals(next)))
			{
				if (pretend)
				{
					//TransformLearner.printDebug("Pretend rule firing!\n" + this.toString());
					return true;
				}

				TransformLearner.printDebug(this.toString());
				if (altPrev != null)
					prevTok.putAttrib(altPrev, "I");
				if (altCurr != null)
					currTok.putAttrib(altCurr, "I");
				if (altNext != null)
					nextTok.putAttrib(altNext, "I");
			}
		}
		else
		{
			if ( (prev == null || prevTok.getAttrib(prev).equals("I")) &&
			     (curr == null || currTok.getAttrib(curr).equals("I")) &&
			     (next == null || nextTok.getAttrib(next).equals("I")))
			{
				if (pretend)
				{
					//TransformLearner.printDebug("Pretend rule firing!\n" + this.toString());
					return true;
				}

				TransformLearner.printDebug(this.toString());
				if (altPrev != null)
					prevTok.putAttrib(altPrev, "I");
				if (altCurr != null)
					currTok.putAttrib(altCurr, "I");
				if (altNext != null)
					nextTok.putAttrib(altNext, "I");
			}
		}

		return false;
	}

	public void setRuleProb(double ruleProb) {
		this.ruleProb = ruleProb;
	}

	public double getRuleProb() {
		return this.ruleProb;
	}

	public int compare(Object obj1, Object obj2) {
		return 0;
		/**
		Rule rule1 = (Rule) obj1;
		Rule rule2 = (Rule) obj2;

		return Double.compare(rule1.getRuleProb(),
				      rule2.getRuleProb());
				      */
	}

	public boolean equals(Object obj) {
		Rule rule = (Rule) obj;

		return this.toString().equals(rule.toString());
	}

	public String toString() {
		return "RULE: IF ( " 
			+ prev + ", " + curr + ", " + next + " ) THEN ( " 
			+ altPrev + ", " + altCurr + ", " + altNext + " )";
	}
}
