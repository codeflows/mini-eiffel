package minieiffel.ast;

import minieiffel.semantics.Type;

/**
 * Abstract superclass for different expression types.
 */
public abstract class ExpressionAST
    extends AbstractASTNode implements InstructionAST {

    /** the resolved type of this expression */
    protected Type type;
    
    /**
     * Returns the type of this expression.
     */
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Accepts an {@link ExpressionVisitor}, i.e. instructs
     * the visitor to visit this expression.
     */
    public abstract void accept(ExpressionVisitor visitor);
    
    public void accept(ProgramVisitor v) {
        v.visit(this);
    }
    
}
