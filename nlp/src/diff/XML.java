package nlp.diff;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;

/**
 * A wrapper for handling XML.
 *
 * <pre>
 * Typical use:
 * Document d1 = XML.blank();
 * Document d2 = XML.parse(System.in);
 * XML.write(d2,System.out);
 * </pre>
 *
 * @author Sterling Stuart Stein
 */
public class XML
{
   /**
    * Used to make XML documents
    */
   static protected DocumentBuilder db;

   /**
    * Used to write XML documents
    */
   static protected Transformer ts;

   static
   {
      try
      {
         db    = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         ts    = TransformerFactory.newInstance().newTransformer();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Make a new, blank XML document
    *
    * @return The document
    */
   static public Document blank()
   {
      return db.newDocument();
   }

   /**
    * Read and parse XML into a document
    *
    * @param i The stream to read
    * @return The document
    */
   static public Document parse(InputStream i) throws Exception
   {
      return db.parse(i);
   }

   /**
    * Write out a document as XML
    *
    * @param d The document to be written
    * @param o The stream to be written to
    */
   static public void write(Document d, OutputStream o)
      throws Exception
   {
      ts.transform(new DOMSource(d), new StreamResult(o));
   }
}
