package minieiffel.ast;

import java.util.List;

/**
 * Just a fancy wrapper for the class list that's the
 * actual beef of a program.
 */
public class ProgramAST {
    
    private List<ClassAST> classes;

    public ProgramAST(List<ClassAST> classes) {
        this.classes = classes;
    }
    
    public List<ClassAST> getClasses() {
        return classes;
    }
    
    /**
     * Takes the given visitor thru this program.
     */
    public void accept(ProgramVisitor v) {
        for (ClassAST klass : classes) {
            v.enteringClass(klass);
            for (FeatureBlockAST block : klass.getFeatureBlocks()) {
                for (FeatureAST feature : block.getFeatures()) {
                    if(feature instanceof MethodAST) {
                        ((MethodAST)feature).accept(v);
                    }
                }
            }
            v.leavingClass();
        }
    }
    
    public String toString() {
        return classes.toString();
    }
    
    public boolean equals(Object o) {
        return o instanceof ProgramAST &&
               ((ProgramAST)o).classes.equals(this.classes);
    }

}
