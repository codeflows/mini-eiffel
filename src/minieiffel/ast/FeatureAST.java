package minieiffel.ast;

import java.util.List;

import minieiffel.semantics.Type;

/**
 * Interface to be implemented by AST nodes that
 * can be contained in a {@link minieiffel.ast.FeatureBlockAST feature}
 * (namely, methods and variable declarations).
 */
public interface FeatureAST extends ASTNode {

    /**
     * Accepts the given visitor.
     */
    void accept(FeatureVisitor visitor);
    
    /**
     * Returns a list of types this feature is visible to.
     */
    List<Type> getVisibility();
    
}
