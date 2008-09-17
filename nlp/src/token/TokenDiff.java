package nlp.token;

import nlp.diff.*;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

/**
 * Find the difference between 2 token files.
 * Run from the commandline.
 *
 * @author Sterling Stuart Stein
 */
public class TokenDiff
{
   /**
    * Convert the vector of paragraphs into an array of Strings compatible with Diff
    *
    * @param v The vector of paragraphs
    * @return An array of Objects representing the tokens in the paragraphs
    */
   public static Object[] tokenArray(Vector v)
   {
      Vector a = new Vector();

      for(Iterator i = v.iterator(); i.hasNext();)
      {
         a.add("Separator: Paragraph");

         Vector s = (Vector)i.next();

         for(Iterator j = s.iterator(); j.hasNext();)
         {
            a.add("Separator: Sentence");

            Vector t = (Vector)j.next();

            for(Iterator k = t.iterator(); k.hasNext();)
            {
               Token tok = (Token)k.next();
               a.add("Token: " + tok.getName());

               HashMap  h    = tok.getHashMap();
               Object[] keys = h.keySet().toArray();
               Arrays.sort(keys);

               for(int l = 0; l < keys.length; l++)
               {
                  String key = (String)keys[l];
                  a.add("Attribute: " + key + " -> " + h.get(key));
               }
            }
         }
      }

      return a.toArray();
   }

   /**
    * Load the 2 files and compare them
    *
    * @param argv The names of the 2 file
    */
   public static void main(String[] argv) throws Exception
   {
      if(argv.length != 2)
      {
         System.err.println("Error: wrong number of arguments");
         System.err.println(
            "Format:  java nlp.token <file 1> <file 2>");
         System.err.println(
            "Example: java nlp.token answers.xml trail.xml");
         System.exit(1);
      }

      Object[] t1  = tokenArray(Token.readXML(
               new BufferedInputStream(new FileInputStream(argv[0]))));
      Object[] t2  = tokenArray(Token.readXML(
               new BufferedInputStream(new FileInputStream(argv[1]))));
      int      len = t1.length;

      if(t2.length > len)
      {
         len = t2.length;
      }

      int d = Diff.diff(t1, t2);
      d = len - d;
      System.out.println("Similarity = " + d + " / " + len + " = " +
         ((100.0 * d) / len) + " %");
      Diff.printdiff(t1, t2);
   }
}
