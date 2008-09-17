package nlp.diff;


/**
 * A Vector of ints since there is no such thing as Vector&lt;int&gt;.
 *
 * <pre>
 * Typical use:
 * IntVector iv = new IntVector();
 * iv.add(0);
 * for(int i=0;i&lt;iv.length();i++)
 * {
 *   int x=iv.get(i);
 * }
 * </pre>
 *
 * @author Sterling Stuart Stein
 */
public class IntVector
{
   /**
    * The array of integers
    */
   protected int[] array;

   /**
    * The length of the array
    */
   protected int length;

   /**
    * Make an empty vector
    */
   public IntVector()
   {
      this(4);
   }

   /**
    * Make an empty vector of given capacity
    *
    * @param cap The Initial capacity of the vector
    */
   public IntVector(int cap)
   {
      array     = null;
      length    = 0;
      ensureCapacity(cap);
   }

   /**
    * Make an empty vector with the given length and containing the given array
    *
    * @param len The length of the new vector
    * @param a   The array to copy
    */
   public IntVector(int len, int[] a)
   {
      array = null;

      int cap = a.length;

      if(cap < len)
      {
         cap = len;
      }

      ensureCapacity(cap);
      length = len;

      int min = len;

      if(a.length < min)
      {
         min = a.length;
      }

      for(int i = 0; i < min; i++)
      {
         array[i] = a[i];
      }
   }

   /**
    * Clone the vector.
    *
    * @return A copy of the vector
    */
   public Object clone()
   {
      return new IntVector(length, array);
   }

   /**
    * Get the length of the vector.
    *
    * @return The length of the vector
    */
   public int length()
   {
      return length;
   }

   /**
    * Get the capacity of the vector.
    *
    * @return The capacity of the vector
    */
   public int capacity()
   {
      return array.length;
   }

   /**
    * Make the vector able to handle at least the given number of elements without needing to resize.
    *
    * @param c The capacity to make sure the vector has
    */
   public void ensureCapacity(int c)
   {
      if(c < 1)
      {
         c = 1;
      }

      int cap = 0;

      if(array != null)
      {
         cap = array.length;
      }

      if(cap < c)
      {
         int[] old = array;
         array = new int[c];

         if(old != null)
         {
            for(int i = 0; i < length; i++)
            {
               array[i] = old[i];
            }
         }
      }
   }

   /**
    * Add a new integer to the end of the vector.
    *
    * @param x The integer to be added
    */
   public void add(int x)
   {
      if(length >= array.length)
      {
         int cap = 2 * array.length;

         if(length > cap)
         {
            cap = length;
         }

         ensureCapacity(cap);
      }

      array[length] = x;
      length++;
   }

   /**
    * Get the vector at the given index.
    *
    * @param i The index to grab it from
    * @return The integer at that index
    */
   public int get(int i)
   {
      if(i < 0 || i >= length)
      {
         throw new ArrayIndexOutOfBoundsException(i);
      }

      return array[i];
   }

   /**
    * Set the value in the vector at the given index.
    *
    * @param i The index to grab it from
    * @param x The value to set it to
    */
   public void set(int i, int x)
   {
      if(i < 0 || i >= length)
      {
         throw new ArrayIndexOutOfBoundsException(i);
      }

      array[i] = x;
   }

   /**
    * Convert the vector into a plain integer array.
    *
    * @return The vector as an array
    */
   public int[] toArray()
   {
      int[] a = new int[length];

      for(int i = 0; i < length; i++)
      {
         a[i] = array[i];
      }

      return a;
   }
}
