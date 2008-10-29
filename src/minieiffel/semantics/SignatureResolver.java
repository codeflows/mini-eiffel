package minieiffel.semantics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import minieiffel.Token;
import minieiffel.ast.ClassAST;
import minieiffel.ast.FeatureBlockAST;
import minieiffel.ast.FeatureVisitor;
import minieiffel.ast.MethodAST;
import minieiffel.ast.ParamDeclAST;
import minieiffel.ast.ProgramAST;
import minieiffel.ast.VariableDeclAST;

/**
 * Resolves the {@link Type type} {@link Signature signatures}
 * of a program:
 * 
 * <ul>
 * <li>
 * Handles cross-references between classes,
 * i.e. if class <em>A</em> is processed first and it
 * references class <em>B</em>, the type <em>B</em> is
 * marked as <em>unresolved</em> until it's reached in
 * the program. If there are any unresolved references after
 * the program has been gone thru, they are reported as errors.
 * </li>
 * <li>
 * Ensures that built-in types are not redefined in a program
 * (i.e. that you can't define a class called <em>INTEGER</em>).
 * </li>
 * <li>
 * Ensures that no class is defined more than once in a program.
 * </li>
 * </ul>
 * 
 * <p>Implemented as a {@link minieiffel.ast.FeatureVisitor}.</p>
 */
public class SignatureResolver implements FeatureVisitor {
    
    /** default visibility (ANY) when none is specified */
    public static final List<Type> DEFAULT_VISIBILITY = Arrays.asList(Type.ANY);
    
    /** empty visibility (NONE) */
    public static final List<Type> EMPTY_VISIBILITY = Arrays.asList(Type.NONE);

    /** driver of the whole analysis */
    private SemanticAnalyzer analyzer;
    
    /** program under resolving */
    private ProgramAST program;

    /** current partial signature */
    private Signature currentSignature;
    
    /** visibility listing for current feature */
    private List<Type> currentVisibility;

    /** resolved types mapped by name */
    private Map<String, Type> resolvedTypes = new HashMap<String, Type>();
    
    /** list of unresolved type names (as the original tokens) */
    private List<Token> unresolvedTypeNames = new LinkedList<Token>();

    /**
     * Creates a signature resolver for the given program.
     */
    public SignatureResolver(SemanticAnalyzer analyzer, ProgramAST program) {
        this.analyzer = analyzer;
        this.program = program;
    }

    /**
     * Resolves the signatures of the program passed to the constructor
     * and returns them in a list.
     */
    public List<Signature> resolveSignatures() {
        List<Signature> signatures = new LinkedList<Signature>();
        // resolve signature for each class
        for (ClassAST classAST : program.getClasses()) {
            Signature sig = resolveSignature(classAST);
            if(sig != null) {
                signatures.add(sig);
                classAST.setSignature(sig);
            }
        }
        // if some types were left unresolved, report them
        if(!unresolvedTypeNames.isEmpty()) {
            for (Token unresolved : unresolvedTypeNames) {
                analyzer.addError(
                        "Can't find class \"" + unresolved.getText() + "\"",
                        unresolved
                );
            }
        }
        return signatures;
    }
    
    /**
     * Visits a feature block and handles its visibility definition.
     */
    public void visit(FeatureBlockAST block) {
        if(block.getVisibility() == null) {
            currentVisibility = DEFAULT_VISIBILITY;
        } else if(block.getVisibility().isEmpty()) {
            currentVisibility = EMPTY_VISIBILITY;
        } else {
            currentVisibility = new LinkedList<Type>();
            for (Token item : block.getVisibility()) {
                currentVisibility.add(typeForName(item));
            }
        }
    }

    /**
     * Visits a variable declaration.
     */
    public void visit(VariableDeclAST variable) {
        Type t = typeForName(variable.getTypeName());
        if(variable.getConstantValue() != null &&
                t.getLiteralType() != variable.getConstantValue().getType()) {
            analyzer.addError(
                    "\"" + variable.getConstantValue().getText() +
                    "\" is an invalid constant value for type " + t,
                    variable.getConstantValue()
            );
        } else {
            variable.setType(t);
            variable.setVisibility(currentVisibility);
            currentSignature.getVariables().add(variable);
        }
    }

    /**
     * Visits a method, sets its return type etc.
     */
    public void visit(MethodAST method) {
        method.setReturnType(
                method.getReturnTypeName() == null ?
                        Type.VOID :
                        typeForName(method.getReturnTypeName())
        );
        method.setVisibility(currentVisibility);
        for (ParamDeclAST param : method.getParamDecls()) {
            param.setType(typeForName(param.getTypeName()));
        }
        currentSignature.getMethods().add(method);
    }

    /**
     * Resolves the signature of a single class.
     */
    private Signature resolveSignature(ClassAST classAST) {
        String typeName = classAST.getName().getText();
        if(Type.BUILTIN_TYPES.containsKey(typeName)) {
            analyzer.addError("Can't redefine built-in type \"" + typeName + "\"", classAST.getName());
            return null;
        } else if(resolvedTypes.containsKey(typeName)) {
            analyzer.addError("Class \"" + typeName + "\" already defined", classAST.getName());
            return null;
        } else {
            Type ourType = new Type(typeName);
            classAST.setType(ourType);
            resolvedTypes.put(typeName, ourType);
            // any references to this class prior to its definition
            // have been marked as unresolved types, remove them
            Token nameToken = classAST.getName();
            while(unresolvedTypeNames.contains(nameToken)) {
                unresolvedTypeNames.remove(nameToken);
            }
        }
        currentSignature = new Signature(
                classAST,
                new LinkedList<VariableDeclAST>(),
                new LinkedList<MethodAST>()
        );
        classAST.accept(this);
        return currentSignature;
    }

    /**
     * Returns the type for the given name token.
     */
    private Type typeForName(Token token) {
        String name = token.getText();
        if("VOID".equals(name)) {
            analyzer.addError("\"VOID\" is a special type that can't be referenced in a source file", token);
            return null;
        } else {
            Type result = null;
            if(Type.BUILTIN_TYPES.containsKey(name)) {
                // first check built-in types
                result = Type.BUILTIN_TYPES.get(name);
            } else if(resolvedTypes.containsKey(name)) {
                // then those already found in this program
                result = resolvedTypes.get(name);
            } else {
                // type missing or hasn't been defined yet
                result = new Type(name);
                unresolvedTypeNames.add(token);
            }
            return result;
        }
    }

}
