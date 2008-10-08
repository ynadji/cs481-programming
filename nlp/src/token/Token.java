package nlp.token;

import nlp.diff.*;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

/**
 * A class for storing a token.
 * It is a String and the associated attributes.
 *
 * <pre>
 * Typical use:
 * Token  tok = new Token(Token.TT_WORD,"word");
 * String s   = tok.getName();
 * Vector v   = Token.readXML(System.in);
 * </pre>
 *
 * @author Sterling Stuart Stein
 */
public class Token
{
   /**
    * Constants for types of tokens: end of file
    */
   public static final int TT_EOF = -1;

   /**
    * Constants for types of tokens: white space
    */
   public static final int TT_SPACE = -2;

   /**
    * Constants for types of tokens: word
    */
   public static final int TT_WORD = -3;

   /**
    * Constants for types of tokens: punctuation
    */
   public static final int TT_PUNC = -4;

   /**
    * Constants for types of tokens: other
    */
   public static final int TT_OTHER = -5;

   /**
    * The type of this token
    */
   protected int type;

   /**
    * The token itself
    */
   protected String val;

   /**
    * The attributes of this token
    */
   protected HashMap attribs;

   /**
    * Make an empty token
    */
   public Token()
   {
      type       = TT_WORD;
      val        = "";
      attribs    = new HashMap();
   }

   /**
    * Make a given token
    *
    * @param t The token's type
    * @param v The token itself
    */
   public Token(int t, String v)
   {
      type       = t;
      val        = v;
      attribs    = new HashMap();
   }

   /**
    * Convert the token to a string
    *
    * @return The token
    */
   public final String toString()
   {
      return val;
   }

   /**
    * Get the token as a string
    *
    * @return The token
    */
   public final String getName()
   {
      return val;
   }

   /**
    * Change the token
    *
    * @param v the token to change it to
    */
   public final void setName(String v)
   {
      val = v;
   }

   /**
    * Get the type of a token
    *
    * @return Get the token's type
    */
   public final int getType()
   {
      return type;
   }

   /**
    * Append the given token this token
    *
    * @param t The token to be appended
    */
   public final void append(Token t)
   {
      val += t.val;
   }

   /**
    * Add an attribute to this token
    *
    * @param name The name of the attribute to be added
    * @param value The value to set the attribute to
    */
   public final void putAttrib(String name, Object value)
   {
	   // if we're updating LOC/ORG/PERS chunk tags
	   // reset all the other values so we only
	   // have one at a time
	   if (name.equals("LOC") ||
	       name.equals("ORG") ||
	       name.equals("PESR"))
	   {
		   attribs.put("LOC", "O");
		   attribs.put("ORG", "O");
		   attribs.put("PERS", "O");
	   }
      attribs.put(name, value);
   }

   /**
    * Get an attribute from this token
    *
    * @param name The name of the attribute to be added
    * @return The value of the attribute
    */
   public final Object getAttrib(String name)
   {
	   if (name.equals("NAME"))
		   return this.getName();

      return attribs.get(name);
   }

   public final boolean containsAttrib(String name)
   {
	   return attribs.containsKey(name);
   }

   /**
    * Get all of the attributes as a HashMap
    *
    * @return A HashMap containing all of the attributes
    */
   public final HashMap getHashMap()
   {
      return attribs;
   }

   /**
    * Set all of the attributes from a HashMap
    *
    * @param hm The HashMap to set the attributes from
    */
   public final void putHashMap(HashMap hm)
   {
      attribs = hm;
   }

   /**
    * Write out the tokens as XML
    *
    * @param paras A vector of paragraphs of sentences of Tokens to be written
    * @param out The stream to write to
    */
   public static void writeXML(Vector paras, OutputStream out)
      throws Exception
   {
      Document xml  = XML.blank();
      Element  root = xml.createElement("doc");
      xml.appendChild(root);

      for(Iterator i = paras.iterator(); i.hasNext();)
      {
         Vector  sents = (Vector)i.next();
         Element p     = xml.createElement("p");

         for(Iterator j = sents.iterator(); j.hasNext();)
         {
            Vector  toks = (Vector)j.next();
            Element s    = xml.createElement("s");

            for(Iterator k = toks.iterator(); k.hasNext();)
            {
               Token   tok = (Token)k.next();
               Element t   = xml.createElement("t");
               t.appendChild(xml.createTextNode(tok.getName()));

               HashMap h = tok.getHashMap();

               for(Iterator l = h.keySet().iterator(); l.hasNext();)
               {
                  String key = (String)l.next();
                  String val = (String)h.get(key);
                  Attr   a   = xml.createAttribute(key);
                  a.setValue(val);
                  t.setAttributeNode(a);
               }

               s.appendChild(t);
            }

            p.appendChild(s);
         }

         root.appendChild(p);
      }

      XML.write(xml, out);
   }

   /**
    * Read the tokens from XML
    *
    * @param in The stream to read from
    * @return A vector of paragraphs of sentences of Tokens
    */
   public static Vector readXML(InputStream in) throws Exception
   {
      Document doc = XML.parse(in);

      NodeList pl  = doc.getChildNodes();

      if(pl.getLength() != 1)
      {
         throw new Exception("Not 1 root element in XML");
      }

      Node n = pl.item(0);

      if(!n.getNodeName().equals("doc"))
      {
         throw new Exception("Wrong root element in XML");
      }

      pl = n.getChildNodes();

      Vector pv  = new Vector();

      int    pll = pl.getLength();

      for(int i = 0; i < pll; i++)
      {
         Node o = pl.item(i);

         if(!o.getNodeName().equals("p"))
         {
            throw new Exception("Expecting tag p in XML");
         }

         NodeList sl  = o.getChildNodes();
         Vector   sv  = new Vector();

         int      sll = sl.getLength();

         for(int j = 0; j < sll; j++)
         {
            Node p = sl.item(j);

            if(!p.getNodeName().equals("s"))
            {
               throw new Exception("Expecting tag s in XML");
            }

            NodeList tl  = p.getChildNodes();
            Vector   tv  = new Vector();

            int      tll = tl.getLength();

            for(int k = 0; k < tll; k++)
            {
               Node q = tl.item(k);

               if(!q.getNodeName().equals("t"))
               {
                  throw new Exception("Expecting tag t in XML");
               }

               //Get text of token
               NodeList textl = q.getChildNodes();

               if(textl.getLength() != 1)
               {
                  throw new Exception("Token should only have text in XML");
               }

               Node text = textl.item(0);

               if(text.getNodeType() != Node.TEXT_NODE)
               {
                  throw new Exception("Token child is not text in XML");
               }

               Token tok = new Token(0, text.getNodeValue());  //Ignore type for now

               //Get attributes of token
               NamedNodeMap m  = q.getAttributes();
               int          ml = m.getLength();

               for(int l = 0; l < ml; l++)
               {
                  Node   attr = m.item(l);
                  String key  = attr.getNodeName();
                  String val  = attr.getNodeValue();
                  tok.putAttrib(key, val);
               }

               tv.add(tok);
            }

            sv.add(tv);
         }

         pv.add(sv);
      }

      return pv;
   }
}
