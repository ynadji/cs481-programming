/*
   Sterling Stuart Stein
   Token Attributes
   Attributes for a token
 */
package nlp.token;

import java.util.*;

/**
 * Set the attributes for a token
 *
 * <pre>
 * Typical use:
 * Token tok = new Token(Token.TT_WORD,"word");
 * TokAttr.annotate(tok);
 * </pre>
 *
 * @author Sterling Stuart Stein
 */
public class TokAttr
{
   /**
    * Constant for capitalization: it is not a word
    */
   public static final String OTHER = "other";

   /**
    * Constant for capitalization: it is lowercase
    */
   public static final String LOWERCASE = "lowercase";

   /**
    * Constant for capitalization: it is all uppercase
    */
   public static final String ALLUPPER = "allupper";

   /**
    * Constant for capitalization: the first letter is uppercase and the rest are lowercase
    */
   public static final String FIRSTUPPER = "firstupper";

   /**
    * Constant for capitalization: the case is mixed
    */
   public static final String MIXEDCASE = "mixedcase";

   /**
    * Annotate a token with attributes
    *
    * @param t The token to be annotated
    */
   static public void annotate(Token t)
   {
      String s = t.getName();
      t.putAttrib("cap", capitalization(s));
      t.setName(s.toLowerCase());
   }

   /**
    * Find the capitalization of a string
    *
    * @param s The string to be examined
    */
   static public String capitalization(String s)
   {
      String lc = s.toLowerCase();  //Should handle encodings this way
      String uc = s.toUpperCase();

      if(s.length() > 0)
      {
         char l     = lc.charAt(0);
         char u     = uc.charAt(0);
         char n     = s.charAt(0);

         int  first = 0;

         if(n == l && n == u)
         {
            return OTHER;
         }

         if(n == l)
         {
            first = 1;
         }
         else
         {
            first = 2;
         }

         if(s.length() == 1)
         {
            if(first == 1)
            {
               return LOWERCASE;
            }

            if(first == 2)
            {
               return FIRSTUPPER;
            }
         }
         else
         {
            boolean haslower = false;
            boolean hasupper = false;

            for(int i = 1; i < s.length(); i++)
            {
               l    = lc.charAt(i);
               u    = uc.charAt(i);
               n    = s.charAt(i);

               if(n == l && n == u)
               {
                  continue;
               }

               if(n == l)
               {
                  haslower = true;
               }
               else
               {
                  hasupper = true;
               }
            }

            if((haslower || first == 1) && hasupper)
            {
               return MIXEDCASE;
            }

            if(first == 2 && hasupper && !haslower)
            {
               return ALLUPPER;
            }

            if(first == 2 && !hasupper)
            {
               return FIRSTUPPER;
            }

            return LOWERCASE;
         }
      }

      return OTHER;
   }
}
