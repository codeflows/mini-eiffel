package minieiffel.ast;

/**
 * A visitor for the actual code of a program, that is,
 * the classes, methods and their contents.
 */
public interface ProgramVisitor {

    /**
     * Called when entering a class.
     */
    void enteringClass(ClassAST klass);
    void leavingClass();

    /**
     * Called when visiting a method.
     */
    void enteringMethod(MethodAST method);
    void leavingMethod();
    
    /**
     * Called when entering an inner scope in the program.
     */
    void enteringBlock();
    
    /**
     * Called when leaving a scope, can be used to pop
     * variable definitions belonging in the scope off
     * the local variable stack, for example.
     */
    void leavingBlock();
    
    void visit(VariableDeclAST var);
    void visit(ExpressionAST expr);
    void visit(AssignmentAST assignment);
    void visit(ConditionalAST conditional);
    void visit(ConstructionAST construction);
    void visit(IterationAST iteration);
    
}
