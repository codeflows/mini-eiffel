package minieiffel.ast;

import minieiffel.Token;

/**
 * An Abstract Syntax Tree node.
 */
public interface ASTNode {
    
    /**
     * Returns a token that can be used to pinpoint the physical
     * location of this node in the source file.
     */
    Token getLocationToken();

}
