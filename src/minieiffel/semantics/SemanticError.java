package minieiffel.semantics;

import minieiffel.Token;

/**
 * A class representing an error in the semantic structure of a program.
 * I.e. this class is not an exception, but more of an error message.
 */
public class SemanticError {
    
    /** explanation of the error */
    private String message;
    
    /** the token at the error position */
    private Token offendingToken;

    public SemanticError(String message, Token token) {
        this.message = message;
        this.offendingToken = token;
    }

    public String getMessage() {
        return message;
    }
    
    public Token getOffendingToken() {
        return offendingToken;
    }
    
    public String toString() {
        return "SemanticError { " + message + ", " + offendingToken + "}";
    }
    
    public boolean equals(Object o) {
        if (o instanceof SemanticError) {
            SemanticError s = (SemanticError) o;
            return this.message.equals(s.message);
        }
        return false;
    }

}
