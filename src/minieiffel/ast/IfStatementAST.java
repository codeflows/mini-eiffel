package minieiffel.ast;

import minieiffel.Token;

/**
 * An <code>if</code> -statement
 * (part of a {@link minieiffel.ast.ConditionalAST}).
 */
public class IfStatementAST extends AbstractASTNode {
    
    private ExpressionAST guard;
    private InstructionsAST then;
    
    public IfStatementAST(ExpressionAST guard, InstructionsAST then) {
        this.guard = guard;
        this.then = then;
    }
    
    public Token getLocationToken() {
        return guard.getLocationToken();
    }
    
    public ExpressionAST getGuard() {
        return guard;
    }
    
    public InstructionsAST getThen() {
        return then;
    }
    
    public void accept(ProgramVisitor v) {
        if(then != null) {
            then.accept(v);
        }
    }

    public String toString() {
        return "if " + guard + " then " + then;
    }
    
    public boolean equals(Object o) {
        if(o instanceof IfStatementAST) {
            IfStatementAST i = (IfStatementAST)o;
            return this.guard.equals(i.guard) &&
                   (this.then == null ? i.then == null : this.then.equals(i.then));
        }
        return false;
    }

}
