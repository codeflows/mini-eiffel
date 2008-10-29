package minieiffel.semantics;

import java.util.List;

import minieiffel.ast.ClassAST;
import minieiffel.ast.MethodAST;
import minieiffel.ast.VariableDeclAST;

/**
 * The signature of a class as seen by the semantics module
 * (a flattened hierarchy of its variables and methods).
 */
public class Signature {
    
    private ClassAST classAST;
    private List<VariableDeclAST> variables;
    private List<MethodAST> methods;

    public Signature(ClassAST klass, List<VariableDeclAST> variables, List<MethodAST> methods) {
        this.classAST = klass;
        this.variables = variables;
        this.methods = methods;
    }
    
    /**
     * The class whose signature this is.
     */
    public ClassAST getClassAST() {
        return classAST;
    }
    
    /**
     * List of variable declarations.
     */
    public List<VariableDeclAST> getVariables() {
        return variables;
    }
    
    /**
     * List of methods.
     */
    public List<MethodAST> getMethods() {
        return methods;
    }

    public String toString() {
        return "Signature { class=" + classAST + ", variables=" + variables + ", methods=" + methods + " }";
    }

}
