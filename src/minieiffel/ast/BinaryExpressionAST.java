package minieiffel.ast;

import minieiffel.Token;

/**
 * A binary expression, that is, one that takes two operands
 * and applies an operator on them. For example,
 * <code>a+b</code>.
 */
public class BinaryExpressionAST extends ExpressionAST {
    
    private ExpressionAST lhs;
    private Token operator;
    private ExpressionAST rhs;
    
    public BinaryExpressionAST(ExpressionAST lhs, Token operator, ExpressionAST rhs) {
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }
    
    public Token getLocationToken() {
        return operator;
    }

    /**
     * Left-hand side expression.
     */
    public ExpressionAST getLhs() {
        return lhs;
    }

    /**
     * The operator to be applied on the expressions.
     */
    public Token getOperator() {
        return operator;
    }

    /**
     * Right-hand side expression.
     */
    public ExpressionAST getRhs() {
        return rhs;
    }
    
    public void accept(ExpressionVisitor v) {
        v.visit(this);
    }
    
    public boolean equals(Object o) {
        if(o instanceof BinaryExpressionAST) {
            BinaryExpressionAST b = (BinaryExpressionAST)o;
            return this.lhs.equals(b.lhs) &&
                   this.operator.equals(b.operator) &&
                   this.rhs.equals(b.rhs);
        }
        return false;
    }
    
    public String toString() {
        return "(" + lhs + " " + operator.getText() + " " + rhs + ")";
    }
    
}
