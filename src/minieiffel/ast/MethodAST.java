package minieiffel.ast;

import java.util.List;

import minieiffel.Token;
import minieiffel.semantics.Type;

/**
 * The definition and the actual code of a method.
 */
public class MethodAST extends AbstractASTNode implements FeatureAST {

    /** name of the method */
    private Token name;
    
    /** list of param declarations */
    private List<ParamDeclAST> paramDecls;
    
    /** name of the return type */
    private Token returnTypeName;
    
    /** local variable declarations */
    private List<VariableDeclAST> localVariableDecls;
    
    /** instructions (may be null) */
    private InstructionsAST instructions;
    
    /** qualified return type */
    private Type returnType;
    
    /** list of types this method is visible to */
    private List<Type> visibility;
    
    public MethodAST(Token name, List<ParamDeclAST> paramDecls, Token returnType, List<VariableDeclAST> localVariableDecls, InstructionsAST instructions) {
        this.name = name;
        this.paramDecls = paramDecls;
        this.returnTypeName = returnType;
        this.localVariableDecls = localVariableDecls;
        this.instructions = instructions;
    }
    
    public Token getLocationToken() {
        return name;
    }

    public InstructionsAST getInstructions() {
        return instructions;
    }

    public List<VariableDeclAST> getLocalVariableDecls() {
        return localVariableDecls;
    }

    public Token getName() {
        return name;
    }

    public List<ParamDeclAST> getParamDecls() {
        return paramDecls;
    }

    public Token getReturnTypeName() {
        return returnTypeName;
    }
    
    public Type getReturnType() {
        return returnType;
    }
    
    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }
    
    public List<Type> getVisibility() {
        return visibility;
    }
    
    public void setVisibility(List<Type> visibility) {
        this.visibility = visibility;
    }
    
    public void accept(FeatureVisitor v) {
        v.visit(this);
    }
    
    public void accept(ProgramVisitor v) {
        v.enteringMethod(this);
        for (VariableDeclAST var : localVariableDecls) {
            v.visit(var);
        }
        if(instructions != null) {
            instructions.accept(v);
        }
        v.leavingMethod();
    }
    
    public boolean equals(Object o) {
        if(o instanceof MethodAST) {
            MethodAST m = (MethodAST)o;
            return
                this.name.equals(m.name) &&
                this.paramDecls.equals(m.paramDecls) &&
                (this.returnTypeName == null ?
                        m.returnTypeName == null :
                        this.returnTypeName.equals(m.returnTypeName)) &&
                this.localVariableDecls.equals(m.localVariableDecls) &&
                (this.instructions == null ?
                        m.instructions == null :
                        this.instructions.equals(m.instructions));
        }
        return false;
    }
    
    public String toString() {
        return name + "(" + paramDecls + ") : " + returnTypeName
            + " is " + localVariableDecls + " " + instructions;
    }

}
