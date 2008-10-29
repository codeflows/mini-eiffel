package minieiffel.ast;

import java.util.Collections;
import java.util.List;

import minieiffel.Token;

/**
 * AST for a conditional statement (if, elseif, else).
 */
public class ConditionalAST extends AbstractASTNode implements InstructionAST {

    private IfStatementAST ifStatement;
    private List<IfStatementAST> elseIfStatements = Collections.emptyList();
    private InstructionsAST elseStatement;
    
    public ConditionalAST(IfStatementAST ifStatement, List<IfStatementAST> elseIfStatements, InstructionsAST elseStatement) {
        this.ifStatement = ifStatement;
        this.elseIfStatements = elseIfStatements;
        this.elseStatement = elseStatement;
    }
    
    public Token getLocationToken() {
        return ifStatement.getLocationToken();
    }
    
    public IfStatementAST getIfStatement() {
        return ifStatement;
    }
    
    public List<IfStatementAST> getElseIfStatements() {
        return elseIfStatements;
    }
    
    public InstructionsAST getElseStatement() {
        return elseStatement;
    }
    
    public void accept(ProgramVisitor v) {
        v.visit(this);
        ifStatement.accept(v);
        for (IfStatementAST elseIfStmt : elseIfStatements) {
            elseIfStmt.accept(v);
        }
        if(elseStatement != null) {
            elseStatement.accept(v);
        }
    }
    
    public String toString() {
        return ifStatement + ", " + elseIfStatements + ", " + elseStatement;
    }
    
    public boolean equals(Object o) {
        if(o instanceof ConditionalAST) {
            ConditionalAST c = (ConditionalAST)o;
            return
                this.ifStatement.equals(c.ifStatement) &&
                this.elseIfStatements.equals(c.elseIfStatements) &&
                (this.elseStatement == null ?
                        c.elseStatement == null :
                        this.elseStatement.equals(c.elseStatement));
        }
        return false;
    }

}
