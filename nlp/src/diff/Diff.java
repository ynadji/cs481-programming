package nlp.diff;

import java.util.*;

/**
 * Find the difference between 2 arrays
 * using the standard dynamic programming algorithm.
 *
 * <pre>
 * Typical use:
 * Object[] o1 = {new Character('a')};
 * Object[] o2 = {new Character('b')};
 * System.out.println("Diff = " + diff(o1, o2));
 * printdiff(o1, o2);
 * </pre>
 *
 * @author Sterling Stuart Stein
 */
public class Diff
{
   /**
    * Find the number of differences between 2 arrays counting insertions, deletions, and changes.
    *
    * @param a The first array
    * @param b The second array
    * @return The number of changes to convert a to b
    */
   public static int diff(Object[] a, Object[] b)
   {
      //Swap a and b if it would save memory
      if(b.length < a.length)
      {
         Object[] t = a;
         a    = b;
         b    = t;
      }

      int[] d1 = new int[a.length + 1];
      int[] d2 = new int[a.length + 1];

      for(int i = 0; i < d1.length; i++)
      {
         d1[i] = i;
      }

      for(int i = 0; i < b.length; i++)
      {
         d2[0] = i + 1;

         for(int j = 1; j < d2.length; j++)
         {
            int cost = (b[i].equals(a[j - 1])) ? 0 : 1;

            int min;
            int v;
            v        = d1[j] + 1;  //Delete
            min      = v;
            v        = d2[j - 1] + 1;  //Insert

            if(v < min)
            {
               min = v;
            }

            v = d1[j - 1] + cost;  //Substitute

            if(v < min)
            {
               min = v;
            }

            d2[j] = min;
         }
         //Swap d1 and d2
         {
            int[] t = d1;
            d1    = d2;
            d2    = t;
         }
      }

      return d1[d1.length - 1];
   }

   /**
    * Find the differences between 2 arrays counting insertions, deletions, and changes.
    *
    * @param a The first array
    * @param b The second array
    * @return An array of pairs of the index of the elements from a and b with -1 representing not present (delete/insert)
    */
   public static int[] difflist(Object[] a, Object[] b)
   {
      boolean swapped = false;

      //Swap a and b if it would save memory
      if(b.length < a.length)
      {
         Object[] t = a;
         a          = b;
         b          = t;
         swapped    = true;
      }

      IntVector[] d1 = new IntVector[a.length + 1];
      IntVector[] d2 = new IntVector[a.length + 1];
      d1[0]          = new IntVector();

      for(int i = 1; i < d1.length; i++)
      {
         d1[i] = (IntVector)d1[i - 1].clone();
         d1[i].add(i - 1);
         d1[i].add(-1);
      }

      for(int i = 0; i < b.length; i++)
      {
         d2[0] = (IntVector)d1[0].clone();
         d2[0].add(-1);
         d2[0].add(i);

         for(int j = 1; j < d2.length; j++)
         {
            boolean   eq  = b[i].equals(a[j - 1]);

            IntVector min;
            IntVector v;
            v             = (IntVector)d1[j].clone();  //Delete
            v.add(-1);
            v.add(i);
            min    = v;

            v      = (IntVector)d2[j - 1].clone();  //Insert
            v.add(j - 1);
            v.add(-1);

            if(v.length() < min.length())
            {
               min = v;
            }

            v = (IntVector)d1[j - 1].clone();  //Substitute

            if(!eq)
            {
               v.add(j - 1);
               v.add(i);
            }

            if(v.length() < min.length())
            {
               min = v;
            }

            d2[j] = min;
         }
         //Swap d1 and d2
         {
            IntVector[] t = d1;
            d1    = d2;
            d2    = t;
         }
      }

      //Switch around result
      int[] res = d1[d1.length - 1].toArray();

      if(swapped)
      {
         for(int i = 0; i < res.length; i += 2)
         {
            int t = res[i];
            res[i]        = res[i + 1];
            res[i + 1]    = t;
         }
      }

      return res;
   }

   /**
    * Convert a String into a Character array for debugging purposes.
    *
    * @param s The String to be converted
    * @return An array of Characters that is equivalent to the String
    */
   public static Character[] conv(String s)
   {
      Character[] x = new Character[s.length()];

      for(int i = 0; i < x.length; i++)
      {
         x[i] = s.charAt(i);  //Autoboxed
      }

      return x;
   }

   /**
    * Print out the list of differences from difflist
    *
    * @param x The first array
    * @param y The second array
    */
   public static void printdiff(Object[] x, Object[] y)
   {
      int[] diff = difflist(x, y);

      for(int i = 0; i < diff.length; i += 2)
      {
         System.out.println(diff[i] + " " + diff[i + 1] + " =");

         if(diff[i + 1] < 0)
         {
            System.out.println("<  " + x[diff[i]]);
         }
         else if(diff[i] < 0)
         {
            System.out.println(">  " + y[diff[i + 1]]);
         }
         else
         {
            System.out.println("<- " + x[diff[i]]);
            System.out.println("-> " + y[diff[i + 1]]);
         }
      }
   }

   /**
    * Show differences between 2 Strings, for debugging.
    *
    * @param argv The 2 Strings
    */
   public static void main(String[] argv)
   {
      if(argv.length != 2)
      {
         System.err.println("Error: 2 arguments needed");

         return;
      }

      Character[] x = conv(argv[0]);
      Character[] y = conv(argv[1]);

      System.out.println("Diff = " + diff(x, y));
      printdiff(x, y);
   }
}
