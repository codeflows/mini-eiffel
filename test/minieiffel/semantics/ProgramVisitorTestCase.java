package minieiffel.semantics;

import java.io.StringReader;
import java.util.Arrays;

import junit.framework.TestCase;
import minieiffel.Lexer;
import minieiffel.Parser;
import minieiffel.Source;
import minieiffel.TestCaseUtil;
import minieiffel.Token;
import minieiffel.Token.Value;
import minieiffel.ast.AssignmentAST;
import minieiffel.ast.ConditionalAST;
import minieiffel.ast.ConstructionAST;
import minieiffel.ast.IfStatementAST;
import minieiffel.ast.InstructionsAST;
import minieiffel.ast.IterationAST;
import minieiffel.ast.ProgramAST;
import minieiffel.ast.ProgramVisitor;
import minieiffel.ast.SimpleExpressionAST;
import minieiffel.ast.VariableDeclAST;

import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;

/**
 * Tests that the AST nodes starting from ProgramAST
 * call the ProgramVisitor's methods in the correct manner.
 */
public class ProgramVisitorTestCase extends TestCase {
    
    private ProgramVisitor visitorMock;
    private MockControl visitorMockControl;

    protected void setUp() {
        visitorMockControl = MockControl.createStrictControl(ProgramVisitor.class);
        visitorMock = (ProgramVisitor)visitorMockControl.getMock();
    }
    
    private void visit(String code) {
        Source source = new Source(new StringReader(code));
        Parser parser = new Parser(new Lexer(source));
        ProgramAST program = parser.handleProgram();
        program.accept(visitorMock);
    }
    
    public void testVisiting() {
        
        ArgumentsMatcher MATCH_ANY = new ArgumentsMatcher() {
            public boolean matches(Object[] arg0, Object[] arg1) {
                return true;
            }
            public String toString(Object[] arg0) {
                return "";
            }
        };
        
        // inner block for testing
        VariableDeclAST yDecl = new VariableDeclAST(TestCaseUtil.id("y"), TestCaseUtil.id("MY_TYPE"), null);
        ConstructionAST yConstr = new ConstructionAST(TestCaseUtil.id("y"));
        InstructionsAST innerBlock = new InstructionsAST(
                Arrays.asList(yDecl),
                yConstr
        );
        
        visitorMock.enteringClass(null);
        visitorMockControl.setMatcher(MATCH_ANY);
        
        visitorMock.enteringMethod(null);
        visitorMockControl.setMatcher(MATCH_ANY);
        
        visitorMock.visit(new VariableDeclAST(TestCaseUtil.id("a"), TestCaseUtil.id("INTEGER"), null));
        
        visitorMock.enteringBlock();
        visitorMock.visit(new VariableDeclAST(TestCaseUtil.id("b"), TestCaseUtil.id("INTEGER"), null));
        visitorMock.visit(new AssignmentAST(TestCaseUtil.id("a"), new SimpleExpressionAST(TestCaseUtil.id("b"))));

        IfStatementAST ifStmt = new IfStatementAST(
                new SimpleExpressionAST(
                        new Token(Value.TRUE)
                ),
                innerBlock
        );
        visitorMock.visit(
                new ConditionalAST(
                        ifStmt,
                        Arrays.asList(ifStmt),
                        innerBlock
                )
        );
        // inner block inside "if x then ... end"
        visitorMock.enteringBlock();
        visitorMock.visit(yDecl);
        visitorMock.visit(yConstr);
        visitorMock.leavingBlock();
        // elseif
        visitorMock.enteringBlock();
        visitorMock.visit(yDecl);
        visitorMock.visit(yConstr);
        visitorMock.leavingBlock();
        // else
        visitorMock.enteringBlock();
        visitorMock.visit(yDecl);
        visitorMock.visit(yConstr);
        visitorMock.leavingBlock();
        
        visitorMock.visit(new ConstructionAST(TestCaseUtil.id("d")));
        visitorMock.visit(
                new IterationAST(
                        innerBlock,
                        new SimpleExpressionAST(TestCaseUtil.id("end_condition")),
                        innerBlock
                )
        );
        // from ... end
        visitorMock.enteringBlock();
        visitorMock.visit(yDecl);
        visitorMock.visit(yConstr);
        visitorMock.leavingBlock();
        // loop ... end
        visitorMock.enteringBlock();
        visitorMock.visit(yDecl);
        visitorMock.visit(yConstr);
        visitorMock.leavingBlock();
        
        visitorMock.leavingBlock();
        
        visitorMock.leavingMethod(); // end of method m
        
        visitorMock.leavingClass(); // end of class A
        
        // all commands are given
        visitorMockControl.replay();
        visit(
                "class A\n" +
                " feature\n" +
                "  m(z:ZED) is\n" +
                "   local\n" +
                "    a:INTEGER\n" +
                "   do\n" +
                "    local\n" +
                "     b:INTEGER\n" +
                "    a := b\n" +
                "    if true then\n" +
                "      do\n" +
                "       local\n" +
                "        y:MY_TYPE" +
                "       !! y\n" +
                "      end\n" +
                "     elseif true then\n" +
                "      do\n" +
                "       local\n" +
                "        y:MY_TYPE" +
                "       !! y\n" +
                "      end\n" +
                "     else\n" +
                "      do\n" +
                "       local\n" +
                "        y:MY_TYPE" +
                "       !! y\n" +
                "      end\n" +
                "    end\n" +
                "    !! d\n" +
                "    from\n" +
                "     do\n" +
                "      local\n" +
                "       y:MY_TYPE" +
                "      !! y\n" +
                "     end\n" +
                "    until end_condition\n" +
                "    loop\n" +
                "     do\n" +
                "      local\n" +
                "       y:MY_TYPE" +
                "      !! y\n" +
                "     end\n" +
                "    end\n" + 
                "   end\n" +
                "end"
        );
        visitorMockControl.verify();
    }

}
