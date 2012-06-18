package com.redhat.openshift.forge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TestUtil {

   @Test
   public void testUnquoteValid() {
      assertEquals("", Util.unquote(""));
      assertEquals("", Util.unquote("''"));
      assertEquals("", Util.unquote("\"\""));

      assertEquals("a", Util.unquote("a"));
      assertEquals("a", Util.unquote("'a'"));
      assertEquals("a", Util.unquote("\"a\""));

      assertEquals("a phrase", Util.unquote("a phrase"));
      assertEquals("a phrase", Util.unquote("'a phrase'"));
      assertEquals("a phrase", Util.unquote("\"a phrase\""));
   }

   @Test
   public void testUnquoteInvalid() {
      assertNull(Util.unquote(null));

      assertEquals("'", Util.unquote("'"));
      assertEquals("\"", Util.unquote("\""));

      assertEquals("a'", Util.unquote("a'"));
      assertEquals("a\"", Util.unquote("a\""));

      assertEquals("'b", Util.unquote("'b"));
      assertEquals("\"b", Util.unquote("\"b"));

      assertEquals(" 'string'", Util.unquote(" 'string'"));
      assertEquals(" \"string\"", Util.unquote(" \"string\""));
   }
}