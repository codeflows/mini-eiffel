package minieiffel.semantics;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import minieiffel.ExpressionParser;
import minieiffel.Lexer;
import minieiffel.Source;
import minieiffel.TestCaseUtil;
import minieiffel.Token;
import minieiffel.Token.Value;
import minieiffel.ast.ExpressionAST;

import org.easymock.MockControl;

public class TypeInferenceTestCase extends TestCase {

    private static final List<Type> EMPTY_TYPES = Collections.emptyList();
    
    private TypeInference inference;

    private SemanticAnalyzer analyzerMock;
    private MockControl analyzerMockControl;

    protected void setUp() {
        analyzerMockControl = MockControl.createControl(SemanticAnalyzer.class);
        analyzerMock = (SemanticAnalyzer)analyzerMockControl.getMock();
    }
    
    protected void checkType(Type type, String expr) {
        inference = new TypeInference(analyzerMock);
        ExpressionParser parser = new ExpressionParser(
                new Lexer(
                        new Source(new StringReader(expr))
                )
        );
        ExpressionAST exprAST = parser.handleExpression();
        assertEquals(
                "Wrong type for expression \"" + expr + "\"",
                type,
                inference.inferType(exprAST)
        );
    }
    
    public void testPrimitiveExpressions() {
        checkType(Type.INTEGER, "5");
        checkType(Type.REAL, "5.0");
        checkType(Type.CHARACTER, "'5'");
        checkType(Type.BOOLEAN, "true");
    }
    
    public void testIntegerOperations() {
        checkType(Type.INTEGER, "-5");
        checkType(Type.INTEGER, "5-5");
        checkType(Type.INTEGER, "5*5");
        checkType(Type.INTEGER, "5/5");
        checkType(Type.INTEGER, "5\\\\5");
    }
    
    public void testRealOperations() {
        checkType(Type.REAL, "-7.0");
        checkType(Type.REAL, "5 + 7.0");
        checkType(Type.REAL, "7.0 + 5");
        checkType(Type.REAL, "7.0 + 7.0");
        checkType(Type.REAL, "5 - 7.0");
        checkType(Type.REAL, "7.0 - 5");
        checkType(Type.REAL, "7.0 - 7.0");
        checkType(Type.REAL, "5 * 7.0");
        checkType(Type.REAL, "7.0 * 5");
        checkType(Type.REAL, "7.0 * 7.0");
        checkType(Type.REAL, "5 / 7.0");
        checkType(Type.REAL, "7.0 / 5");
        checkType(Type.REAL, "7.0 / 7.0");
    }
    
    public void testNumericalComparisonOperations() {
        checkType(Type.BOOLEAN, "5 = 5");
        checkType(Type.BOOLEAN, "5 /= 5");
        checkType(Type.BOOLEAN, "5 > 5");
        checkType(Type.BOOLEAN, "5 < 5");
        checkType(Type.BOOLEAN, "5 >= 5");
        checkType(Type.BOOLEAN, "5 <= 5");
        checkType(Type.BOOLEAN, "7.0 = 7.0");
        checkType(Type.BOOLEAN, "7.0 /= 7.0");
        checkType(Type.BOOLEAN, "7.0 > 7.0");
        checkType(Type.BOOLEAN, "7.0 < 7.0");
        checkType(Type.BOOLEAN, "7.0 >= 7.0");
        checkType(Type.BOOLEAN, "7.0 <= 7.0");
        checkType(Type.BOOLEAN, "5 > 7.0");
        checkType(Type.BOOLEAN, "7.0 > 5");
        checkType(Type.BOOLEAN, "5 < 7.0");
        checkType(Type.BOOLEAN, "7.0 < 5");
        checkType(Type.BOOLEAN, "5 >= 7.0");
        checkType(Type.BOOLEAN, "7.0 >= 5");
        checkType(Type.BOOLEAN, "5 <= 7.0");
        checkType(Type.BOOLEAN, "7.0 <= 5");
        // these weren't in the list on the course homepage,
        // but since >= <= are defined for reals and integers,
        // I guess these must be as well
        checkType(Type.BOOLEAN, "7.0 = 5");
        checkType(Type.BOOLEAN, "5 = 7.0");
        checkType(Type.BOOLEAN, "7.0 /= 5");
        checkType(Type.BOOLEAN, "5 /= 7.0");
    }
    
    public void testCharacterIsPromotedToAnInteger() {
        checkType(Type.INTEGER, "-'x'");
        checkType(Type.INTEGER, "5+'d'");
        checkType(Type.INTEGER, "2*'x'");
        checkType(Type.INTEGER, "3/'y'");
        checkType(Type.INTEGER, "1\\\\'a'");
        checkType(Type.INTEGER, "'d'+5");
        checkType(Type.INTEGER, "'x'*2");
        checkType(Type.INTEGER, "'y'/3");
        checkType(Type.INTEGER, "'a'\\\\1");
        checkType(Type.REAL, "5.0+'d'");
        checkType(Type.REAL, "'x'*2.0");
        checkType(Type.REAL, "3.0/'y'");
        checkType(Type.BOOLEAN, "3 >= 'e'");
        checkType(Type.BOOLEAN, "'a' = 9");
        checkType(Type.BOOLEAN, "3.0 >= 'e'");
        checkType(Type.BOOLEAN, "'a' = 9.0");
    }
    
    public void testCharacterOperations() {
        /* TODO how are these defined? char + char, char - char */
        checkType(Type.INTEGER, "'a'+'b'");
        checkType(Type.INTEGER, "'a'-'b'");
    }
    
    public void testBooleanOperations() {
        checkType(Type.BOOLEAN, "true = true");
        checkType(Type.BOOLEAN, "true /= true");
        checkType(Type.BOOLEAN, "not true");
        checkType(Type.BOOLEAN, "true and true");
        checkType(Type.BOOLEAN, "true and then true");
        checkType(Type.BOOLEAN, "true or true");
        checkType(Type.BOOLEAN, "true xor true");
        checkType(Type.BOOLEAN, "true or else true");
    }
    
    public void testInvalidOperations() {
        
        analyzerMock.addError("Operation '-' not defined for BOOLEAN", new Token(Value.MINUS));
        analyzerMockControl.replay();
        checkType(null, "-true");
        analyzerMockControl.verify();
        analyzerMockControl.reset();
        
        analyzerMock.addError("Operation 'not' not defined for INTEGER", new Token(Value.NOT));
        analyzerMockControl.replay();
        checkType(null, "not 3");
        analyzerMockControl.verify();
        analyzerMockControl.reset();
        
        analyzerMock.addError("Operation '+' not defined for INTEGER, BOOLEAN", new Token(Value.PLUS));
        analyzerMockControl.replay();
        checkType(null, "2+true");
        analyzerMockControl.verify();
        analyzerMockControl.reset();
        
        analyzerMock.addError("Operation 'and then' not defined for REAL, BOOLEAN", new Token(Value.AND_THEN));
        analyzerMockControl.replay();
        checkType(null, "3.0 and then true");
        analyzerMockControl.verify();
        analyzerMockControl.reset();
        
        analyzerMock.addError("Operation 'xor' not defined for CHARACTER, INTEGER", new Token(Value.XOR));
        analyzerMockControl.replay();
        checkType(null, "'a' xor 3");
        analyzerMockControl.verify();
        analyzerMockControl.reset();
        
    }
    
    public void testHigherLevelExpressions() {
        checkType(Type.INTEGER, "1+2+3+4+5");
        checkType(Type.REAL, "(2 * 3 + 4) - (560 / 3.0)");
        checkType(Type.BOOLEAN, "56=78 or 34/=90 and then true or false");
    }
    
    public void testInvalidHigherLevelExpressions() {
        
        analyzerMock.addError("Operation '\\\\' not defined for REAL, REAL", new Token(Value.REMAINDER));
        analyzerMock.addError("Operation '+' not defined for INTEGER, BOOLEAN", new Token(Value.PLUS));
        analyzerMockControl.replay();
        checkType(null, "(3.0 \\\\ 2.5) - (100 + true)");
        analyzerMockControl.verify();
        analyzerMockControl.reset();
        
        analyzerMock.addError("Operation '+' not defined for BOOLEAN, INTEGER", new Token(Value.PLUS));
        analyzerMockControl.replay();
        checkType(null, "-(false + 3)");
        analyzerMockControl.verify();
        
    }
    
    public void testPrimitiveVariable() {

        analyzerMock.resolveVariableType(null, TestCaseUtil.id("a"));
        analyzerMockControl.setReturnValue(Type.INTEGER, 3);
        analyzerMockControl.replay();
        checkType(Type.INTEGER, "a");
        checkType(Type.INTEGER, "-a");
        checkType(Type.INTEGER, "a+3");
        analyzerMockControl.verify();
        
    }
    
    public void testUserTypeVariable() {

        Type userType = new Type("USER_TYPE");
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("b"));
        analyzerMockControl.setReturnValue(userType);
        analyzerMockControl.replay();
        checkType(userType, "b");
        analyzerMockControl.verify();
        
    }
    
    public void testUserTypeFieldAccess() {

        Type typeC = new Type("TYPE_C");
        Type typeD = new Type("TYPE_D");
        
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("c"));
        analyzerMockControl.setReturnValue(typeC);
        analyzerMock.resolveVariableType(typeC, TestCaseUtil.id("d"));
        analyzerMockControl.setReturnValue(typeD);
        analyzerMockControl.replay();
        checkType(typeD, "c.d");
        analyzerMockControl.verify();
        
    }
    
    public void testPrimitiveVariablesInsideUserTypes() {
        
        Type typeX = new Type("TYPE_X");
        
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("x"));
        analyzerMockControl.setReturnValue(typeX);
        analyzerMock.resolveVariableType(typeX, TestCaseUtil.id("someInt"));
        analyzerMockControl.setReturnValue(Type.INTEGER);
        analyzerMockControl.replay();
        checkType(Type.INTEGER, "x.someInt + 2");
        analyzerMockControl.verify();
    }

    // XXX: the documentation states that only the operator
    //      '.' is allowed for user-defined types, but I guess
    //      equality is as well, as it's used in the examples
    public void testOnlyDotAndEqualityIsAllowedForUserTypes() {

        Type typeX = new Type("TYPE_X");
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("x"));
        analyzerMockControl.setReturnValue(typeX);
        analyzerMock.addError(
                "The operator '+' can't be used on the " +
                "user-defined type TYPE_X",
                new Token(Value.PLUS)
        );
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("x"));
        analyzerMockControl.setReturnValue(typeX, 2);
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("y"));
        analyzerMockControl.setReturnValue(Type.ANY);
        analyzerMockControl.replay();
        checkType(null, "x + 2");
        checkType(Type.BOOLEAN, "x = 5.0");
        checkType(Type.BOOLEAN, "y /= x");
        analyzerMockControl.verify();
        
    }
    
    public void testUserTypeDotPrimitiveIsNotAllowed() {
        Type a = new Type("A");
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("a"));
        analyzerMockControl.setReturnValue(a);
        analyzerMock.addError(
                "The right-hand side value of the operator " +
                "'.' must be an identifier or a method call" +
                " (e.g. \"a.someVar\" or \"a.someMethod()\")",
                new Token(Value.DOT)
        );
        analyzerMockControl.replay();
        checkType(null, "a.2");
        analyzerMockControl.verify();
    }
    
    public void testPrimitiveDotAnythingIsNotAllowed() {
        analyzerMock.addError(
                "The operator '.' is not allowed on primitive values",
                new Token(Value.DOT)
        );
        analyzerMockControl.replay();
        checkType(null, "true.method()");
        analyzerMockControl.verify();
    }
    
    public void testPrimitivePlusUserTypeIsNotAllowed() {
        Type a = new Type("A");
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("myObj"));
        analyzerMockControl.setReturnValue(a);
        analyzerMock.addError(
                "Operation '+' not defined for INTEGER, A",
                new Token(Value.PLUS)
        );
        analyzerMockControl.replay();
        checkType(null, "(123) + myObj");
        analyzerMockControl.verify();
    }
    
    public void testPlainInvocation() {
        Type math = new Type("MATH");
        analyzerMock.resolveMethodType(
                null,
                TestCaseUtil.id("sum"),
                Arrays.asList(Type.INTEGER, Type.INTEGER)
        );
        analyzerMockControl.setReturnValue(Type.INTEGER);
        analyzerMockControl.replay();
        checkType(Type.INTEGER, "sum(100/65, 23*45)");
        analyzerMockControl.verify();
    }
    
    public void testPlainInvocationWithVariableParams() {
        Type math = new Type("MATH");
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("x"));
        analyzerMockControl.setReturnValue(Type.INTEGER);
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("y"));
        analyzerMockControl.setReturnValue(Type.REAL);
        analyzerMock.resolveMethodType(
                null,
                TestCaseUtil.id("sumTwoReals"),
                Arrays.asList(Type.REAL, Type.REAL)
        );
        analyzerMockControl.setReturnValue(Type.REAL);
        analyzerMockControl.replay();
        checkType(Type.REAL, "sumTwoReals(x * 1.12, y)");
        analyzerMockControl.verify();
    }
    
    public void testFieldAccessAndInvocation() {
        Type arbitrary = new Type("WHATEVER");
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("a"));
        analyzerMockControl.setReturnValue(arbitrary);
        analyzerMock.resolveVariableType(arbitrary, TestCaseUtil.id("b"));
        analyzerMockControl.setReturnValue(arbitrary);
        analyzerMock.resolveMethodType(
                arbitrary,
                TestCaseUtil.id("c"),
                EMPTY_TYPES
        );
        analyzerMockControl.setReturnValue(Type.BOOLEAN);
        analyzerMockControl.replay();
        checkType(Type.BOOLEAN, "a.b.c()");
        analyzerMockControl.verify();
    }

    public void testNonExistentMethod() {
        analyzerMock.resolveMethodType(
                null,
                TestCaseUtil.id("getValue"),
                EMPTY_TYPES
        );
        analyzerMockControl.setReturnValue(null);
        analyzerMockControl.replay();
        checkType(null, "getValue()");
        analyzerMockControl.verify();
    }
    
    public void testMethodWithInvalidParams() {
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("b"));
        analyzerMockControl.setReturnValue(Type.INTEGER);
        analyzerMock.resolveVariableType(null, TestCaseUtil.id("c"));
        analyzerMockControl.setReturnValue(null);
        analyzerMock.resolveMethodType(
                null,
                TestCaseUtil.id("a"),
                Arrays.asList(Type.INTEGER, null)
        );
        analyzerMockControl.setReturnValue(null);
        analyzerMockControl.replay();
        checkType(null, "a(b, c)");
        analyzerMockControl.verify();
    }
    
    public void testVoidReturnType() {
        analyzerMock.resolveMethodType(
                null,
                TestCaseUtil.id("voidMethod"),
                EMPTY_TYPES
        );
        analyzerMockControl.setReturnValue(Type.VOID, 3);
        analyzerMock.addError("Operation '*' not defined for INTEGER, VOID", new Token(Value.MULTIPLY));
        analyzerMock.addError("Operation '-' not defined for VOID", new Token(Value.MINUS));
        analyzerMockControl.replay();
        checkType(Type.VOID, "voidMethod()");
        checkType(null, "3 * voidMethod()");
        checkType(null, "-voidMethod()");
        analyzerMockControl.verify();
    }
    
}
