package minieiffel;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import minieiffel.Source.Position;
import minieiffel.Token.TokenType;
import minieiffel.Token.Value;
import minieiffel.ast.AssignmentAST;
import minieiffel.ast.BinaryExpressionAST;
import minieiffel.ast.ClassAST;
import minieiffel.ast.ConditionalAST;
import minieiffel.ast.ConstructionAST;
import minieiffel.ast.FeatureAST;
import minieiffel.ast.FeatureBlockAST;
import minieiffel.ast.IfStatementAST;
import minieiffel.ast.InstructionAST;
import minieiffel.ast.InstructionsAST;
import minieiffel.ast.InvocationAST;
import minieiffel.ast.IterationAST;
import minieiffel.ast.MethodAST;
import minieiffel.ast.ParamDeclAST;
import minieiffel.ast.ProgramAST;
import minieiffel.ast.SimpleExpressionAST;
import minieiffel.ast.UnaryExpressionAST;
import minieiffel.ast.VariableDeclAST;

public class ParserTestCase extends TestCase {
    
    private static final List<VariableDeclAST> EMPTY_VARS = Collections.emptyList();
    private static final List<IfStatementAST> EMPTY_IFS = Collections.emptyList();
    
    private Parser parser;
    private Lexer lexer;
    
    /* Helper methods */
    
    private void createParser(String code) {
        Source source = new Source(new StringReader(code));
        this.lexer = new Lexer(source);
        this.parser = new Parser(lexer);
    }
    
    private void assertErrorDetails(
            SyntaxException err, Position pos, Object expected, TokenType actual) {
        if(pos != null) {
            assertEquals("Syntax error at wrong position", pos, err.getOffendingToken().getPosition());
        }
        assertEquals("Expected type wrong", expected, err.getExpected());
        assertEquals("Actual type wrong", actual, err.getOffendingToken().getType());
    }
    
    private InstructionsAST instructions(InstructionAST... instructions) {
        return new InstructionsAST(
                EMPTY_VARS,
                instructions
        );
    }

    /* Actual tests */
    
    public void testLexerMustBeNonNull() {
        try {
            new Parser(null);
            fail("Creating a parser with null Lexer should've failed");
        } catch(IllegalArgumentException e) {
            assertEquals("Lexer must be non-null", e.getMessage());
        }
    }
    
    public void testConstruction() {
        createParser("!! myObj");
        ConstructionAST ast = parser.handleConstruction();
        assertEquals(TestCaseUtil.id("myObj"), ast.getIdentifier());
    }
    
    public void testInvalidConstruction() {
        createParser("!!");
        try {
            parser.handleConstruction();
            fail();
        } catch(SyntaxException e) {
            assertErrorDetails(e, new Position(1,3), TokenType.IDENTIFIER, TokenType.EOF);
        }
        createParser("!! 123");
        try {
            parser.handleConstruction();
            fail();
        } catch(SyntaxException e) {
            assertErrorDetails(e, new Position(1,4), TokenType.IDENTIFIER, TokenType.INT_LITERAL);
        }
    }
    
    public void testEmptyVariableDecl() {
        createParser("");
        assertTrue(parser.handleVariableDecl().isEmpty());
    }

    public void testSingleVariableDecl() {
        createParser("my_int : INTEGER");
        assertEquals(
            new VariableDeclAST(TestCaseUtil.id("my_int"), TestCaseUtil.id("INTEGER"), null),
            parser.handleVariableDecl().get(0)
        );
    }
    
    public void testMultipleVariableDecl() {
        createParser("a, b, c : INTEGER");
        TestCaseUtil.assertListContents(
                parser.handleVariableDecl(),
                new VariableDeclAST(TestCaseUtil.id("a"), TestCaseUtil.id("INTEGER"), null),
                new VariableDeclAST(TestCaseUtil.id("b"), TestCaseUtil.id("INTEGER"), null),
                new VariableDeclAST(TestCaseUtil.id("c"), TestCaseUtil.id("INTEGER"), null)
        );
    }

    public void testVariableDeclWithValue() {
        createParser("a, b : CHAR is '_'");
        TestCaseUtil.assertListContents(
                parser.handleVariableDecl(),
                new VariableDeclAST(TestCaseUtil.id("a"), TestCaseUtil.id("CHAR"), new Token(TokenType.CHAR_LITERAL, "_")),
                new VariableDeclAST(TestCaseUtil.id("b"), TestCaseUtil.id("CHAR"), new Token(TokenType.CHAR_LITERAL, "_"))
        );
    }
    
    public void testSubsequentVariableDecls() {
        createParser("a : INTEGER\nb : CHAR");
        TestCaseUtil.assertListContents(
                parser.handleVariableDecl(),
                new VariableDeclAST(TestCaseUtil.id("a"), TestCaseUtil.id("INTEGER"), null),
                new VariableDeclAST(TestCaseUtil.id("b"), TestCaseUtil.id("CHAR"), null)
        );
    }
    
    public void testInvalidVariableDecls() {
        createParser("a : 123");
        try {
            parser.handleVariableDecl();
            fail();
        } catch(SyntaxException e) {
            assertErrorDetails(e, new Position(1,5), TokenType.IDENTIFIER, TokenType.INT_LITERAL);
        }
        createParser("a, b, : INTEGER");
        try {
            parser.handleVariableDecl();
            fail();
        } catch(SyntaxException e) {
            assertErrorDetails(e, new Position(1,7), TokenType.IDENTIFIER, TokenType.OTHER);
        }
    }
    
    public void testConstantDecl() {
        createParser("");
        assertNull(parser.handleConstantDecl());
        createParser("is 3.14");
        Token literal = parser.handleConstantDecl();
        assertEquals("3.14", literal.getText());
        assertEquals(TokenType.REAL_LITERAL, literal.getType());
    }
    
    public void testInvalidConstantDecl() {
        createParser("is id");
        try {
            parser.handleConstantDecl();
            fail();
        } catch(SyntaxException e) {
            assertErrorDetails(e, new Position(1,4), TokenType.LITERAL, TokenType.IDENTIFIER);
        }
    }
    
    public void testTypeName() {
        createParser("SOMETYPE");
        assertEquals(parser.handleTypeName(), new Token(TokenType.IDENTIFIER, "SOMETYPE"));
    }
    
    public void testReturnType() {
        createParser("");
        assertNull(parser.handleReturnType());
        createParser(": SOMETYPE");
        assertEquals(parser.handleReturnType(), new Token(TokenType.IDENTIFIER, "SOMETYPE"));
    }
    
    public void testParamList() {
        createParser("a : SOMETYPE");
        List<ParamDeclAST> params = parser.handleParamList();
        TestCaseUtil.assertListContents(
                params,
                new ParamDeclAST(TestCaseUtil.id("a"), TestCaseUtil.id("SOMETYPE"))
        );
        createParser("a, b : SOMETYPE");
        params = parser.handleParamList();
        TestCaseUtil.assertListContents(
                params,
                new ParamDeclAST(TestCaseUtil.id("a"), TestCaseUtil.id("SOMETYPE")),
                new ParamDeclAST(TestCaseUtil.id("b"), TestCaseUtil.id("SOMETYPE"))
        );
        createParser("x : INTEGER; y : STRING");
        params = parser.handleParamList();
        TestCaseUtil.assertListContents(
                params,
                new ParamDeclAST(TestCaseUtil.id("x"), TestCaseUtil.id("INTEGER")),
                new ParamDeclAST(TestCaseUtil.id("y"), TestCaseUtil.id("STRING"))
        );
    }
    
    public void testInvalidParamList() {
        createParser("x : INTEGER;");
        try {
            parser.handleParamList();
            fail();
        } catch(SyntaxException e) {
            assertErrorDetails(e, new Position(1,13), TokenType.IDENTIFIER, TokenType.EOF);
        }
    }
    
    public void testParams() {
        createParser("()");
        assertTrue(parser.handleParams().isEmpty());
        createParser("(a : SOMETYPE; b : SOMEOTHERTYPE)");
        TestCaseUtil.assertListContents(
                parser.handleParams(),
                new ParamDeclAST(TestCaseUtil.id("a"), TestCaseUtil.id("SOMETYPE")),
                new ParamDeclAST(TestCaseUtil.id("b"), TestCaseUtil.id("SOMEOTHERTYPE"))
        );
    }
    
    public void testInvalidParams() {
        createParser("(x : INTEGER; y : INTEGER");
        try {
            parser.handleParams();
            fail();
        } catch(SyntaxException e) {
            assertErrorDetails(e, new Position(1,26), Value.RIGHT_PAREN, TokenType.EOF);
        }
    }
    
    public void testLocalDeclarations() {
        createParser("");
        assertEquals(0, parser.handleLocalDeclarations().size());
        createParser("local\na:INTEGER\nb,c:ANY\nabc()");
        TestCaseUtil.assertListContents(
                parser.handleLocalDeclarations(),
                new VariableDeclAST(TestCaseUtil.id("a"), TestCaseUtil.id("INTEGER"), null),
                new VariableDeclAST(TestCaseUtil.id("b"), TestCaseUtil.id("ANY"), null),
                new VariableDeclAST(TestCaseUtil.id("c"), TestCaseUtil.id("ANY"), null)
        );
    }
    
    public void testVisibility() {
        // same as { ANY }
        createParser("");
        assertNull(parser.handleVisibility());
        // same as { NONE }
        createParser("{}");
        assertTrue(parser.handleVisibility().isEmpty());
        createParser("{ ANY }");
        TestCaseUtil.assertListContents(
                parser.handleVisibility(),
                new Token(TokenType.IDENTIFIER, "ANY")
        );
        createParser("{INTEGER, STRING}");
        TestCaseUtil.assertListContents(
                parser.handleVisibility(),
                new Token(TokenType.IDENTIFIER, "INTEGER"),
                new Token(TokenType.IDENTIFIER, "STRING")
        );
    }
    
    public void testInvalidVisibility() {
        try { createParser("{AND"); parser.handleVisibility(); fail(); } catch(SyntaxException e) { }
        try { createParser("{AND,"); parser.handleVisibility(); fail(); } catch(SyntaxException e) { }
        try { createParser("{AND,}"); parser.handleVisibility(); fail(); } catch(SyntaxException e) { }
    }
    
    // note: expression parsing is already tested extensively
    // in ExpressionParserTestCase, this is just a simple integration test
    public void testExpressions() {
        createParser("-(not(-123)*3 or false\\\\some_real)");
        assertEquals(
                new UnaryExpressionAST(
                        new Token(Value.MINUS),
                        new BinaryExpressionAST(
                                new BinaryExpressionAST(
                                        new UnaryExpressionAST(
                                                new Token(Value.NOT),
                                                new UnaryExpressionAST(
                                                        new Token(Value.MINUS),
                                                        new SimpleExpressionAST(new Token(TokenType.INT_LITERAL, "123"))
                                                )
                                        ),
                                        new Token(Value.MULTIPLY),
                                        new SimpleExpressionAST(new Token(TokenType.INT_LITERAL, "3"))
                                ),
                                new Token(Value.OR),
                                new BinaryExpressionAST(
                                        new SimpleExpressionAST(new Token(Value.FALSE)),
                                        new Token(Value.REMAINDER),
                                        new SimpleExpressionAST(new Token(TokenType.IDENTIFIER, "some_real"))
                                )
                        )
                ),
                parser.handleExpression()
        );
    }
    
    public void testAssignment() {
        createParser("id := id + 36");
        assertEquals(
                new AssignmentAST(
                        TestCaseUtil.id("id"),
                        new BinaryExpressionAST(
                                new SimpleExpressionAST(TestCaseUtil.id("id")),
                                new Token(Value.PLUS),
                                new SimpleExpressionAST(new Token(TokenType.INT_LITERAL, "36"))
                        )
                ),
                parser.handleAssignment()
        );
    }
    
    public void testInstruction() {
        
        // conditional
        createParser("if 2 < 3 then end");
        assertEquals(
                new ConditionalAST(
                        new IfStatementAST(
                                new BinaryExpressionAST(
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2"),
                                        new Token(Value.LESS),
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                                ),
                                null
                        ),
                        EMPTY_IFS,
                        null
                ),
                parser.handleInstruction()
        );
        
        // loop
        createParser("from until 1 loop end");
        assertEquals(
                new IterationAST(
                        null,
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1"),
                        null
                ),
                parser.handleInstruction()
        );
        
        // construction
        createParser("!! myObj");
        assertEquals(
                new ConstructionAST(TestCaseUtil.id("myObj")),
                parser.handleInstruction()
        );
        
        // assignment
        createParser("abc := 2 + 3");
        assertEquals(
                new AssignmentAST(
                        TestCaseUtil.id("abc"),
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2"),
                                new Token(Value.PLUS),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                        )
                ),
                parser.handleInstruction()
        );
        
        // expressions
        createParser("abc + 2 + 3");
        assertEquals(
                new BinaryExpressionAST(
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "abc"),
                                new Token(Value.PLUS),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2")
                        ),
                        new Token(Value.PLUS),
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                ),
                parser.handleInstruction()
        );
        createParser("abc (2 + 3)");
        assertEquals(
                new InvocationAST(
                        TestCaseUtil.id("abc"),
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2"),
                                new Token(Value.PLUS),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                        )
                ),
                parser.handleInstruction()
        );
        createParser("-2");
        assertEquals(
                new UnaryExpressionAST(
                        new Token(Value.MINUS),
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2")
                ),
                parser.handleInstruction()
        );
    }
    
    public void testInstructions() {
        
        createParser("");
        assertNull(parser.handleInstructions());

        // big integration test for all the instruction types
        createParser(
                "do\n" +
                "  local z : INTEGER is 3\n" +
                "  !! a\n" +
                "  abc := c + 3\n" +
                "  !! b\nobj.invoke(2*3)\n" +
                "  from until 1 loop do !! c end end\n" +
                "  if 0 = 1 then do !! d end end\n" +
                "end");
        InstructionsAST bigOne = new InstructionsAST(
                Arrays.asList(new VariableDeclAST(
                                TestCaseUtil.id("z"),
                                TestCaseUtil.id("INTEGER"),
                                new Token(TokenType.INT_LITERAL, "3")
                )),
                new ConstructionAST(TestCaseUtil.id("a")),
                new AssignmentAST(
                        TestCaseUtil.id("abc"),
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "c"),
                                new Token(Value.PLUS),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                        )
                ),
                new ConstructionAST(TestCaseUtil.id("b")),
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "obj"),
                        new Token(Value.DOT),
                        new InvocationAST(
                                TestCaseUtil.id("invoke"),
                                new BinaryExpressionAST(
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2"),
                                        new Token(Value.MULTIPLY),
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "3")
                                )
                        )
                ),
                new IterationAST(
                        null,
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1"),
                        instructions(new ConstructionAST(TestCaseUtil.id("c")))
                ),
                new ConditionalAST(
                        new IfStatementAST(
                                new BinaryExpressionAST(
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "0"),
                                        new Token(Value.EQUALITY),
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1")
                                ),
                                instructions(new ConstructionAST(TestCaseUtil.id("d")))
                        ),
                        EMPTY_IFS,
                        null
                )
        );
        assertEquals(bigOne, parser.handleInstructions());
        
    }

    public void testIteration() {
        createParser("from\ndo\ni := 0\nend\nuntil\ni > 5\nloop\ndo\ni := i + 1\nend\nend");
        IterationAST loop = new IterationAST(
                instructions(new AssignmentAST(
                        TestCaseUtil.id("i"),
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "0")
                )),
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "i"),
                        new Token(Value.GREATER),
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "5")
                ),
                instructions(new AssignmentAST(
                        TestCaseUtil.id("i"),
                        new BinaryExpressionAST(
                                TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "i"),
                                new Token(Value.PLUS),
                                TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1")
                        )
                ))
        );
        assertEquals(loop, parser.handleIteration());
        createParser("from do i := 0 end until i > 5 loop do i := i + 1 end end");
        assertEquals(loop, parser.handleIteration());
        // bare minimum loop
        createParser("from until true loop end");
        assertEquals(
                new IterationAST(
                        null,
                        new SimpleExpressionAST(new Token(Value.TRUE)),
                        null
                ),
                parser.handleIteration()
        );
        // inner loop
        createParser("from until 0 loop do from until 1 loop do 2 end end end end");
        assertEquals(
                new IterationAST(
                        null,
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "0"),
                        instructions(
                                new IterationAST(
                                        null,
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1"),
                                        instructions(TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2"))
                                )
                        )
                ),
                parser.handleIteration()
        );
    }
    
    public void testInvalidIteration() {
        try {
            createParser("from");
            parser.handleIteration();
            fail();
        } catch(SyntaxException e) {
            assertErrorDetails(e, null, Value.UNTIL, TokenType.EOF);
        }
        try {
            createParser("from until true loop do 2+3 end");
            parser.handleIteration();
            fail();
        } catch(SyntaxException e) {
            assertErrorDetails(e, null, Value.END, TokenType.EOF);
        }
    }
    
    public void testElse() {
        createParser("");
        assertNull(parser.handleElse());
        createParser("else\ndo method(1+2)\nend");
        assertEquals(
                new InstructionsAST(
                        EMPTY_VARS,
                        new InvocationAST(
                                TestCaseUtil.id("method"),
                                new BinaryExpressionAST(
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1"),
                                        new Token(Value.PLUS),
                                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "2")
                                )
                        )
                ),
                parser.handleElse()
        );
    }
    
    public void testElseIfs() {
        createParser("elseif\ntrue = false\nthen\ndo false = true end");
        assertEquals(
                new IfStatementAST(
                        new BinaryExpressionAST(
                                new SimpleExpressionAST(new Token(Value.TRUE)),
                                new Token(Value.EQUALITY),
                                new SimpleExpressionAST(new Token(Value.FALSE))
                        ),
                        new InstructionsAST(
                                EMPTY_VARS,
                                new BinaryExpressionAST(
                                        new SimpleExpressionAST(new Token(Value.FALSE)),
                                        new Token(Value.EQUALITY),
                                        new SimpleExpressionAST(new Token(Value.TRUE))
                                )
                        )
                ),
                parser.handleElseIfs()
        );
    }
    
    public void testConditional() {
        InstructionsAST assignOneToX = new InstructionsAST(
                EMPTY_VARS,
                new AssignmentAST(
                        TestCaseUtil.id("x"),
                        TestCaseUtil.simpleExpr(TokenType.INT_LITERAL, "1")
                )
        );
        IfStatementAST stmt = new IfStatementAST(
                new BinaryExpressionAST(
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "a"),
                        new Token(Value.EQUALITY),
                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "b")
                ),
                assignOneToX
        );
        // if
        createParser("if\na=b\nthen do x:=1 end end\n");
        assertEquals(
                new ConditionalAST(
                        stmt,
                        EMPTY_IFS,
                        null
                ),
                parser.handleConditional()
        );
        // if-else
        createParser("if\na=b\nthen do x:=1 end else do x:=1 end end\n");
        assertEquals(
                new ConditionalAST(
                        stmt,
                        EMPTY_IFS,
                        assignOneToX
                ),
                parser.handleConditional()
        );
        // if-elseif-elseif-else
        createParser(
                "if     a=b then do x:=1 end " +
                "elseif a=b then do x:=1 end " +
                "elseif a=b then do x:=1 end " +
                "else            do x:=1  end " +
                "end"
        );
        assertEquals(
                new ConditionalAST(
                        stmt,
                        Arrays.asList(stmt, stmt),
                        assignOneToX
                ),
                parser.handleConditional()
        );
    }
    
    public void testMethod() {
        /*
         * METHOD  ::= <identifier> <params> <return type> is 
            <local declarations>
            <instructions>
         */
        createParser(
                "round(a : REAL) : INTEGER is\n" +
                "  local x, y : CHAR is 'z'\n" +
                "  do\n" +
                "    !!x\n" +
                "    x := y\n" +
                "  end\n"
        );
        assertEquals(
                new MethodAST(
                        TestCaseUtil.id("round"),
                        Arrays.asList(
                                new ParamDeclAST(TestCaseUtil.id("a"), TestCaseUtil.id("REAL"))
                        ),
                        TestCaseUtil.id("INTEGER"),
                        Arrays.asList(
                                new VariableDeclAST(
                                        TestCaseUtil.id("x"),
                                        TestCaseUtil.id("CHAR"),
                                        new Token(TokenType.CHAR_LITERAL, "z")
                                ),
                                new VariableDeclAST(
                                        TestCaseUtil.id("y"),
                                        TestCaseUtil.id("CHAR"),
                                        new Token(TokenType.CHAR_LITERAL, "z")
                                )
                        ),
                        instructions(
                                new ConstructionAST(TestCaseUtil.id("x")),
                                new AssignmentAST(
                                        TestCaseUtil.id("x"),
                                        TestCaseUtil.simpleExpr(TokenType.IDENTIFIER, "y")
                                )
                        )
                ),
                parser.handleMethod()
        );
        
    }

    public void testFeature() {
        // test material
        List<ParamDeclAST> emptyParams = Collections.emptyList();
        List<VariableDeclAST> emptyVars = Collections.emptyList();
        List<Token> emptyVisibility = Collections.emptyList();
        List<FeatureAST> emptyFeatures = Collections.emptyList();
        
        createParser("feature");
        assertEquals(
                new FeatureBlockAST(null, emptyFeatures),
                parser.handleFeature()
        );
        
        createParser("feature {X,Y}");
        assertEquals(
                new FeatureBlockAST(
                        Arrays.asList(TestCaseUtil.id("X"), TestCaseUtil.id("Y")),
                        emptyFeatures
                ),
                parser.handleFeature()
        );
        
        createParser("feature {}\na:INTEGER");
        assertEquals(
                new FeatureBlockAST(
                        emptyVisibility,
                        Arrays.asList(
                                (FeatureAST)new VariableDeclAST(
                                        TestCaseUtil.id("a"),
                                        TestCaseUtil.id("INTEGER"),
                                        null
                                )
                        )
                ),
                parser.handleFeature()
        );
        
        createParser(
                "feature {ANY}\n" +
                "a is\n" +
                "b() is\n" +
                "id1, id2 : INTEGER\n" +
                "c(x:X) is\n" +
                "id3 : CHAR is 'w'\n" +
                "d(y:Y) : Z is\n" +
                "e : F is\n" +
                "local o:p"
        );
        FeatureBlockAST bigOne = new FeatureBlockAST(
                Arrays.asList(TestCaseUtil.id("ANY")),
                Arrays.asList(
                        (FeatureAST)new MethodAST(
                                TestCaseUtil.id("a"),
                                emptyParams,
                                null,
                                emptyVars,
                                null
                        ),
                        (FeatureAST)new MethodAST(
                                TestCaseUtil.id("b"),
                                emptyParams,
                                null,
                                emptyVars,
                                null
                        ),
                        (FeatureAST)new VariableDeclAST(
                                TestCaseUtil.id("id1"),
                                TestCaseUtil.id("INTEGER"),
                                null
                        ),
                        (FeatureAST)new VariableDeclAST(
                                TestCaseUtil.id("id2"),
                                TestCaseUtil.id("INTEGER"),
                                null
                        ),
                        (FeatureAST)new MethodAST(
                                TestCaseUtil.id("c"),
                                Arrays.asList(
                                        new ParamDeclAST(
                                                TestCaseUtil.id("x"),
                                                TestCaseUtil.id("X"))
                                ),
                                null,
                                emptyVars,
                                null
                        ),
                        (FeatureAST)new VariableDeclAST(
                                TestCaseUtil.id("id3"),
                                TestCaseUtil.id("CHAR"),
                                new Token(TokenType.CHAR_LITERAL, "w")
                        ),
                        (FeatureAST)new MethodAST(
                                TestCaseUtil.id("d"),
                                Arrays.asList(
                                        new ParamDeclAST(
                                                TestCaseUtil.id("y"),
                                                TestCaseUtil.id("Y"))
                                ),
                                TestCaseUtil.id("Z"),
                                emptyVars,
                                null
                        ),
                        (FeatureAST)new MethodAST(
                                TestCaseUtil.id("e"),
                                emptyParams,
                                TestCaseUtil.id("F"),
                                Arrays.asList(
                                        new VariableDeclAST(
                                                TestCaseUtil.id("o"),
                                                TestCaseUtil.id("p"),
                                                null
                                        )
                                ),
                                null
                        )
                )
        );
        //System.out.println(bigOne);
        //System.out.println(parser.handleFeature());
        assertEquals(bigOne, parser.handleFeature());
    }
    
    public void testInvalidFeature() {
        try {
            createParser("feature\na a");
            parser.handleFeature();
            fail();
        } catch(SyntaxException e) { }
        try {
            createParser("feature\na:");
            parser.handleFeature();
            fail();
        } catch(SyntaxException e) { }
    }

    public void testFeatures() {
        createParser(
                "feature {NONE}\n" +
                "  a : INTEGER\n" +
                "feature {ANY}\n" +
                "  b : INTEGER\n"
        );
        FeatureBlockAST first = new FeatureBlockAST(
                Arrays.asList(TestCaseUtil.id("NONE")),
                Arrays.asList(
                        (FeatureAST)new VariableDeclAST(
                                TestCaseUtil.id("a"),
                                TestCaseUtil.id("INTEGER"),
                                null
                        )
                )
        );
        FeatureBlockAST second = new FeatureBlockAST(
                Arrays.asList(TestCaseUtil.id("ANY")),
                Arrays.asList(
                        (FeatureAST)new VariableDeclAST(
                                TestCaseUtil.id("b"),
                                TestCaseUtil.id("INTEGER"),
                                null
                        )
                )
        );
        assertEquals(parser.handleFeatures(), Arrays.asList(first, second));
    }

    public void testClassDef() {
        createParser(
                "class CLASS_A\n" +
                "  feature\n" +
                "    a : INTEGER\n" +
                "  feature{WHATEVER}\n" +
                "    b : REAL\n" +
                "end"
        );
        assertEquals(
                new ClassAST(
                        TestCaseUtil.id("CLASS_A"),
                        Arrays.asList(
                                new FeatureBlockAST(
                                        null,
                                        Arrays.asList(
                                                (FeatureAST)new VariableDeclAST(
                                                        TestCaseUtil.id("a"),
                                                        TestCaseUtil.id("INTEGER"),
                                                        null
                                                )
                                        )
                                ),
                                new FeatureBlockAST(
                                        Arrays.asList(TestCaseUtil.id("WHATEVER")),
                                        Arrays.asList(
                                                (FeatureAST)new VariableDeclAST(
                                                        TestCaseUtil.id("b"),
                                                        TestCaseUtil.id("REAL"),
                                                        null
                                                )
                                        )
                                )
                        )
                ),
                parser.handleClassDef()
        );
    }

    public void testProgram() {
        
        List<ClassAST> emptyClasses = Collections.emptyList();
        List<FeatureBlockAST> emptyFeats = Collections.emptyList();
        
        createParser("");
        assertEquals(
                new ProgramAST(emptyClasses),
                parser.handleProgram()
        );

        createParser("\n\n");
        assertEquals(
                new ProgramAST(emptyClasses),
                parser.handleProgram()
        );
        
        createParser(
                "class first\nend\n\n\nclass second\nend"
        );
        assertEquals(
                new ProgramAST(
                        Arrays.asList(
                                new ClassAST(
                                        TestCaseUtil.id("first"),
                                        emptyFeats
                                ),
                                new ClassAST(
                                        TestCaseUtil.id("second"),
                                        emptyFeats
                                )
                        )
                ),
                parser.handleProgram()
        );

    }
    
    public void testInvalidProgram() {
        createParser("\n\nMyId\n\n");
        try {
            parser.handleProgram();
            fail();
        } catch(SyntaxException e) { }
    }

}
