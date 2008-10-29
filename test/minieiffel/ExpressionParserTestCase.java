package minieiffel;

import java.io.StringReader;

import junit.framework.TestCase;
import minieiffel.Token.TokenType;
import minieiffel.Token.Value;
import minieiffel.ast.BinaryExpressionAST;
import minieiffel.ast.InvocationAST;
import minieiffel.ast.SimpleExpressionAST;
import minieiffel.ast.UnaryExpressionAST;

public class ExpressionParserTestCase extends TestCase {
    
    private ExpressionParser parser;
    
    private void createParser(String code) {
        Source source = new Source(new StringReader(code));
        this.parser = new ExpressionParser(new Lexer(source));
    }
    
    public void testLiteralExpressions() {
        createParser("547");
        assertEquals(TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "547"), parser.handleExpression());
        createParser("15.0");
        assertEquals(TestCaseUtil.simpleExpr(TokenType.REAL_LITERAL, "15.0"), parser.handleExpression());
        createParser("'a'");
        assertEquals(TestCaseUtil.simpleExpr(TokenType.CHAR_LITERAL, "a"), parser.handleExpression());
        createParser("false");
        assertEquals(new SimpleExpressionAST(new Token(Value.FALSE)), parser.handleExpression());
    }
    
    public void testIdentifierExpressions() {
        createParser("abc");
        assertEquals(TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "abc"), parser.handleExpression());
    }
    
    public void testSimpleArithmeticExpressions() {
        createParser("1 + 2");
        assertEquals(
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1"),
                        new Token(Value.PLUS),
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2")
                ),
                parser.handleExpression()
        );
        createParser("3 * 4 * 5");
        assertEquals(
                new BinaryExpressionAST(
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3"),
                                new Token(Value.MULTIPLY),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "4")
                        ),
                        new Token(Value.MULTIPLY),
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "5")
                ),
                parser.handleExpression()
        );
    }
    
    public void testPrecedence() {
        createParser("1 + 2 * 3");
        assertEquals(
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1"),
                        new Token(Value.PLUS),
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2"),
                                new Token(Value.MULTIPLY),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                        )
                ),
                parser.handleExpression()
        );
        createParser("3 / a.b");
        assertEquals(
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3"),
                        new Token(Value.DIVIDE),
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                                new Token(Value.DOT),
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b")
                        )
                ),
                parser.handleExpression()
        );
        createParser("true and then 2 /= 3");
        assertEquals(
                new BinaryExpressionAST(
                        new SimpleExpressionAST(new Token(Value.TRUE)),
                        new Token(Value.AND_THEN),
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2"),
                                new Token(Value.INEQUALITY),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                        )
                ),
                parser.handleExpression()
        );
    }
    
    public void testParenthesizedExpressions() {
        createParser("(a)");
        assertEquals(
                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                parser.handleExpression()
        );
        createParser("(1 + 2) * 3");
        assertEquals(
                new BinaryExpressionAST(
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1"),
                                new Token(Value.PLUS),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2")
                        ),
                        new Token(Value.MULTIPLY),
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                ),
                parser.handleExpression()
        );
        createParser("5 * (4 + 3) + 2 * 1");
        assertEquals(
                new BinaryExpressionAST(
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "5"),
                                new Token(Value.MULTIPLY),
                                new BinaryExpressionAST(
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "4"),
                                        new Token(Value.PLUS),
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                                )
                        ),
                        new Token(Value.PLUS),
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2"),
                                new Token(Value.MULTIPLY),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1")
                        )
                ),
                parser.handleExpression()
        );
        createParser("1 / (2 + 3 + 4)");
        assertEquals(
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1"),
                        new Token(Value.DIVIDE),
                        new BinaryExpressionAST(
                                new BinaryExpressionAST(
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2"),
                                        new Token(Value.PLUS),
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                                ),
                                new Token(Value.PLUS),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "4")
                        )
                ),
                parser.handleExpression()
        );
        createParser("(((((((a+b))*c)))))");
        assertEquals(
                new BinaryExpressionAST(
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                                new Token(Value.PLUS),
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b")
                        ),
                        new Token(Value.MULTIPLY),
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "c")
                ),
                parser.handleExpression()
        );
        createParser("(a+b*c+d*e)");
        assertEquals(
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                        new Token(Value.PLUS),
                        new BinaryExpressionAST(
                                new BinaryExpressionAST(
                                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b"),
                                        new Token(Value.MULTIPLY),
                                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "c")
                                ),
                                new Token(Value.PLUS),
                                new BinaryExpressionAST(
                                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "d"),
                                        new Token(Value.MULTIPLY),
                                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "e")
                                )
                        )
                ),
                parser.handleExpression()
        );
    }
    
    public void testUnaryOperators() {
        createParser("not a");
        assertEquals(
            new UnaryExpressionAST(new Token(Value.NOT), TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a")),
            parser.handleExpression()
        );
        createParser("a and not b");
        assertEquals(
            new BinaryExpressionAST(
                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                new Token(Value.AND),
                new UnaryExpressionAST(new Token(Value.NOT), TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b"))
            ),
            parser.handleExpression()
        );
        createParser("not false or not true and not false");
        assertEquals(
            new BinaryExpressionAST(
                new UnaryExpressionAST(new Token(Value.NOT), new SimpleExpressionAST(new Token(Value.FALSE))),
                new Token(Value.OR),
                new BinaryExpressionAST(
                    new UnaryExpressionAST(new Token(Value.NOT), new SimpleExpressionAST(new Token(Value.TRUE))),
                    new Token(Value.AND),
                    new UnaryExpressionAST(new Token(Value.NOT), new SimpleExpressionAST(new Token(Value.FALSE)))
                )
            ),
            parser.handleExpression()
        );
        createParser("not (a and b)");
        assertEquals(
            new UnaryExpressionAST(
                new Token(Value.NOT),
                new BinaryExpressionAST(
                    TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                    new Token(Value.AND),
                    TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b")
                )
            ),
            parser.handleExpression()
        );
    }
    
    public void testUnaryAndBinaryMinus() {
        createParser("-a");
        assertEquals(
            new UnaryExpressionAST(
                new Token(Value.MINUS),
                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a")
            ),
            parser.handleExpression()
        );
        createParser("-(-a)");
        assertEquals(
                new UnaryExpressionAST(
                        new Token(Value.MINUS),
                        new UnaryExpressionAST(
                                new Token(Value.MINUS),
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a")
                        )
                ),
                parser.handleExpression()
        );
        createParser("a-b");
        assertEquals(
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                        new Token(Value.MINUS),
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b")
                ),
                parser.handleExpression()
        );
        createParser("a-(-b)");
        assertEquals(
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                        new Token(Value.MINUS),
                        new UnaryExpressionAST(
                                new Token(Value.MINUS),
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b")
                        )
                ),
                parser.handleExpression()
        );
        createParser("-(a-b)");
        assertEquals(
                new UnaryExpressionAST(
                        new Token(Value.MINUS),
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                                new Token(Value.MINUS),
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b")
                        )
                ),
                parser.handleExpression()
        );
        createParser("a + b * c - not - 3");
        assertEquals(
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                        new Token(Value.PLUS),
                        new BinaryExpressionAST(
                                new BinaryExpressionAST(
                                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b"),
                                        new Token(Value.MULTIPLY),
                                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "c")
                                ),
                                new Token(Value.MINUS),
                                new UnaryExpressionAST(
                                        new Token(Value.NOT),
                                        new UnaryExpressionAST(
                                                new Token(Value.MINUS),
                                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                                        )
                                )
                        )
                ),
                parser.handleExpression()
        );
    }
    
    public void testNoArgInvocation() {
        createParser("method()");
        assertEquals(
                new InvocationAST(new Token(TokenType.IDENTIFIER, "method")),
                parser.handleExpression()
        );
    }
    
    public void testSingleArgInvocation() {
        createParser("method(a)");
        assertEquals(
                new InvocationAST(
                        new Token(TokenType.IDENTIFIER, "method"),
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a")
                ),
                parser.handleExpression()
        );
    }
    
    public void testMultiArgInvocation() {
        createParser("method(a, b)");
        assertEquals(
                new InvocationAST(
                        new Token(TokenType.IDENTIFIER, "method"),
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b")
                ),
                parser.handleExpression()
        );
    }
    
    public void testRecursiveInvocation() {
        createParser("method(a() + d(), b(c))");
        assertEquals(
                new InvocationAST(
                        new Token(TokenType.IDENTIFIER, "method"),
                        new BinaryExpressionAST(
                                new InvocationAST(new Token(TokenType.IDENTIFIER, "a")),
                                new Token(Value.PLUS),
                                new InvocationAST(new Token(TokenType.IDENTIFIER, "d"))
                        ),
                        new InvocationAST(
                                new Token(TokenType.IDENTIFIER, "b"),
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "c")
                        )
                ),
                parser.handleExpression()
        );
    }
    
    public void testInvocationsAndOperators() {
        createParser("first.second.third(fourth)");
        assertEquals(
                new BinaryExpressionAST(
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "first"),
                                new Token(Value.DOT),
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "second")
                        ),
                        new Token(Value.DOT),
                        new InvocationAST(
                                new Token(TokenType.IDENTIFIER, "third"),
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "fourth")
                        )
                ),
                parser.handleExpression()
        );
        createParser("a(1.5) + b('x') * c(true)");
        assertEquals(
                new BinaryExpressionAST(
                        new InvocationAST(
                                new Token(TokenType.IDENTIFIER, "a"),
                                TestCaseUtil.simpleExpr(TokenType.REAL_LITERAL, "1.5")
                        ),
                        new Token(Value.PLUS),
                        new BinaryExpressionAST(
                                new InvocationAST(
                                        new Token(TokenType.IDENTIFIER, "b"),
                                        TestCaseUtil.simpleExpr(TokenType.CHAR_LITERAL, "x")
                                ),
                                new Token(Value.MULTIPLY),
                                new InvocationAST(
                                        new Token(TokenType.IDENTIFIER, "c"),
                                        new SimpleExpressionAST(new Token(Value.TRUE))
                                )
                        )
                ),
                parser.handleExpression()
        );
    }
    
    // tests for invalid expressions follow
    
    public void testInvalidInvocation() {
        createParser("method(a,)");
        try {
            parser.handleExpression();
            fail("Should've failed, argument missing");
        } catch(SyntaxException e) { }
        createParser("method(,)");
        try {
            parser.handleExpression();
            fail("Should've failed, arguments missing");
        } catch(SyntaxException e) { }
    }
    
    public void testNonMatchingParens() {
        createParser("(a+b))");
        try {
            parser.handleExpression();
            fail();
        } catch(SyntaxException e) { }
        createParser("))");
        try {
            parser.handleExpression();
            fail();
        } catch(SyntaxException e) { }
        createParser("((a)\n");
        try {
            parser.handleExpression();
            fail();
        } catch(SyntaxException e) { }
        createParser(")");
        try {
            parser.handleExpression();
            fail();
        } catch(SyntaxException e) { }
        createParser("(");
        try {
            parser.handleExpression();
            fail();
        } catch(SyntaxException e) { }
    }
    
    public void testInvalidExpressions() {
        try {
            createParser("123+");
            parser.handleExpression();
            fail();
        } catch (SyntaxException e) { }
        try {
            createParser("a not b");
            parser.handleExpression();
            fail();
        } catch (SyntaxException e) { }
        try {
            createParser("a b");
            parser.handleExpression();
            fail();
        } catch (SyntaxException e) { }
        try {
            createParser("+");
            parser.handleExpression();
            fail();
        } catch (SyntaxException e) { }
        try {
            createParser("not");
            parser.handleExpression();
            fail();
        } catch (SyntaxException e) { }
        try {
            createParser("false or else");
            parser.handleExpression();
            fail();
        } catch (SyntaxException e) { }
        try {
            createParser("3++2");
            parser.handleExpression();
            fail();
        } catch (SyntaxException e) { }
    }

    public void testEmptyExpressionIsInvalid() {
        createParser("");
        try {
            parser.handleExpression();
            fail();
        } catch(SyntaxException e) { }
    }

}
