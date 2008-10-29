package minieiffel.ast;

import minieiffel.Token;

/**
 * A simple expression, either a literal or an identifier.
 */
public class SimpleExpressionAST extends ExpressionAST {
    
    private Token token;

    public SimpleExpressionAST(Token token) {
        this.token = token;
    }

    public Token getLocationToken() {
        return token;
    }
    
    public void accept(ExpressionVisitor v) {
        v.visit(this);
    }
    
    public boolean equals(Object o) {
        return o instanceof SimpleExpressionAST &&
                ((SimpleExpressionAST)o).token.equals(this.token);
    }
    
    public String toString() {
        return token.getText();
    }

}
