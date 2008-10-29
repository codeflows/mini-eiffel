package minieiffel.ast;

import java.util.Arrays;
import java.util.List;

import minieiffel.Token;

/**
 * An instructions block consisting of local variable declaration
 * and a sequence of {@link minieiffel.ast.InstructionAST instructions}.
 */
public class InstructionsAST extends AbstractASTNode {

    private List<VariableDeclAST> localDecls;
    private List<InstructionAST> instructions;
    
    public InstructionsAST(List<VariableDeclAST> localDecls, List<InstructionAST> instructions) {
        this.localDecls = localDecls;
        this.instructions = instructions;
    }
    
    public InstructionsAST(List<VariableDeclAST> localDecls, InstructionAST... instructions) {
        this.localDecls = localDecls;
        this.instructions = Arrays.asList(instructions);
    }
    
    public Token getLocationToken() {
        return null;
    }
    
    public List<VariableDeclAST> getLocalDecls() {
        return localDecls;
    }
    
    public List<InstructionAST> getInstructions() {
        return instructions;
    }
    
    public void accept(ProgramVisitor v) {
        v.enteringBlock();
        for (VariableDeclAST var : localDecls) {
            v.visit(var);
        }
        for (InstructionAST instruction : instructions) {
            instruction.accept(v);
        }
        v.leavingBlock();
    }
    
    public String toString() {
        return "do local " + localDecls + " " + instructions + " end";
    }

    public boolean equals(Object obj) {
        if (obj instanceof InstructionsAST) {
            InstructionsAST i = (InstructionsAST) obj;
            return this.localDecls.equals(i.localDecls) &&
                   this.instructions.equals(i.instructions);
        }
        return false;
    }

}
