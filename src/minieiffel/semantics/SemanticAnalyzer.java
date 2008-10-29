package minieiffel.semantics;

import java.util.List;

import minieiffel.Token;
import minieiffel.ast.ProgramAST;

/**
 * Analyzes the semantics of a Mini-Eiffel program and reports
 * any errors found in the process. Maintains the symbol table
 * of a program and provides methods for other classes to
 * access the program's variables/methods (namely,
 * {@link #resolveMethodType(Type, Token, List)} and
 * {@link #resolveVariableType(Type, Token)}.
 * Analysis doesn't stop at the first error, the analyzer
 * tries to analyze the whole program exhaustively.
 */
public interface SemanticAnalyzer {

    /**
     * Analyzes the given program.
     */
    void analyze(ProgramAST program);
    
    /**
     * Returns any semantic errors that occured during the analysis.
     */
    List<SemanticError> getErrors();

    /**
     * Adds a semantic error to the list of errors.
     */
    void addError(String message, Token offendingToken);
    
    /**
     * Resolves the type of the given variable.
     * 
     * @param owner type containing the variable (null for current type)
     * @param name of the variable
     * @return resolved type or <code>null</code> if variable can't be resolved
     */
    Type resolveVariableType(Type owner, Token name);
    
    /**
     * Resolves the return type of the given method.
     * 
     * @param owner type containing the method (null for current type)
     * @param name of the method
     * @param argumentTypes types of the arguments
     * @return return type of the method or <code>null</code> if no such method
     */
    Type resolveMethodType(Type owner, Token name, List<Type> argumentTypes);

    /**
     * Prints out the symbol table for debug purposes.
     */
    void printSymbolTable();

}
