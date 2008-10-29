package minieiffel.ast;

/**
 * Visitor for the features of a program: feature blocks and the
 * variable declarations and methods inside them.
 */
public interface FeatureVisitor {
    
    void visit(FeatureBlockAST block);
    void visit(VariableDeclAST variable);
    void visit(MethodAST method);

}
