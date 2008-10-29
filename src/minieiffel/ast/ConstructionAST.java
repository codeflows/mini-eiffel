package minieiffel.ast;

import minieiffel.Token;
import minieiffel.semantics.Type;

/**
 * Represents the construction of an object. For example:
 * <code>!! myObj</code>.
 */
public class ConstructionAST extends AbstractASTNode implements InstructionAST {

    private Token identifier;
    private Type type;
    
    public ConstructionAST(Token identifier) {
        this.identifier = identifier;
    }
    
    public Token getLocationToken() {
        return identifier;
    }
    
    public Token getIdentifier() {
        return identifier;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
    
    public void accept(ProgramVisitor v) {
        v.visit(this);
    }
    
    public String toString() {
        return "!! " + identifier.getText();
    }
    
    public boolean equals(Object o) {
        return o instanceof ConstructionAST &&
               ((ConstructionAST)o).identifier.equals(this.identifier);
    }

}
