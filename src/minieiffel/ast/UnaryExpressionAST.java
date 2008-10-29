package minieiffel.ast;

import minieiffel.Token;

/**
 * Unary expression, that is, one that's prefixed with an operator.
 */
public class UnaryExpressionAST extends ExpressionAST {

    private Token operator;
    private ExpressionAST expression;

    public UnaryExpressionAST(Token operator, ExpressionAST expression) {
        this.operator = operator;
        this.expression = expression;
    }
    
    public Token getLocationToken() {
        return operator;
    }
    
    public Token getOperator() {
        return operator;
    }
    
    public ExpressionAST getExpression() {
        return expression;
    }
    
    public void accept(ExpressionVisitor v) {
        v.visit(this);
    }

    public boolean equals(Object obj) {
        if(obj instanceof UnaryExpressionAST) {
            UnaryExpressionAST u = (UnaryExpressionAST)obj;
            return this.operator.equals(u.operator) &&
                   this.expression.equals(u.expression);
        }
        return false;
    }

    public String toString() {
        return "(" + operator.getValue() + " " + expression + ")";
    }
    
}
