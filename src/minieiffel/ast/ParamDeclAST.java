package minieiffel.ast;

import minieiffel.Token;
import minieiffel.semantics.Type;

/**
 * A parameter declaration.
 */
public class ParamDeclAST extends AbstractASTNode {

    /** name of the param and name of its type */
    private Token name, typeName;
    
    /** qualified type */
    private Type type;

    public ParamDeclAST(Token name, Token typeName) {
        this.name = name;
        this.typeName = typeName;
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
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public boolean equals(Object o) {
        if(o instanceof ParamDeclAST) {
            ParamDeclAST a = (ParamDeclAST)o;
            return a.name.equals(this.name) &&
                   a.typeName.equals(this.typeName);
        }
        return false;
    }
    
    public String toString() {
        return name.getText() + " : " + typeName.getText();
    }
    
}
