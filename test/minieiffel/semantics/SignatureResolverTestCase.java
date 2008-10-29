package minieiffel.semantics;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import minieiffel.Lexer;
import minieiffel.Parser;
import minieiffel.Source;
import minieiffel.TestCaseUtil;
import minieiffel.Token;
import minieiffel.Token.TokenType;
import minieiffel.ast.MethodAST;
import minieiffel.ast.ParamDeclAST;
import minieiffel.ast.VariableDeclAST;

import org.easymock.MockControl;

public class SignatureResolverTestCase extends TestCase {
    
    private static final List<VariableDeclAST> EMPTY_VARS = Collections.emptyList();
    private static final List<MethodAST> EMPTY_METHODS = Collections.emptyList();

    private SignatureResolver resolver;
    private List<Signature> signatures;
    
    private SemanticAnalyzer analyzerMock;
    private MockControl analyzerMockControl;

    protected void setUp() {
        analyzerMockControl = MockControl.createStrictControl(SemanticAnalyzer.class);
        analyzerMock = (SemanticAnalyzer)analyzerMockControl.getMock();
    }
    
    private void resolve(String code) {
        Source source = new Source(new StringReader(code));
        Parser parser = new Parser(new Lexer(source));
        resolver = new SignatureResolver(analyzerMock, parser.handleProgram());
        signatures = resolver.resolveSignatures();
    }
    
    public void testEmptyClass() {
        analyzerMockControl.replay();
        resolve("class Empty\nend");
        analyzerMockControl.verify();
        Signature sig = signatures.get(0);
        assertEquals(new Type("Empty"), sig.getClassAST().getType());
        assertEquals(EMPTY_VARS, sig.getVariables());
        assertEquals(EMPTY_METHODS, sig.getMethods());
    }
    
    public void testEmptyFeature() {
        analyzerMockControl.replay();
        resolve("class EmptyFeature\nfeature\nend");
        analyzerMockControl.verify();
        Signature sig = signatures.get(0);
        assertEquals(new Type("EmptyFeature"), sig.getClassAST().getType());
        assertEquals(EMPTY_VARS, sig.getVariables());
        assertEquals(EMPTY_METHODS, sig.getMethods());
    }
    
    public void testDefaultVisibility() {
        analyzerMockControl.replay();
        resolve(
                "class Test\n" +
                "feature\n" +
                "  a : INTEGER\n" +
                "  x : BOOLEAN is\n" +
                "end"
        );
        analyzerMockControl.verify();
        
        assertEquals(1, signatures.size());
        Signature sig = signatures.get(0);
        assertEquals(new Type("Test"), sig.getClassAST().getType());
        
        assertEquals(1, sig.getVariables().size());
        VariableDeclAST a = sig.getVariables().get(0);
        assertEquals(Type.INTEGER, a.getType());
        assertEquals(SignatureResolver.DEFAULT_VISIBILITY, a.getVisibility());
        
        assertEquals(1, sig.getMethods().size());
        MethodAST x = sig.getMethods().get(0);
        assertEquals(Type.BOOLEAN, x.getReturnType());
        assertEquals(SignatureResolver.DEFAULT_VISIBILITY, x.getVisibility());
    }
    
    public void testEmptyVisibility() {
        analyzerMockControl.replay();
        resolve(
                "class EmptyVisibilityTest\n" +
                "feature {}\n" +
                "  h : CHARACTER\n" +
                "end"
        );
        analyzerMockControl.verify();
        
        assertEquals(1, signatures.size());
        Signature sig = signatures.get(0);
        assertEquals(new Type("EmptyVisibilityTest"), sig.getClassAST().getType());
        
        assertEquals(1, sig.getVariables().size());
        VariableDeclAST h = sig.getVariables().get(0);
        assertEquals(Type.CHARACTER, h.getType());
        assertEquals(SignatureResolver.EMPTY_VISIBILITY, h.getVisibility());
    }
    
    public void testCustomVisibility() {
        analyzerMockControl.replay();
        resolve(
                "class VisibilityTest\n" +
                "feature {VisibilityTest, INTEGER}\n" +
                "  z : REAL\n" +
                "  q : VisibilityTest is\n" +
                "feature {REAL}\n" +
                "  p : CHARACTER is\n" +
                "end"
        );
        analyzerMockControl.verify();
        
        Type self = new Type("VisibilityTest");
        
        assertEquals(1, signatures.size());
        Signature sig = signatures.get(0);
        assertEquals(self, sig.getClassAST().getType());
        
        assertEquals(1, sig.getVariables().size());
        VariableDeclAST z = sig.getVariables().get(0);
        assertEquals(Type.REAL, z.getType());
        TestCaseUtil.assertListContents(z.getVisibility(), self, Type.INTEGER);
        
        assertEquals(2, sig.getMethods().size());
        
        MethodAST q = sig.getMethods().get(0);
        assertEquals(self, q.getReturnType());
        TestCaseUtil.assertListContents(q.getVisibility(), self, Type.INTEGER);
        
        MethodAST p = sig.getMethods().get(1);
        assertEquals(Type.CHARACTER, p.getReturnType());
        TestCaseUtil.assertListContents(p.getVisibility(), Type.REAL);
    }
    
    public void testMethodParameterTypes() {
        analyzerMockControl.replay();
        resolve(
                "class MethodParams\n" +
                "feature {}\n" +
                "  myPrivateMethod(a:INTEGER; b:REAL) is\n" +
                "end"
        );
        analyzerMockControl.verify();
        
        Signature sig = signatures.get(0);
        MethodAST myPrivateMethod = sig.getMethods().get(0);
        assertEquals(Type.VOID, myPrivateMethod.getReturnType());
        TestCaseUtil.assertListContents(myPrivateMethod.getVisibility(), Type.NONE);
        
        assertEquals(2, myPrivateMethod.getParamDecls().size());
        ParamDeclAST a = myPrivateMethod.getParamDecls().get(0);
        assertEquals(Type.INTEGER, a.getType());
        ParamDeclAST b = myPrivateMethod.getParamDecls().get(1);
        assertEquals(Type.REAL, b.getType());
    }
    
    public void testVoidNotAllowedAsParamOrVisibilityType() {
        Token voidToken = new Token(TokenType.IDENTIFIER, "VOID");
        analyzerMock.addError("\"VOID\" is a special type that can't be referenced in a source file", voidToken);
        analyzerMockControl.setVoidCallable(4);
        analyzerMockControl.replay();
        resolve(
                "class VoidInvalidityTest\n" +
                "feature {VOID}\n" +
                "  a:VOID\n" +
                "  b(c:VOID)is\n" +
                "  d:VOID is\n" +
                "end"
        );
        analyzerMockControl.verify();
    }
    
    public void testCrossReferencing() {
        analyzerMockControl.replay();
        resolve(
                "class A\n" +
                "feature {B}\n" +
                " aVar:B\n" +
                " aMethod(x:B):B is\n" +
                "end\n" +
                "class B\n" +
                "feature {A}\n" +
                " bVar:A\n" +
                " bMethod(y:A):A is\n" +
                "end\n"
        );
        analyzerMockControl.verify();

        Type typeA = new Type("A");
        Type typeB = new Type("B");
        
        assertEquals(2, signatures.size());
        
        Signature A = signatures.get(0);
        assertEquals(typeA, A.getClassAST().getType());

        assertEquals(1, A.getVariables().size());
        VariableDeclAST aVar = A.getVariables().get(0);
        assertEquals(typeB, aVar.getType());
        TestCaseUtil.assertListContents(aVar.getVisibility(), typeB);
        
        assertEquals(1, A.getMethods().size());
        MethodAST aMethod = A.getMethods().get(0);
        assertEquals(typeB, aMethod.getReturnType());
        TestCaseUtil.assertListContents(aMethod.getVisibility(), typeB);
        assertEquals(typeB, aMethod.getParamDecls().get(0).getType());
        
        Signature B = signatures.get(1);
        assertEquals(typeB, B.getClassAST().getType());

        assertEquals(1, B.getVariables().size());
        VariableDeclAST bVar = B.getVariables().get(0);
        assertEquals(typeA, bVar.getType());
        TestCaseUtil.assertListContents(bVar.getVisibility(), typeA);
        
        assertEquals(1, B.getMethods().size());
        MethodAST bMethod = B.getMethods().get(0);
        assertEquals(typeA, bMethod.getReturnType());
        TestCaseUtil.assertListContents(bMethod.getVisibility(), typeA);
        assertEquals(typeA, bMethod.getParamDecls().get(0).getType());
    }
    
    public void testRedefiningBuiltinClasses() {
        analyzerMock.addError("Can't redefine built-in type \"INTEGER\"", new Token(TokenType.IDENTIFIER, "INTEGER"));
        analyzerMockControl.replay();
        resolve("class INTEGER\nend\n");
        analyzerMockControl.verify();
        assertEquals(0, signatures.size());
    }
    
    public void testDefiningClassTwice() {
        analyzerMock.addError("Class \"A\" already defined", new Token(TokenType.IDENTIFIER, "A"));
        analyzerMockControl.replay();
        resolve(
                "class A\n" +
                "end\n" +
                "class A\n" +
                "feature\n" +
                " b:INTEGER\n" +
                "end"
        );
        analyzerMockControl.verify();
        assertEquals(1, signatures.size());
        assertTrue(signatures.get(0).getVariables().isEmpty());
    }
    
    public void testReferencingNonExistingClasses() {
        Token B = new Token(TokenType.IDENTIFIER, "B");
        analyzerMock.addError("Can't find class \"B\"", B);
        analyzerMockControl.setVoidCallable(4);
        analyzerMockControl.replay();
        resolve(
                "class A\n" +
                "feature {B}\n" +
                " bVar:B\n" +
                " bMethod(bParam:B) : B is\n" +
                "end"
        );
        analyzerMockControl.verify();
    }
    
    public void testConstantValues() {
        Token PI = new Token(TokenType.REAL_LITERAL, "3.14");
        analyzerMock.addError("\"3.14\" is an invalid constant value for type INTEGER", PI);
        analyzerMockControl.replay();
        resolve(
                "class ConstantValueTest\n" +
                "feature\n" +
                "  a:INTEGER is 3.14\n" +
                "  b:INTEGER is 3\n" +
                "end"
        );
        analyzerMockControl.verify();
        assertEquals(1, signatures.size());
        Signature sig = signatures.get(0);
        assertEquals(1, sig.getVariables().size());
        VariableDeclAST b = sig.getVariables().get(0);
        assertEquals(Type.INTEGER, b.getType());
    }
    
}
