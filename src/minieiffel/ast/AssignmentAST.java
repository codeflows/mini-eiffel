package minieiffel.ast;

import minieiffel.Token;

/**
 * AST node for an assignment operation, for example:
 * <code>the_answer := 42</code>.
 */
public class AssignmentAST extends AbstractASTNode implements InstructionAST {

    private Token identifier;
    private ExpressionAST expression;
    
    public AssignmentAST(Token identifier, ExpressionAST expression) {
        this.identifier = identifier;
        this.expression = expression;
    }
    
    public Token getLocationToken() {
        return identifier;
    }

    /**
     * Returns the name of assignment target variable.
     */
    public Token getIdentifier() {
        return identifier;
    }

    /**
     * Returns the expression whose value is to be assigned.
     */
    public ExpressionAST getExpression() {
        return expression;
    }
    
    public void accept(ProgramVisitor v) {
        v.visit(this);
    }
    
    public String toString() {
        return identifier.getText() + " := " + expression;
    }

    public boolean equals(Object obj) {
        if(obj instanceof AssignmentAST) {
            AssignmentAST a = (AssignmentAST)obj;
            return this.identifier.equals(a.identifier) &&
                   this.expression.equals(a.expression);
        }
        return false;
    }

}
