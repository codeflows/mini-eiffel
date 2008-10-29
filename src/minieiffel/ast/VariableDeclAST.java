package minieiffel.ast;

import java.util.List;

import minieiffel.Token;
import minieiffel.semantics.Type;

/**
 * A variable declaration.
 */
public class VariableDeclAST extends AbstractASTNode implements FeatureAST {
    
    /** name of the variable, its type and any constant value it might have */
    private Token name, typeName, constantValue;
    
    /** qualified type */
    private Type type;
    
    /** types for whom this variable is visible
     *  (only applicable for top-level variables) */
    private List<Type> visibility;

    public VariableDeclAST(Token identifier, Token type, Token value) {
        this.name = identifier;
        this.typeName = type;
        this.constantValue = value;
    }
    
    public Token getLocationToken() {
        return name;
    }

    public Token getName() {
        return name;
    }
    
    public Token getTypeName() {
        return typeName;
    }
    
    public Token getConstantValue() {
        return constantValue;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
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
    
    public boolean equals(Object o) {
        if(o instanceof VariableDeclAST) {
            VariableDeclAST a = (VariableDeclAST)o;
            return a.name.equals(this.name) &&
                   a.typeName.equals(this.typeName) &&
                   (a.constantValue == null ? this.constantValue == null : a.constantValue.equals(this.constantValue));
        }
        return false;
    }
    
    public String toString() {
        return name.getText() + " : "
            + typeName.getText()
            + (constantValue == null ? "" : " is " + constantValue.getText());
    }

}
