package minieiffel;

import java.io.StringReader;

import junit.framework.TestCase;
import minieiffel.Source.Position;
import minieiffel.Token.TokenType;
import minieiffel.Token.Value;

public class LexerTestCase extends TestCase {

    private Lexer lexer;
    
    private void createLexer(String source) {
        lexer = new Lexer(new Source(new StringReader(source)));
    }
    
    /**
     * Consumes a token, checks its type but ignores the content.
     */
    private Token consumeToken(TokenType type) {
        Token token = lexer.nextToken();
        assertEquals("Invalid token type", type, token.getType());
        if(type == TokenType.NEWLINE) {
            assertEquals("Newline token's text should be the newline", "\n", token.getText());
        }
        return token;
    }
    
    /**
     * Consumes the next token from the lexer and
     * checks its type and textual content (if specified)
     * according to the parameters given.
     */
    private Token consumeToken(TokenType type, String text) {
        Token token = consumeToken(type);
        assertEquals("Invalid token text", text, token.getText());
        return token;
    }
    
    /**
     * Consumes a token, checks it type and ensures it has
     * the correct value.
     */
    private Token consumeToken(TokenType type, Value value) {
        Token token = consumeToken(type);
        assertEquals("Invalid token value", value, token.getValue());
        return token;
    }
    
    /**
     * Consumes tokens on a line and asserts their start positions are correct.
     */
    private void consumeTokensOnLine(int line, int... positions) {
        for (int pos : positions) {
            Token t = lexer.nextToken();
            assertEquals(new Position(line, pos), t.getPosition());
        }
    }
    
    public void testSourceMustBeNonNull() {
        try {
            new Lexer(null);
            fail("Creating a lexer with null Source should've failed");
        } catch(IllegalArgumentException e) {
            assertEquals("Source must be non-null", e.getMessage());
        }
    }
    
    public void testOperators() {
        createLexer(".+-*/^=<>\\\\<=>=/=or xor and then and or else");
        consumeToken(TokenType.OPERATOR, Value.DOT);
        consumeToken(TokenType.OPERATOR, Value.PLUS);
        consumeToken(TokenType.OPERATOR, Value.MINUS);
        consumeToken(TokenType.OPERATOR, Value.MULTIPLY);
        consumeToken(TokenType.OPERATOR, Value.DIVIDE);
        consumeToken(TokenType.OPERATOR, Value.POWER);
        consumeToken(TokenType.OPERATOR, Value.EQUALITY);
        consumeToken(TokenType.OPERATOR, Value.LESS);
        consumeToken(TokenType.OPERATOR, Value.GREATER);
        consumeToken(TokenType.OPERATOR, Value.REMAINDER);
        consumeToken(TokenType.OPERATOR, Value.LESS_OR_EQUAL);
        consumeToken(TokenType.OPERATOR, Value.GREATER_OR_EQUAL);
        consumeToken(TokenType.OPERATOR, Value.INEQUALITY);
        consumeToken(TokenType.OPERATOR, Value.OR);
        consumeToken(TokenType.OPERATOR, Value.XOR);
        consumeToken(TokenType.OPERATOR, Value.AND_THEN);
        consumeToken(TokenType.OPERATOR, Value.AND);
        consumeToken(TokenType.OPERATOR, Value.OR_ELSE);
        consumeToken(TokenType.EOF);
    }
    
    public void testIdentifiersThatLookLikeOperators() {
        createLexer("not_123_abc not or_ xor_ and_ and then_ and then or else_ or else and thenx and then and");
        consumeToken(TokenType.IDENTIFIER, "not_123_abc");
        consumeToken(TokenType.OPERATOR, Value.NOT);
        consumeToken(TokenType.IDENTIFIER, "or_");
        consumeToken(TokenType.IDENTIFIER, "xor_");
        consumeToken(TokenType.IDENTIFIER, "and_");
        consumeToken(TokenType.OPERATOR, Value.AND);
        consumeToken(TokenType.IDENTIFIER, "then_");
        consumeToken(TokenType.OPERATOR, Value.AND_THEN);
        consumeToken(TokenType.OPERATOR, Value.OR);
        consumeToken(TokenType.IDENTIFIER, "else_");
        consumeToken(TokenType.OPERATOR, Value.OR_ELSE);
        consumeToken(TokenType.OPERATOR, Value.AND);
        consumeToken(TokenType.IDENTIFIER, "thenx");
        consumeToken(TokenType.OPERATOR, Value.AND_THEN);
        consumeToken(TokenType.OPERATOR, Value.AND);
        consumeToken(TokenType.EOF);
        consumeToken(TokenType.EOF);
    }
    
    public void testReadingPartialOperatorAtEOF() {
        createLexer("+-\\");
        consumeToken(TokenType.OPERATOR, Value.PLUS);
        consumeToken(TokenType.OPERATOR, Value.MINUS);
        consumeToken(TokenType.ERROR, "\\");
    }
    
    public void testInvalidOperators() {
        createLexer("\\*and the");
        consumeToken(TokenType.ERROR, "\\*");
        consumeToken(TokenType.OPERATOR, Value.AND);
        consumeToken(TokenType.IDENTIFIER, "the");
        consumeToken(TokenType.EOF);
    }
    
    public void testUnaryOperators() {
        createLexer("-not");
        consumeToken(TokenType.OPERATOR, Value.MINUS);
        consumeToken(TokenType.OPERATOR, Value.NOT);
        consumeToken(TokenType.EOF);
    }
    
    public void testWhitespaceHandling() {
        createLexer("and . + or  or else  or  else");
        consumeToken(TokenType.OPERATOR, Value.AND);
        consumeToken(TokenType.OPERATOR, Value.DOT);
        consumeToken(TokenType.OPERATOR, Value.PLUS);
        consumeToken(TokenType.OPERATOR, Value.OR);
        consumeToken(TokenType.OPERATOR, Value.OR_ELSE);
        consumeToken(TokenType.OPERATOR, Value.OR);
        consumeToken(TokenType.KEYWORD, Value.ELSE);
        consumeToken(TokenType.EOF);
    }

    public void testSubsequentEmptyLinesAreSqueezed() {
        createLexer("  \n  \n  \n");
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.EOF);
    }
    
    public void testSignificantNewLinesAreReported() {
        createLexer("  \nelse\n\n");
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.KEYWORD, Value.ELSE);
        consumeToken(TokenType.NEWLINE);
    }
    
    public void testUnorthodoxNewLinesAreSupported() {
        createLexer("+\n-\r/\r\n");
        consumeToken(TokenType.OPERATOR, Value.PLUS);
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.OPERATOR, Value.MINUS);
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.OPERATOR, Value.DIVIDE);
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.EOF);
    }
    
    public void testCommentAreStrippedOut() {
        createLexer(
                "+-\n-- A comment +-\n  --Another comment+-\n" +
                "+- --Comment after operators\n" +
                "/*-- Comment after other operators\n--More comments"
        );
        consumeToken(TokenType.OPERATOR, Value.PLUS);
        consumeToken(TokenType.OPERATOR, Value.MINUS);
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.OPERATOR, Value.PLUS);
        consumeToken(TokenType.OPERATOR, Value.MINUS);
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.OPERATOR, Value.DIVIDE);
        consumeToken(TokenType.OPERATOR, Value.MULTIPLY);
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.EOF);
    }
    
    public void testIdentifiers() {
        createLexer("myString my_123 some123 str1ng_222");
        consumeToken(TokenType.IDENTIFIER, "myString");
        consumeToken(TokenType.IDENTIFIER, "my_123");
        consumeToken(TokenType.IDENTIFIER, "some123");
        consumeToken(TokenType.IDENTIFIER, "str1ng_222");
        consumeToken(TokenType.EOF);
    }
 
    public void testInvalidIdentifiers() {
        createLexer("_invalid 2as_is_this");
        consumeToken(TokenType.ERROR, "_invalid");
        consumeToken(TokenType.ERROR, "2as_is_this");
        consumeToken(TokenType.EOF);
    }
    
    public void testKeywords() {
        createLexer(
                "class end feature is local do if then " +
                "elseif else from until loop"
        );
        consumeToken(TokenType.KEYWORD, Value.CLASS);
        consumeToken(TokenType.KEYWORD, Value.END);
        consumeToken(TokenType.KEYWORD, Value.FEATURE);
        consumeToken(TokenType.KEYWORD, Value.IS);
        consumeToken(TokenType.KEYWORD, Value.LOCAL);
        consumeToken(TokenType.KEYWORD, Value.DO);
        consumeToken(TokenType.KEYWORD, Value.IF);
        consumeToken(TokenType.KEYWORD, Value.THEN);
        consumeToken(TokenType.KEYWORD, Value.ELSEIF);
        consumeToken(TokenType.KEYWORD, Value.ELSE);
        consumeToken(TokenType.KEYWORD, Value.FROM);
        consumeToken(TokenType.KEYWORD, Value.UNTIL);
        consumeToken(TokenType.KEYWORD, Value.LOOP);
        consumeToken(TokenType.EOF);
    }
    
    public void testCharLiterals() {
        createLexer("'a' 'A' '5' '''' '&'");
        consumeToken(TokenType.CHAR_LITERAL, "a");
        consumeToken(TokenType.CHAR_LITERAL, "A");
        consumeToken(TokenType.CHAR_LITERAL, "5");
        consumeToken(TokenType.CHAR_LITERAL, "'");
        consumeToken(TokenType.CHAR_LITERAL, "&");
        consumeToken(TokenType.EOF);
    }
    
    public void testInvalidCharLiterals() {
        createLexer("'bb '' 'a");
        consumeToken(TokenType.ERROR, "'bb");
        consumeToken(TokenType.ERROR, "''");
        consumeToken(TokenType.ERROR, "'a");
        consumeToken(TokenType.EOF);
    }
    
    public void testIntegerLiterals() {
        createLexer("123 0 5 9 34567 s123 123s123");
        consumeToken(TokenType.INT_LITERAL, "123");
        consumeToken(TokenType.INT_LITERAL, "0");
        consumeToken(TokenType.INT_LITERAL, "5");
        consumeToken(TokenType.INT_LITERAL, "9");
        consumeToken(TokenType.INT_LITERAL, "34567");
        consumeToken(TokenType.IDENTIFIER, "s123");
        consumeToken(TokenType.ERROR, "123s123");
        consumeToken(TokenType.EOF);
    }
    
    public void testRealLiterals() {
        createLexer("1.0 345.23 22. .22 1.1x");
        consumeToken(TokenType.REAL_LITERAL, "1.0");
        consumeToken(TokenType.REAL_LITERAL, "345.23");
        consumeToken(TokenType.ERROR, "22.");
        consumeToken(TokenType.OPERATOR, ".");
        consumeToken(TokenType.INT_LITERAL, "22");
        consumeToken(TokenType.ERROR, "1.1x");
        consumeToken(TokenType.EOF);
    }
    
    public void testBooleanLiterals() {
        createLexer("true trueX false falseX");
        consumeToken(TokenType.BOOLEAN_LITERAL, Value.TRUE);
        consumeToken(TokenType.IDENTIFIER, "trueX");
        consumeToken(TokenType.BOOLEAN_LITERAL, Value.FALSE);
        consumeToken(TokenType.IDENTIFIER, "falseX");
        consumeToken(TokenType.EOF);
    }
    
    public void testConstruction() {
        createLexer("!! myObj\n!!yourObj\n!");
        consumeToken(TokenType.OTHER, Value.CONSTRUCTION);
        consumeToken(TokenType.IDENTIFIER, "myObj");
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.OTHER, Value.CONSTRUCTION);
        consumeToken(TokenType.IDENTIFIER, "yourObj");
        consumeToken(TokenType.NEWLINE);
        consumeToken(TokenType.ERROR, "!");
        consumeToken(TokenType.EOF);
    }
    
    public void testBraces() {
        createLexer("{ NONE }");
        consumeToken(TokenType.OTHER, Value.LEFT_BRACE);
        consumeToken(TokenType.IDENTIFIER, "NONE");
        consumeToken(TokenType.OTHER, Value.RIGHT_BRACE);
        consumeToken(TokenType.EOF);
    }
    
    public void testCommas() {
        createLexer("a, b");
        consumeToken(TokenType.IDENTIFIER, "a");
        consumeToken(TokenType.OTHER, Value.COMMA);
        consumeToken(TokenType.IDENTIFIER, "b");
        consumeToken(TokenType.EOF);
    }
    
    public void testColons() {
        createLexer("a : INTEGER");
        consumeToken(TokenType.IDENTIFIER, "a");
        consumeToken(TokenType.OTHER, Value.COLON);
        consumeToken(TokenType.IDENTIFIER, "INTEGER");
        consumeToken(TokenType.EOF);
    }
    
    public void testSemicolons() {
        createLexer("a;b;");
        consumeToken(TokenType.IDENTIFIER, "a");
        consumeToken(TokenType.OTHER, Value.SEMICOLON);
        consumeToken(TokenType.IDENTIFIER, "b");
        consumeToken(TokenType.OTHER, Value.SEMICOLON);
        consumeToken(TokenType.EOF);
    }
    
    public void testParenthesis() {
        createLexer("deposit(sum: INTEGER) is");
        consumeToken(TokenType.IDENTIFIER, "deposit");
        consumeToken(TokenType.OTHER, Value.LEFT_PAREN);
        consumeToken(TokenType.IDENTIFIER, "sum");
        consumeToken(TokenType.OTHER, Value.COLON);
        consumeToken(TokenType.IDENTIFIER, "INTEGER");
        consumeToken(TokenType.OTHER, Value.RIGHT_PAREN);
        consumeToken(TokenType.KEYWORD, Value.IS);
        consumeToken(TokenType.EOF);
    }
    
    public void testAssignment() {
        createLexer("balance := balance + sum");
        consumeToken(TokenType.IDENTIFIER, "balance");
        consumeToken(TokenType.OTHER, Value.ASSIGNMENT);
        consumeToken(TokenType.IDENTIFIER, "balance");
        consumeToken(TokenType.OPERATOR, Value.PLUS);
        consumeToken(TokenType.IDENTIFIER, "sum");
        consumeToken(TokenType.EOF);
    }
    
    public void testPositions() {
        String source =
        /*      0        10        20        30
                12345678901234567890123456789012  */
        /*1*/  "class ACCOUNT\n" +
        /*2*/  "feature {NONE}\n" +
        /*3*/  "  -- the same as feature {}\n" +
        /*4*/  "  balance:     INTEGER\n" +
        /*5*/  "\n" +
        /*6*/  "  add(sum: INTEGER) is\n" +
        /*7*/  "  -- add sum to the balance\n" +
        /*8*/  "  -- visible only to features located in this class\n" +
        /*9*/  "\n"  +
        /*10*/ "  do\n" +
        /*11*/ "    balance := balance + sum\n" +
        /*12*/ "  end -- private add\n" +
        /*13*/ "  and then\n" +
        /*14*/ "  and thenX\n" +
        /*15*/ "  or else and";
        createLexer(source);
        consumeTokensOnLine(1, 1, 7, 14);
        consumeTokensOnLine(2, 1, 9, 10, 14, 15);
        consumeTokensOnLine(4, 3, 10, 16, 23);
        consumeTokensOnLine(6, 3, 6, 7, 10, 12, 19, 21, 23);
        consumeTokensOnLine(10, 3, 5);
        consumeTokensOnLine(11, 5, 13, 16, 24, 26, 29);
        consumeTokensOnLine(12, 3, 21);
        consumeTokensOnLine(13, 3, 11);
        consumeTokensOnLine(14, 3, 7, 12);
        consumeTokensOnLine(15, 3, 11, 14);
    }
    
    public void testCurrentToken() {
        createLexer("a b and then");
        assertNull(lexer.currentToken());
        Token t = consumeToken(TokenType.IDENTIFIER, "a");
        assertEquals(t, lexer.currentToken());
        t = consumeToken(TokenType.IDENTIFIER, "b");
        assertEquals(t, lexer.currentToken());
        t = consumeToken(TokenType.OPERATOR, Value.AND_THEN);
        assertEquals(t, lexer.currentToken());
    }
    
    public void testPeekToken() {
        createLexer("x or or else y");
        assertNull(lexer.currentToken());

        Token x = new Token(TokenType.IDENTIFIER, "x");
        // peeking twice should yield the same result
        assertEquals(x, lexer.peekToken());
        assertEquals(x, lexer.peekToken());
        // current token should still be null
        assertNull(lexer.currentToken());
        assertEquals(x, lexer.nextToken());
        assertEquals(x, lexer.currentToken());
        assertEquals(new Position(1,1), lexer.currentToken().getPosition());
        
        Token or = new Token(Value.OR);
        assertEquals(or, lexer.peekToken());
        assertEquals(or, lexer.peekToken());
        assertEquals(x, lexer.currentToken());
        assertEquals(or, lexer.nextToken());
        assertEquals(or, lexer.currentToken());
        assertEquals(new Position(1,3), lexer.currentToken().getPosition());
        
        Token orElse = new Token(Value.OR_ELSE);
        assertEquals(orElse, lexer.peekToken());
        assertEquals(orElse, lexer.peekToken());
        assertEquals(or, lexer.currentToken());
        assertEquals(orElse, lexer.nextToken());
        assertEquals(orElse, lexer.currentToken());
        assertEquals(new Position(1,6), lexer.currentToken().getPosition());
        
        Token y = new Token(TokenType.IDENTIFIER, "y");
        assertEquals(y, lexer.peekToken());
        assertEquals(y, lexer.peekToken());
        assertEquals(orElse, lexer.currentToken());
        assertEquals(y, lexer.nextToken());
        assertEquals(y, lexer.currentToken());
        assertEquals(new Position(1,14), lexer.currentToken().getPosition());
    }

}
