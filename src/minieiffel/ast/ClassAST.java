package minieiffel.ast;

import java.util.List;

import minieiffel.Token;
import minieiffel.semantics.Signature;
import minieiffel.semantics.Type;

/**
 * Encloses the name and the
 * {@link minieiffel.ast.FeatureBlockAST features}
 * of a single class.
 */
public class ClassAST extends AbstractASTNode {

    /** name of this class */
    private Token name;
    
    /** list of features */
    private List<FeatureBlockAST> featureBlocks;
    
    /** qualified type */
    private Type type;
    
    /** the signature of this class */
    private Signature signature;
    
    public ClassAST(Token name, List<FeatureBlockAST> featureBlocks) {
        this.name = name;
        this.featureBlocks = featureBlocks;
    }
    
    public Token getLocationToken() {
        return name;
    }
    
    public Token getName() {
        return name;
    }
    
    public List<FeatureBlockAST> getFeatureBlocks() {
        return featureBlocks;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public Signature getSignature() {
        return signature;
    }
    
    public void setSignature(Signature signature) {
        this.signature = signature;
    }
    
    /**
     * Calls <code>FeatureVisitor.visit(...)</code> for each
     * feature block and feature in this class.
     */
    public void accept(FeatureVisitor v) {
        for (FeatureBlockAST block : featureBlocks) {
            v.visit(block);
            for (FeatureAST feature : block.getFeatures()) {
                feature.accept(v);
            }
        }
    }
    
    public String toString() {
        return "class " + name.getText() + " " + featureBlocks + " end";
    }
    
    public boolean equals(Object o) {
        if (o instanceof ClassAST) {
            ClassAST c = (ClassAST) o;
            return this.name.equals(c.name) &&
                   this.featureBlocks.equals(c.featureBlocks);
        }
        return false;
    }

}
