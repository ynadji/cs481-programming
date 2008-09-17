package nlp.token;

import java.io.*;

/*
   State table, x=output
   State Desc           Space Other Alpha Punc EOF
   0     Initial          3    4     1     5   x7
   1     Word            x3   x4     1     2   x7
   2     WordWithPunc    x6   x6     1    x6   x6
   3     Space            3   x4    x1    x5   x7
   4     Other           x3    4    x1    x5   x7
   5     Punc            x3   x4    x1    x5   x7
   <no read after>
   6     PuncAfterWord   x3   x4    x1    x5   x7 (6 if longer)
   7     EOF             x7   x7    x7    x7   x7
 */

/**
 * Tokenize an input stream.
 * This is taken from ATMan.
 *
 * <pre>
 * Typical use:
 * Tokenizer t = new Tokenizer(reader);
 * Token tok;
 * while((tok = t.getNext()) != null)
 * {
 * }
 * </pre>
 *
 * @author Sterling Stuart Stein
 *   with contributions by Paul Chase and Ken Bloom.
 */
public class Tokenizer
{
   /**
    * The stream being read from
    */
   protected Reader reader = null;

   /**
    * The accumulator for the current token
    */
   protected StringBuffer cur = null;

   /**
    * The accumulator for the next token
    */
   protected StringBuffer next = null;

   /**
    * The next position
    */
   protected int nextpos = 0;

   /**
    * The state in the finite state machine
    */
   protected int state = 0;

   /**
    * The type of each character
    */
   protected byte[] ctype = null;

   /**
    * The type of the current character
    */
   protected byte curtype = 0;

   /**
    * Character type constant: white space
    */
   protected static final byte CT_SPACE = 1;

   /**
    * Character type constant: other
    */
   protected static final byte CT_OTHER = 2;

   /**
    * Character type constant: alphanumeric
    */
   protected static final byte CT_ALPHA = 3;

   /**
    * Character type constant: punctuation
    */
   protected static final byte CT_PUNC = 4;

   /**
    * Character type constant: end of file
    */
   protected static final byte CT_EOF = 5;


    /**
     * boolean parameter: read POS (slash-separated) or not
     */

    boolean read_POS = false;

   /**
    * Testing program.  Tokenizes "test.txt" to stdout.
    *
    * @param argv Ignored
    */
   public static void main(String[] argv)
   {
      Reader z;
      Token  tk;

      try
      {
         z = new BufferedReader(new FileReader(argv[0]));
      }
      catch(Exception e)
      {
         System.err.println("Opening " + argv[0] + " failed.");
         e.printStackTrace();

         return;
      }

	  /* Create a tokenizer that reads slash-separated parts-of-speech */
      Tokenizer t = new Tokenizer(z,true);

      try
      {
	  int numtok = 0;
         while((tk = t.getNext()) != null)
         {
            System.out.println("Tok: " + tk.getName() + " POS " + tk.getAttrib("POS"));
	    if (tk.getAttrib("POS") != null) {
		numtok++;
	    }
         }
	 System.out.println("TOTAL Tokens: " + numtok);
      }
      catch(Exception e)
      {
         System.err.println("Read error.");
         e.printStackTrace();
      }
   }

   /**
    * Makes a tokenizer that reads from the given reader
    *
    * @param r Where to read from
    */
    public Tokenizer(Reader r)
   {
      if(r == null)
      {
         throw new NullPointerException();
      }

      reader = r;

      init();
   }

   /**
    * Makes a tokenizer that reads from the given reader
    *
    * @param r Where to read from
	* @param getPOS Whether the tokenizer should read in parts-of-speech for each token, separated by a slash, as in:<BR>
	* <TT>the/DT0 books/NN2 are/VBZ on/PRP the/DT0 table/NN1 ./PUN </tt>
    */
	public Tokenizer(Reader r, boolean getPOS)
   {
      if(r == null)
      {
         throw new NullPointerException();
      }

      reader = r;
      read_POS = getPOS;

      init();
   }

   /**
    * Initialize the tokenizer, notably the character type array.
    */
   protected void init()
   {
      nextpos    = 0;
      state      = 0;
      curtype    = CT_SPACE;
      cur        = new StringBuffer(20);
      next       = new StringBuffer(20);
      ctype      = new byte[65536];

      setChars(0, 65535, CT_OTHER);  //Default
      setChars(0, 32, CT_SPACE);

      setChars('a', 'z', CT_ALPHA);
      setChars('A', 'Z', CT_ALPHA);
      setChars('0', '9', CT_ALPHA);

      setChars('!', '/', CT_PUNC);
      setChars('/', '/', CT_OTHER);
      setChars(':', '@', CT_PUNC);
      setChars('[', ']', CT_PUNC);
      setChars('{', '~', CT_PUNC);
   }

   /**
    * Sets the range of characters to the same type
    *
    * @param low  Where the range starts
    * @param hi   Where the range ends
    * @param type The type to set it to
    */
   public void setChars(int low, int hi, byte type)
   {
      while(low <= hi)
      {
         ctype[low++] = type;
      }
   }

   /**
    * Gets the next token from the stream
    *
    * @return The next token from the stream or null if it the stream has ended
    */
    static Token get_buffer = (Token)null;
   public Token getNext() throws IOException
    {
	if (read_POS) {
	    Token t = (get_buffer==null)?internalGetNext():get_buffer;
	    get_buffer = null;
	    if (t == null) {
		return t;
	    }
	    if (t.getName().length() == 0) {
		return t;
	    } else {
		Token sep = internalGetNext();
		if (sep == null) {
		    return t;
		}
		if (sep.getName().charAt(0) == '/') {
		    Token pos = internalGetNext();
		    t.putAttrib("POS",pos.getName());
		    return t;
		} else {
		    get_buffer = sep;
		    return t;
		}
	    }
	} else {
	    return internalGetNext();
	}
    }
    

   protected Token internalGetNext() throws IOException
   {
      int     c   = 0;  //Character that was read
      boolean ret = false;  //Should return
      Token   tok = null;

      if(state == 7)
      {  //No more tokens

         return null;
      }

      while(true)
      {
         if(state < 6)
         {  //Read next character
            c = reader.read();

            if(c < 0 || c > 65535)
            {
               reader.close();
               c          = 0;
               curtype    = CT_EOF;
            }
            else
            {
               curtype = ctype[c];  //Get type of character
            }
         }

         //Use state machine
         switch(state)
         {
            case 1:  //Word

               if(curtype != CT_ALPHA)
               {
                  if(curtype == CT_PUNC)
                  {
                     next.append((char)c);
                     state = 2;

                     continue;
                  }

                  tok = new Token(Token.TT_WORD, cur.toString());
                  cur.setLength(0);  //Erase old token

                  if(curtype == CT_EOF)
                  {
                     state = 7;
                  }
                  else
                  {
                     cur.append((char)c);

                     if(curtype == CT_SPACE)
                     {
                        state = 3;
                     }
                     else
                     {
                        state = 4;  //CT_OTHER
                     }
                  }

                  return tok;
               }

               break;

            case 2:  //Word with punctuation

               switch(curtype)
               {
                  case CT_SPACE:
                  case CT_OTHER:
                  case CT_EOF:
                  case CT_PUNC:
                     tok = new Token(Token.TT_WORD, cur.toString());
                     cur.setLength(0);  //cur=next
                     cur.append(next);
                     next.setLength(0);  //next=c

                     if(curtype != CT_EOF)
                     {
                        next.append((char)c);
                     }

                     state = 6;

                     return tok;

                  case CT_ALPHA:
                     state = 1;
                     cur.append(next);
                     cur.append((char)c);
                     next.setLength(0);

                     continue;
               }

               break;

            case 3:  //Space

               if(curtype != CT_SPACE)
               {
                  tok = new Token(Token.TT_SPACE, cur.toString());
                  cur.setLength(0);
                  ret = true;
               }

               break;

            case 4:  //Other

               if(curtype != CT_OTHER)
               {
                  tok = new Token(Token.TT_OTHER, cur.toString());
                  cur.setLength(0);
                  ret = true;
               }

               break;

            case 5:  //Punctuation
               tok = new Token(Token.TT_PUNC, cur.toString());
               cur.setLength(0);
               ret = true;

               break;

            case 6:  //Punctuation after word

               if(cur.length() > 1)
               {
                  tok = new Token(Token.TT_PUNC, cur.substring(0, 1));
                  cur.deleteCharAt(0);

                  return tok;
               }

               tok = new Token(Token.TT_PUNC, cur.toString());
               cur.setLength(0);  //cur=next
               cur.append(next);
               next.setLength(0);  //next=""
               ret = true;

               break;

            case 7:
               ret = true;

               break;
         }

         //Use defaults
         if(curtype != CT_EOF && state < 6)
         {
            cur.append((char)c);
         }

         switch(curtype)
         {
            case CT_SPACE:
               state = 3;

               break;

            case CT_OTHER:
               state = 4;

               break;

            case CT_ALPHA:
               state = 1;

               break;

            case CT_PUNC:
               state = 5;

               break;

            case CT_EOF:
               state = 7;

               break;
         }

         if(ret)
         {
            return tok;
         }
      }
   }
}
