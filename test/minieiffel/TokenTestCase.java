package minieiffel;

import junit.framework.TestCase;
import minieiffel.Token.TokenType;
import minieiffel.Token.Value;

public class TokenTestCase extends TestCase {

    public void testValueOrTypeMustBeNonNull() {
        try {
            new Token((Value)null);
            fail();
        } catch(IllegalArgumentException e) {
            assertEquals("Value must be non-null", e.getMessage());
        }
        try {
            new Token((TokenType)null);
            fail();
        } catch(IllegalArgumentException e) {
            assertEquals("Type must be non-null", e.getMessage());
        }
    }
    
    public void testTokenEquality() {
        assertEquals(new Token(Value.AND), new Token(Value.AND));
        assertFalse(new Token(Value.OR).equals(new Token(Value.THEN)));
        assertEquals(new Token(TokenType.BOOLEAN_LITERAL), new Token(TokenType.BOOLEAN_LITERAL));
        assertFalse(new Token(TokenType.CHAR_LITERAL).equals(new Token(TokenType.REAL_LITERAL)));
        assertEquals(new Token(TokenType.INT_LITERAL, "123"), new Token(TokenType.INT_LITERAL, "123"));
        assertFalse(new Token(TokenType.BOOLEAN_LITERAL, "true").equals(new Token(TokenType.BOOLEAN_LITERAL, "false")));
    }
    
}
