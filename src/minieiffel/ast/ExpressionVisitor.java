package minieiffel.ast;

/**
 * Visitor interface for all the different
 * {@link minieiffel.ast.ExpressionAST expression types}.
 */
public interface ExpressionVisitor {

    void visit(SimpleExpressionAST expr);
    void visit(UnaryExpressionAST expr);
    void visit(BinaryExpressionAST expr);
    void visit(InvocationAST expr);
    
}
