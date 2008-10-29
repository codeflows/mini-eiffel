package minieiffel.semantics;

import java.io.StringReader;

import junit.framework.TestCase;
import minieiffel.Lexer;
import minieiffel.Parser;
import minieiffel.Source;
import minieiffel.TestCaseUtil;
import minieiffel.Token;
import minieiffel.Token.TokenType;
import minieiffel.ast.AssignmentAST;
import minieiffel.ast.ConstructionAST;
import minieiffel.ast.ExpressionAST;
import minieiffel.ast.ProgramAST;
import minieiffel.ast.VariableDeclAST;

/**
 * Test case for the semantic analyzer. Most of the semantic
 * rules are tested here, but TypeInferenceTestCase tests
 * the expression type inference functionality and
 * SignatureResolverTestCase tests signature resolving.
 */
public class SemanticAnalyzerTestCase extends TestCase {

    private DefaultSemanticAnalyzer analyzer;
    private ProgramAST program;
    
    protected void setUp() {
        
        // program structure for this test case
        // (only the signature, implementations are left out)
        String signature =
            "class A\n" +
            " feature {}\n" +
            "  a is\n" +
            "  b : INTEGER\n" +
            "  myC : C\n" +
            "  whatever : ANY\n" +
            " feature {B}\n" +
            "  c : REAL\n" +
            " feature\n" +
            "  d(e:INTEGER) is \n" +
            "end\n" +
            "class B\n" +
            " feature {C}" +
            "  f : BOOLEAN\n" +
            "  someBMethod is\n" +
            "end\n" +
            "class C\n" +
            " feature\n" +
            "  g(h:A) : B is\n" +
            "  i : CHARACTER\n" +
            "  j : ANY\n" +
            "  k(m:ANY) : A is\n" +
            "  ambi(a:INTEGER) : INTEGER is\n" +
            "  ambi(b:REAL) : REAL is\n" +
            "end\n" +
            "class D\n" +
            " feature\n" +
            "  z : INTEGER\n" +
            "  PI : REAL is 3.14\n" +
            "  x() is\n" +
            "end";
        
        program = createParser(signature).handleProgram();
        
        // create a semantic analyzer whose "analyzeStructure()"
        // -method is overridden not to analyze the structure
        // of a program automatically: instead, the test methods
        // in this class will do so "manually", i.e. by calling
        // the visitor methods on the analyzer instance
        analyzer = new DefaultSemanticAnalyzer() {
            // override to do nothing
            protected void analyzeStructure(ProgramAST program) { }
        };
    }
    
    private Parser createParser(String code) {
        Source source = new Source(new StringReader(code));
        return new Parser(new Lexer(source));
    }

    private void enterClassAndMethod() {
        enterClassAndMethod("A", "d", Type.INTEGER);
    }
    
    /**
     * Enters the given class and method.
     */
    private void enterClassAndMethod(String klass, String method, Type... types) {
        analyzer.analyze(program);
        analyzer.enteringClass(klass);
        analyzer.enteringMethod(method, types);
        analyzer.enteringBlock();
    }
    
    /**
     * Leaves the current class and method.
     */
    private void leaveClassAndMethod() {
        analyzer.leavingBlock();
        analyzer.leavingMethod();
        analyzer.leavingClass();
    }
    
    /**
     * Visits a variable declaration.
     */
    private VariableDeclAST declareVariable(String name, String typeName) {
        VariableDeclAST var = new VariableDeclAST(
                TestCaseUtil.id(name),
                TestCaseUtil.id(typeName),
                null
        );
        analyzer.visit(var);
        return var;
    }

    /**
     * Visits the given expression and ensures its type is correct.
     */
    private void visitExpression(String source, Type expectedType) {
        ExpressionAST expr = createParser(source).handleExpression();
        analyzer.visit(expr);
        assertEquals(expectedType, expr.getType());
    }
    
    /* actual tests follow */

    public void testResolvingVariable() {
        enterClassAndMethod();
        ExpressionAST expr = createParser("b").handleExpression();
        assertNull(expr.getType());
        analyzer.visit(expr);
        assertEquals(Type.INTEGER, expr.getType());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testResolvingVariableFromAnotherClass() {
        enterClassAndMethod();
        ExpressionAST expr = createParser("myC.i").handleExpression();
        assertNull(expr.getType());
        analyzer.visit(expr);
        assertEquals(Type.CHARACTER, expr.getType());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testResolvingUnknownVariable() {
        enterClassAndMethod();
        analyzer.visit(createParser("nosuchthing * 150").handleExpression());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "No variable \"nosuchthing\" defined " +
                        "in class \"A\"",
                        null
                )
        );
    }
    
    public void testResolvingMethod() {
        enterClassAndMethod();
        ExpressionAST expr = createParser("d(123)").handleExpression();
        assertNull(expr.getType());
        analyzer.visit(expr);
        assertEquals(Type.VOID, expr.getType());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testResolvingMethodFromAnotherClass() {
        enterClassAndMethod();
        ExpressionAST expr = createParser("myC.g(current)").handleExpression();
        assertNull(expr.getType());
        analyzer.visit(expr);
        assertEquals(new Type("B"), expr.getType());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testResolvingUnknownMethod() {
        enterClassAndMethod();
        analyzer.visit(createParser("true and nonexistent(2.35,current)").handleExpression());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError("Undefined method \"nonexistent(REAL,A)\"", null)
        );
    }
    
    public void testAmbiguousMethodResolving() {
        enterClassAndMethod();
        ExpressionAST expr = createParser("myC.ambi(123)").handleExpression();
        analyzer.visit(expr);
        assertEquals(Type.INTEGER, expr.getType());
        expr = createParser("myC.ambi(1.23)").handleExpression();
        analyzer.visit(expr);
        assertEquals(Type.REAL, expr.getType());
        expr = createParser("myC.ambi(void)").handleExpression();
        analyzer.visit(expr);
        assertEquals(null, expr.getType());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError("Ambiguous method call \"ambi(ANY)\"", null)
        );
    }
        
    public void testResolvingVoid() {
        enterClassAndMethod();
        ExpressionAST expr = createParser("void").handleExpression();
        analyzer.visit(expr);
        assertEquals(Type.ANY, expr.getType());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testResolvingCurrent() {
        enterClassAndMethod();
        ExpressionAST expr = createParser("current").handleExpression();
        analyzer.visit(expr);
        assertEquals(new Type("A"), expr.getType());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }

    public void testResolvingResultInVoidMethod() {
        enterClassAndMethod();
        ExpressionAST expr = createParser("result").handleExpression();
        analyzer.visit(expr);
        assertNull(expr.getType());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "No variable \"result\" defined " +
                        "in class \"A\"",
                        null
                )
        );
    }
    
    public void testResolvingResultInMethodWithReturnType() {
        analyzer.analyze(program);
        analyzer.enteringClass("C");
        analyzer.enteringMethod("g", new Type("A"));
        analyzer.enteringBlock();
        ExpressionAST expr = createParser("result").handleExpression();
        analyzer.visit(expr);
        assertEquals(new Type("B"), expr.getType());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testVoidAsMethodParameter() {
        enterClassAndMethod();
        ExpressionAST expr = createParser("d(void)").handleExpression();
        assertNull(expr.getType());
        analyzer.visit(expr);
        assertEquals(Type.VOID, expr.getType());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testVoidAsAssignmentExpression() {
        enterClassAndMethod();
        analyzer.visit(createParser("b := void").handleAssignment());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testCurrentAndVoidAsAssignmentTarget() {
        enterClassAndMethod();
        analyzer.visit(createParser("current := 2").handleAssignment());
        analyzer.visit(createParser("void := 3").handleAssignment());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError("Can't assign to special variable 'current'", null),
                new SemanticError("Can't assign to special variable 'void'", null)
        );
    }
    
    public void testAssignmentsMustBeMadeToConformingType() {
        enterClassAndMethod();
        analyzer.visit(createParser("b := true").handleAssignment());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError("Can't assign BOOLEAN value to variable of type INTEGER", null)
        );
    }
    
    public void testVoidReturnTypeAsAssignmentExpression() {
        enterClassAndMethod();
        analyzer.visit(createParser("b := a()").handleAssignment());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError("Can't assign VOID value to variable of type INTEGER", null)
        );
    }
    
    public void testAssigningToAny() {
        enterClassAndMethod();
        analyzer.visit(createParser("whatever := 3").handleAssignment());
        analyzer.visit(createParser("whatever := true").handleAssignment());
        analyzer.visit(createParser("whatever := void").handleAssignment());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testAssigningAny() {
        enterClassAndMethod();
        analyzer.visit(createParser("b := whatever").handleAssignment());
        analyzer.visit(createParser("myC := whatever").handleAssignment());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError("Can't assign ANY value to variable of type INTEGER", null),
                new SemanticError("Can't assign ANY value to variable of type C", null)
        );
    }
    
    public void testAnyParameter() {
        enterClassAndMethod();
        ExpressionAST expr;
        analyzer.visit((expr = createParser("myC.k(3)").handleExpression()));
        assertEquals(new Type("A"), expr.getType());
        analyzer.visit((expr = createParser("myC.k(void)").handleExpression()));
        assertEquals(new Type("A"), expr.getType());
        analyzer.visit((expr = createParser("myC.k(5.0)").handleExpression()));
        assertEquals(new Type("A"), expr.getType());
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }

    public void testIfElseIfAndUntilMustBeBoolean() {
        enterClassAndMethod();
        analyzer.visit(createParser("if 3 then elseif 4 then end").handleConditional());
        analyzer.visit(createParser("from until 5 loop end").handleIteration());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError("\"if\" must be of type BOOLEAN", null),
                new SemanticError("\"elseif\" must be of type BOOLEAN", null),
                new SemanticError("\"until\" must be of type BOOLEAN", null)
        );
    }
    
    public void testConstruction() {
        analyzer.analyze(program);
        analyzer.enteringClass("C");
        analyzer.enteringMethod("k", Type.INTEGER);
        analyzer.enteringBlock();
        ConstructionAST c;
        analyzer.visit((c = createParser("!! i").handleConstruction()));
        assertEquals(Type.CHARACTER, c.getType());
        analyzer.visit((c = createParser("!! notfound").handleConstruction()));
        assertNull(c.getType());
        analyzer.visit((c = createParser("!! current").handleConstruction()));
        assertNull(c.getType());
        analyzer.visit((c = createParser("!! result").handleConstruction()));
        assertNull(c.getType());
        analyzer.visit((c = createParser("!! void").handleConstruction()));
        assertNull(c.getType());
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError("No variable \"notfound\" defined in class \"C\"", null),
                new SemanticError("Can't construct special variable 'current'", null),
                new SemanticError("Can't construct special variable 'result'", null),
                new SemanticError("Can't construct special variable 'void'", null)
        );
    }
    
    public void testLocalVariables() {
        enterClassAndMethod();
        
        VariableDeclAST localVarA = declareVariable("aaa", "C");
        assertEquals(new Type("C"), localVarA.getType());

        VariableDeclAST localVarB = declareVariable("bbb", "INTEGER");
        assertEquals(Type.INTEGER, localVarB.getType());
        
        ExpressionAST expr;
        expr = createParser("b").handleExpression();
        analyzer.visit(expr);
        assertEquals(Type.INTEGER, expr.getType());

        expr = createParser("aaa.k(void)").handleExpression();
        analyzer.visit(expr);
        assertEquals(new Type("A"), expr.getType());
        
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testLocalVariableTypeNotFound() {
        enterClassAndMethod();
        
        VariableDeclAST x = declareVariable("x", "NOT_FOUND");
        assertNull(x.getType());

        leaveClassAndMethod();
        
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError("Unknown class \"NOT_FOUND\"", null)
        );
    }
    
    /**
     * "A variable must not be declared twice in the same scope"
     */
    public void testRedeclaringVariableInSameScopeFails() {
        enterClassAndMethod();
        
        VariableDeclAST firstX = declareVariable("x", "BOOLEAN");
        assertEquals(Type.BOOLEAN, firstX.getType());

        VariableDeclAST secondX = declareVariable("x", "INTEGER");
        assertNull(secondX.getType());

        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "Variable \"x\" already declared in this scope",
                        null
                )
        );
    }
    
    public void testRedeclaringVariableInInnerScopeFails() {
        enterClassAndMethod();
        
        VariableDeclAST firstX = declareVariable("x", "BOOLEAN");
        assertEquals(Type.BOOLEAN, firstX.getType());
        
        analyzer.enteringBlock();

        VariableDeclAST secondX = declareVariable("x", "INTEGER");
        assertNull(secondX.getType());
        
        analyzer.leavingBlock();

        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "Variable \"x\" already declared in this scope",
                        null
                )
        );
    }
    
    public void testAccessingVariablesInInnerScope() {
        enterClassAndMethod();
        
        VariableDeclAST x = declareVariable("x", "BOOLEAN");
        assertEquals(Type.BOOLEAN, x.getType());
        
        analyzer.enteringBlock();

        VariableDeclAST y = declareVariable("y", "INTEGER");
        assertEquals(Type.INTEGER, y.getType());
        
        analyzer.visit(createParser("b").handleExpression());
        analyzer.visit(createParser("x").handleExpression());
        analyzer.visit(createParser("y").handleExpression());
        
        analyzer.leavingBlock();

        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    /**
     * "A variable declared in an inner scope, but not
     *  declared in an outer scope, cannot be accessed in
     *  the outer scope."
     */
    public void testVaribleDeclaredInInnerScopeCantBeAccessedFromOuterScope() {
        enterClassAndMethod();
        
        VariableDeclAST x = declareVariable("x", "BOOLEAN");
        assertEquals(Type.BOOLEAN, x.getType());
        
        analyzer.enteringBlock();

        VariableDeclAST y = declareVariable("y", "INTEGER");
        assertEquals(Type.INTEGER, y.getType());
        
        analyzer.visit(createParser("y").handleExpression());
        
        analyzer.leavingBlock();

        analyzer.visit(createParser("y").handleExpression());
        
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "No variable \"y\" defined in class \"A\"",
                        null
                )
        );
    }
    
    /**
     * 'result' can be declared in a method with void return type.
     */
    public void testReferencingResultInAMethodWithNoReturnTypeButWithResultDeclared() {
        enterClassAndMethod();
        
        VariableDeclAST result = declareVariable("result", "INTEGER");
        assertEquals(Type.INTEGER, result.getType());
        
        ExpressionAST expr = createParser("result").handleExpression();
        analyzer.visit(expr);
        assertEquals(Type.INTEGER, expr.getType());
        
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    /**
     * "Declaring a variable result in a function with return
     *  type is a semantic error."
     */
    public void testDeclaringResultInAFunctionWithReturnType() {
        analyzer.analyze(program);
        analyzer.enteringClass("C");
        analyzer.enteringMethod("g", new Type("A"));
        analyzer.enteringBlock();
        
        VariableDeclAST result = declareVariable("result", "INTEGER");
        assertNull(result.getType());
        
        ExpressionAST expr = createParser("result").handleExpression();
        analyzer.visit(expr);
        assertEquals(new Type("B"), expr.getType());
        
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "Can't redeclare special variable " +
                        "'result' in a method with a return " +
                        "type",
                        null
                )
        );
    }

    public void testVariableVisibility() {

        // C.f shouldn't be visible to A
        enterClassAndMethod();
        declareVariable("localB", "B");
        visitExpression("localB", new Type("B"));
        visitExpression("localB.f", null);
        leaveClassAndMethod();
        
        // but should be visible to C
        enterClassAndMethod("C", "g", new Type("A"));
        declareVariable("localB", "B");
        visitExpression("localB", new Type("B"));
        visitExpression("localB.f", Type.BOOLEAN);
        leaveClassAndMethod();
        
        // and to B itself
        enterClassAndMethod("B", "someBMethod");
        visitExpression("f", Type.BOOLEAN);
        leaveClassAndMethod();
        
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "Variable \"f\" is not visible " +
                        "to class \"A\"",
                        null
                )
        );
        
    }
    
    public void testMethodVisibility() {
        
        // A.a() should be visible inside A
        enterClassAndMethod();
        visitExpression("a()", Type.VOID);
        leaveClassAndMethod();
        
        // but not inside C
        enterClassAndMethod("C", "g", new Type("A"));
        visitExpression("k(true or false).a()", null);
        leaveClassAndMethod();
        
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "Method \"a()\" is not visible " +
                        "to class \"C\"",
                        null
                )
        );
        
    }
    
    public void testParameters() {
        
        //g(h:A) : B
        enterClassAndMethod("C", "g", new Type("A"));
        visitExpression("result", new Type("B"));
        visitExpression("h", new Type("A"));
        leaveClassAndMethod();

        //k(m:ANY) : A
        enterClassAndMethod("C", "k", Type.ANY);
        visitExpression("result", new Type("A"));
        visitExpression("m", Type.ANY);
        leaveClassAndMethod();
        
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
    public void testConstantValues() {
        enterClassAndMethod();
        analyzer.visit(
                new VariableDeclAST(
                        TestCaseUtil.id("someInt"),
                        TestCaseUtil.id("INTEGER"),
                        new Token(TokenType.INT_LITERAL, "1234")
                )
        );
        visitExpression("someInt", Type.INTEGER);
        analyzer.visit(
                new VariableDeclAST(
                        TestCaseUtil.id("someBoolean"),
                        TestCaseUtil.id("BOOLEAN"),
                        new Token(TokenType.REAL_LITERAL, "3.14")
                )
        );
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "\"3.14\" is an invalid constant value for type BOOLEAN",
                        null
                )
        );
    }
    
    public void testAssigningToConstantFails() {
        enterClassAndMethod("D", "x");
        analyzer.visit(
                new AssignmentAST(
                        TestCaseUtil.id("PI"),
                        TestCaseUtil.simpleExpr(TokenType.REAL_LITERAL, "1.0")
                )
        );
        leaveClassAndMethod();
        TestCaseUtil.assertListContents(
                analyzer.getErrors(),
                new SemanticError(
                        "Can't assign to constant value \"PI\"",
                        null
                )
        );
    }
    
    public void testVariableHiding() {
        enterClassAndMethod("D", "x");
        visitExpression("z", Type.INTEGER);
        visitExpression("current.z", Type.INTEGER);
        declareVariable("z", "BOOLEAN");
        visitExpression("z", Type.BOOLEAN);
        visitExpression("current.z", Type.INTEGER);
        leaveClassAndMethod();
        assertTrue(analyzer.getErrors().isEmpty());
    }
    
}
