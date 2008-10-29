package minieiffel.ast;

import java.util.List;

import minieiffel.Token;

/**
 * Wraps a block of features and their visibility parameters.
 */
public class FeatureBlockAST extends AbstractASTNode {

    private List<Token> visibility;
    private List<FeatureAST> features;
    
    public FeatureBlockAST(List<Token> visibility, List<FeatureAST> features) {
        this.visibility = visibility;
        this.features = features;
    }
    
    public Token getLocationToken() {
        return null;
    }

    public List<Token> getVisibility() {
        return visibility;
    }
    
    public List<FeatureAST> getFeatures() {
        return features;
    }
    
    public String toString() {
        return "feature {" + visibility + "} " + features;
    }
    
    public boolean equals(Object o) {
        if(o instanceof FeatureBlockAST) {
            FeatureBlockAST f = (FeatureBlockAST)o;
            return (this.visibility == null ?
                        f.visibility == null :
                        this.visibility.equals(f.visibility)) &&
                   this.features.equals(f.features);
        }
        return false;
    }
    
}
