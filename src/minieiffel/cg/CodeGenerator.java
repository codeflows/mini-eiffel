package minieiffel.cg;

import java.util.Map;

import minieiffel.ast.ProgramAST;
import minieiffel.semantics.Type;

/**
 * Generates Java bytecode out of a Mini-Eiffel program.
 */
public interface CodeGenerator {

    /**
     * Generates bytecode for the classes found in the given program.
     * Returns a map of the raw bytecode for each class mapped by
     * its unique Type object.
     */
    Map<Type, byte[]> generateClasses(ProgramAST program);
    
}
