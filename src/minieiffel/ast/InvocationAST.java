package minieiffel.ast;

import java.util.Arrays;
import java.util.List;

import minieiffel.Token;

/**
 * Represents a method invocation and its parameters.
 */
public class InvocationAST extends ExpressionAST {

    private Token identifier;
    private List<ExpressionAST> arguments;

    public InvocationAST(Token identifier, ExpressionAST... arguments) {
        this.identifier = identifier;
        this.arguments = Arrays.asList(arguments);
    }
    
    public InvocationAST(Token identifier, List<ExpressionAST> arguments) {
        this.identifier = identifier;
        this.arguments = arguments;
    }
    
    public Token getLocationToken() {
        return identifier;
    }

    public Token getIdentifier() {
        return identifier;
    }
    
    public List<ExpressionAST> getArguments() {
        return arguments;
    }
    
    public void accept(ExpressionVisitor v) {
        v.visit(this);
    }

    public boolean equals(Object o) {
        if(o instanceof InvocationAST) {
            InvocationAST i = (InvocationAST)o;
            return this.identifier.equals(i.identifier) &&
                   this.arguments.equals(i.arguments);
        }
        return false;
    }
    
    public String toString() {
        return identifier.getText() + "(" +
               arguments.toString().replaceAll("^\\[(.*)\\]$", "$1")
               + ")";
    }
    
}
