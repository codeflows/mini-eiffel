package minieiffel.ast;

import minieiffel.Token;

/**
 * Represents a loop with a start condition, end condition and a body.
 */
public class IterationAST extends AbstractASTNode implements InstructionAST {

    private InstructionsAST from;
    private ExpressionAST until;
    private InstructionsAST loop;
    
    public IterationAST(InstructionsAST from, ExpressionAST until, InstructionsAST loop) {
        this.from = from;
        this.until = until;
        this.loop = loop;
    }
    
    public Token getLocationToken() {
        return null;
    }

    public InstructionsAST getFrom() {
        return from;
    }

    public InstructionsAST getLoop() {
        return loop;
    }

    public ExpressionAST getUntil() {
        return until;
    }
    
    public void accept(ProgramVisitor v) {
        v.visit(this);
        if(from != null) {
            from.accept(v);
        }
        if(loop != null) {
            loop.accept(v);
        }
    }
    
    public String toString() {
        return "from " + from + " until " + until + " loop " + loop + " end";
    }
    
    public boolean equals(Object o) {
        if (o instanceof IterationAST) {
            IterationAST i = (IterationAST) o;
            return (this.from == null ? i.from == null : this.from.equals(i.from)) &&
                    this.until.equals(i.until) &&
                   (this.loop == null ? i.loop == null : this.loop.equals(i.loop));
        }
        return false;
    }

}
