package minieiffel.ast;

/**
 * Marker interface for AST nodes that are instructions.
 */
public interface InstructionAST extends ASTNode {

    /**
     * Accepts a method visitor (i.e. calls the visitor method
     * corresponding to this instruction on it).
     */
    void accept(ProgramVisitor v);

}
