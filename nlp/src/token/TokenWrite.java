package nlp.token;

import nlp.diff.*;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

/**
 * Read in text from stdin, tokenize it, and write it out as XML to stdout.
 * Run from the commandline.
 *
 * @author Sterling Stuart Stein
 */
public class TokenWrite
{
   /**
    * Count how many newlines are in a string
    *
    * @param s The string to be counted from
    * @return How many newlines it contains
    */
   public static int countnewlines(String s)
   {
      int count = 0;

      for(int i = 0; i < s.length(); i++)
      {
         if(s.charAt(i) == '\n')
         {
            count++;
         }
      }

      return count;
   }

   /**
    * Read the tokens from the given stream
    *
    * @param in The stream to read from
    * @return Vector of paragraphs of sentences of tokens
    */
   public static Vector readTokens(InputStream in) throws Exception
   {
      Tokenizer t     = new Tokenizer(new BufferedReader(
               new InputStreamReader(in)));
      Vector    paras = new Vector();
      Vector    sents = new Vector();
      Vector    toks  = new Vector();

      Token     tok;

      while((tok = t.getNext()) != null)
      {
         if(tok.getType() == Token.TT_SPACE)
         {
            if(countnewlines(tok.getName()) > 1)
            {
               if(toks.size() > 0)
               {
                  sents.add(toks);
                  toks = new Vector();
               }

               if(sents.size() > 0)
               {
                  paras.add(sents);
                  sents = new Vector();
               }
            }
            else
            {
               //Ignore spaces
            }
         }
         else
         {
            TokAttr.annotate(tok);
            toks.add(tok);

            if(tok.getType() == Token.TT_PUNC)
            {
               char c = tok.getName().charAt(0);

               if(c == '.' || c == '?' || c == '!')
               {
                  sents.add(toks);
                  toks = new Vector();
               }
            }
         }
      }

      if(toks.size() > 0)
      {
         sents.add(toks);
      }

      if(sents.size() > 0)
      {
         paras.add(sents);
      }

      return paras;
   }

   /**
    * Read in text from stdin, tokenize it, and write it out as XML to stdout.
    *
    * @param argv Ignored
    */
   public static void main(String[] argv) throws Exception
   {
      Vector paras = readTokens(System.in);
      Token.writeXML(paras, System.out);
   }
}
