package minieiffel.semantics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import minieiffel.Source;
import minieiffel.Token;
import minieiffel.Token.TokenType;
import minieiffel.ast.AssignmentAST;
import minieiffel.ast.ClassAST;
import minieiffel.ast.ConditionalAST;
import minieiffel.ast.ConstructionAST;
import minieiffel.ast.ExpressionAST;
import minieiffel.ast.FeatureAST;
import minieiffel.ast.IfStatementAST;
import minieiffel.ast.IterationAST;
import minieiffel.ast.MethodAST;
import minieiffel.ast.ParamDeclAST;
import minieiffel.ast.ProgramAST;
import minieiffel.ast.ProgramVisitor;
import minieiffel.ast.SimpleExpressionAST;
import minieiffel.ast.VariableDeclAST;
import minieiffel.util.Stack;

/**
 * Default implementation of a semantic analyzer. Implemented
 * as a {@link minieiffel.ast.ProgramVisitor}.
 * 
 * <p>{@link minieiffel.semantics.SignatureResolver Signature resolving}
 * and {@link minieiffel.semantics.TypeInference expression type inference}
 * are implemented in their own classes.</p>
 */
public class DefaultSemanticAnalyzer implements SemanticAnalyzer, ProgramVisitor {
    
    /** for infering expression types */
    private TypeInference inference = new TypeInference(this);
    
    /** errors encountered */
    private List<SemanticError> errors = new LinkedList<SemanticError>();
    
    /** list of all type signatures in the program under analysis */
    private List<Signature> signatures;
    
    /** maps types to a list of method ASTs in that class */
    private Map<Type, List<MethodAST>> methodTypeMappings;
    
    /** maps types to a map that maps variable
     *  definitions to the corresponding variable ASTs */
    private Map<Type, Map<String, VariableDeclAST>> variableTypeMappings;
    
    /** all the available classes mapped by name */
    private Map<String, Type> typesByName;
    
    /** the class that's under analysis at the moment */
    private Type currentType;

    /** the method we're in at the moment */
    private MethodAST currentMethod;
    
    /** stack for lists of local variables, top list being the
     *  current scope */
    private Stack<List<VariableDeclAST>> localVariables =
        new Stack<List<VariableDeclAST>>();
    
    /** keeps track of the local variables of each method
     *  (for printing out the symbol table) */
    private Map<MethodAST, List<VariableDeclAST>> localVariablesForMethod =
        new IdentityHashMap<MethodAST, List<VariableDeclAST>>();
    
    /** keeps track of references to variables and methods */
    private Map<FeatureAST, List<Source.Position>> references =
        new IdentityHashMap<FeatureAST, List<Source.Position>>();
    
    /** dummy variable declarations for the "void" and "current" values so
     *  that the same algorithms can be used on all variable references */
    private VariableDeclAST voidDecl;
    private VariableDeclAST currentDecl;
    
    /**
     * Analyzes the given program.
     */
    public void analyze(ProgramAST program) {
        // first resolve the signatures of the classes in the program
        resolveSignatures(program);
        // then analyze the structure of the program
        analyzeStructure(program);
    }

    /**
     * Returns the list of any semantic errors found during the analysis process.
     */
    public List<SemanticError> getErrors() {
        return errors;
    }

    /**
     * Adds a semantic error to the list.
     */
    public void addError(String message, Token offendingToken) {
        errors.add(new SemanticError(message, offendingToken));
    }

    /**
     * Resolves the type of the variable with the given name
     * inside the given owner. If owner is null, the type
     * currently being analysed is considered as the owner.
     */
    public Type resolveVariableType(Type owner, Token name) {
        VariableDeclAST var = resolveVariable(owner, name);
        if(var != null) {
            return var.getType();
        } else {
            return null;
        }
    }
    
    /**
     * Resolves the return type of the method with the given name
     * and parameter types, if any. If owner type is null,
     * current type is assumed.
     */
    public Type resolveMethodType(Type owner, Token name,
            List<Type> argumentTypes) {
        
        if(owner == null) {
            owner = currentType;
        }
        
        List<MethodAST> matchingMethods =
            findMethods(owner, name.getText(), argumentTypes);
        
        Type result = null;
        if(matchingMethods.size() == 0) {
            // no methods found
            addError(
                    "Undefined method \"" +
                    methodToString(name, argumentTypes)
                    + "\"",
                    name
            );
        } else if(matchingMethods.size() > 1) {
            // more than one option - ambiguity
            addError(
                    "Ambiguous method call \"" +
                    methodToString(name, argumentTypes)
                    + "\"",
                    name
            );
        } else {
            // exactly one match, check visibility first
            MethodAST method = matchingMethods.get(0);
            if(featureNotVisible(method, owner)) {
                addError(
                        "Method \"" + methodToString(name, argumentTypes) +
                        "\" is not visible " +
                        "to class \"" + currentType + "\"",
                        name
                );
            } else {
                // all's well
                registerReference(method, name);
                result = method.getReturnType();
            }
        }
        
        return result;
    }

    /**
     * Called when the visitor enters a class.
     */
    public void enteringClass(ClassAST klass) {
        currentType = klass.getType();
    }

    /**
     * A variant of {@link #enteringClass(ClassAST)} for
     * unit testing purposes.
     */
    public void enteringClass(String name) {
        for (Signature signature : signatures) {
            if(signature.getClassAST().getName().getText().equals(name)) {
                enteringClass(signature.getClassAST());
                break;
            }
        }
    }

    public void leavingClass() {
        currentType = null;
    }

    /**
     * Called when the visitor enters a method with the
     * given signature. Declares the method's parameters
     * (and its return type, if any) as local variables.
     */
    public void enteringMethod(MethodAST method) {

        enteringBlock();
        
        // if return type is not void, declare special variable 'result'
        if(method.getReturnType() != null &&
                method.getReturnType() != Type.VOID) {
            
            VariableDeclAST result = new VariableDeclAST(
                    new Token(TokenType.IDENTIFIER, "result"),
                    new Token(TokenType.IDENTIFIER,
                            method.getReturnType().getName()),
                    null
            );
            result.setType(method.getReturnType());
            localVariables.peek().add(result);
        }

        // declare parameters as local variables
        for (ParamDeclAST param : method.getParamDecls()) {
            visit(
                    new VariableDeclAST(
                            param.getName(),
                            param.getTypeName(),
                            null
                    )
            );
        }
        
        currentMethod = method;
    }
    
    /**
     * Same as {@link #enteringMethod(MethodAST)}, but geared
     * towards unit testing purposes (easier than constructing
     * a MethodAST just for testing and passing it in).
     */
    public void enteringMethod(String name, Type... paramTypes) {
        
        List<MethodAST> candidates =
            findMethods(currentType, name, Arrays.asList(paramTypes));
        if(candidates.size() != 1) {
            throw new RuntimeException(
                    "Found " + candidates.size() +
                    " matching methods for signature " +
                    "when entering method"
            );
        }
        enteringMethod(candidates.get(0));
        
    }

    public void leavingMethod() {
        leavingBlock();
        currentMethod = null;
    }

    public void enteringBlock() {
        localVariables.push(new LinkedList<VariableDeclAST>());
    }

    public void leavingBlock() {
        localVariables.pop();
    }

    /* visitor methods follow */
    
    public void visit(VariableDeclAST var) {
        Type t = typesByName.get(var.getTypeName().getText());
        if(t == null) {
            addError(
                    "Unknown class \"" +
                    var.getTypeName().getText() + "\"",
                    var.getTypeName()
            );
        } else {
            if(var.getName().getText().equals("result") &&
                    currentMethod.getReturnType() != Type.VOID) {
                addError(
                        "Can't redeclare special variable " +
                        "'result' in a method with a return type",
                        var.getName()
                );
            } else if(findLocalVariable(var.getName()) != null) {
                addError(
                        "Variable \"" + var.getName().getText()
                        + "\" already declared in this scope",
                        var.getName()
                );
            } else if(var.getConstantValue() != null &&
                        t.getLiteralType() != var.getConstantValue().getType()) {
                addError(
                        "\"" + var.getConstantValue().getText() +
                        "\" is an invalid constant value for type " + t,
                        var.getName()
                );
            } else {
                var.setType(t);
                registerLocalVariable(var);
            }
        }
    }

    public void visit(ExpressionAST expr) {
        // standalone expression, probably a method call
        inference.inferType(expr);
    }

    public void visit(AssignmentAST assignment) {
        Type exprType = inference.inferType(assignment.getExpression());
        VariableDeclAST variable = resolveVariable(
                null,
                assignment.getIdentifier()
        );
        // only validate if boths types are known
        if(variable != null && variable.getType() != null && exprType != null) {
            
            Type variableType = variable.getType();

            if(assignment.getIdentifier().getText().equals("void") ||
                    assignment.getIdentifier().getText().equals("current")) {
                addError(
                        "Can't assign to special variable '" + 
                        assignment.getIdentifier().getText() + "'",
                        assignment.getLocationToken()
                );
            } else if(variable.getConstantValue() != null) {
                addError(
                        "Can't assign to constant value \"" +
                        assignment.getIdentifier().getText() + "\"",
                        assignment.getLocationToken()
                );
            } else if(assignment.getExpression()
                    instanceof SimpleExpressionAST &&
                    ((SimpleExpressionAST)assignment.getExpression())
                       .getLocationToken().getText().equals("void")) {
                
                // right side is the 'void' keyword (i.e. null),
                // which can be assigned to any variable
                
            } else if(exprType.equals(Type.VOID) ||
                    (!variableType.equals(exprType) &&
                            !variableType.equals(Type.ANY))) {
                
                // if expr is a method returning VOID, or if variable and
                // expr type don't match (and neither of them is
                // of type ANY), report error
                addError(
                        "Can't assign " + exprType + 
                        " value to variable of type " +
                        variableType,
                        assignment.getLocationToken()
                );
                
            }
        }
    }

    public void visit(ConditionalAST conditional) {
        checkExpressionType(
                Type.BOOLEAN,
                conditional.getIfStatement().getGuard(),
                "\"if\" must be of type BOOLEAN"
        );
        for (IfStatementAST elseIfStmt : conditional.getElseIfStatements()) {
            checkExpressionType(
                    Type.BOOLEAN,
                    elseIfStmt.getGuard(),
                    "\"elseif\" must be of type BOOLEAN"
            );
        }
    }

    public void visit(ConstructionAST construction) {
        String id = construction.getIdentifier().getText();
        Type t = resolveVariableType(null, construction.getIdentifier());
        if("current".equals(id) || "void".equals(id) || "result".equals(id)) {
            addError(
                    "Can't construct special variable '" + id + "'",
                    construction.getIdentifier()
            );
        } else {
            construction.setType(t);
        }
    }

    public void visit(IterationAST iteration) {
        checkExpressionType(
                Type.BOOLEAN,
                iteration.getUntil(),
                "\"until\" must be of type BOOLEAN"
        );
    }
    
    public void printSymbolTable() {
        System.out.println("Symbol table for program:");
        for (Signature sig : signatures) {
            System.out.println(
                    " Class " + sig.getClassAST().getName().getText() +
                    " @ " +
                    sig.getClassAST().getLocationToken().getPosition()
            );
            System.out.println("  Variables:");
            for (VariableDeclAST memberVar : sig.getVariables()) {
                printVariable("", memberVar);
            }
            System.out.println("  Methods:");
            for (MethodAST method : sig.getMethods()) {
                printMethod(method);
            }
            System.out.println();
        }
    }
    
    /* private/protected implementation follows */
    
    /**
     * Resolves a variable by the given owner type and name.
     */
    protected VariableDeclAST resolveVariable(Type owner, Token name) {
        
        VariableDeclAST result = null;
        if(owner == null) {
            
            // owner is null, i.e. searching in current type
            owner = currentType;
            
            // check special variables in the current context
            if("void".equals(name.getText())) {
                result = voidDecl;
            } else if("current".equals(name.getText())) {
                currentDecl.setType(currentType);
                result = currentDecl;
            } else {
                // search in local variables
                VariableDeclAST var = findLocalVariable(name);
                if(var != null) {
                    // local variables are naturally always visible,
                    // no need for visibility checks
                    registerReference(var, name);
                    result = var;
                }
            }
            
        }
        
        if(result == null && variableTypeMappings.containsKey(owner)) {
            VariableDeclAST var =
                    variableTypeMappings.get(owner).get(name.getText());
            if(var == null) {
                addError(
                        "No variable \"" + name.getText() + "\" defined " +
                        "in class \"" + owner + "\"",
                        name
                );
            } else {
                
                if(featureNotVisible(var, owner)) {
                    addError(
                            "Variable \"" + name.getText() +
                            "\" is not visible " +
                            "to class \"" + currentType + "\"",
                            name
                    );
                    
                } else {
                    registerReference(var, name);
                    result = var;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Resolves the signatures of the types in the given program
     * and initializes the internal data structures of this class.
     */
    protected void resolveSignatures(ProgramAST program) {
        // first resolve the signatures
        SignatureResolver resolver = new SignatureResolver(this, program);
        signatures = resolver.resolveSignatures();
        // then process them further by indexing their methods and variables
        typesByName = new HashMap<String, Type>(Type.BUILTIN_TYPES);
        methodTypeMappings = new HashMap<Type, List<MethodAST>>();
        variableTypeMappings = new HashMap<Type, Map<String, VariableDeclAST>>();
        for (Signature sig : signatures) {
            Map<String, VariableDeclAST> variableTypes =
                new HashMap<String, VariableDeclAST>();
            for(VariableDeclAST var : sig.getVariables()) {
                variableTypes.put(var.getName().getText(), var);
            }
            Type type = sig.getClassAST().getType();
            typesByName.put(sig.getClassAST().getName().getText(), type);
            methodTypeMappings.put(type, sig.getMethods());
            variableTypeMappings.put(type, variableTypes);
        }
        // initialize special declarations
        voidDecl = new VariableDeclAST(
                new Token(TokenType.IDENTIFIER, "void"),
                new Token(TokenType.IDENTIFIER, "ANY"),
                new Token(TokenType.IDENTIFIER, "_void_value")
        );
        voidDecl.setType(Type.ANY);
        currentDecl = new VariableDeclAST(
                new Token(TokenType.IDENTIFIER, "result"),
                new Token(TokenType.IDENTIFIER, "_name_of_current_type"),
                new Token(TokenType.IDENTIFIER, "_current_value")
        );
    }

    /**
     * Implemented as a separate method to provide a hook for the
     * test cases to alter this behaviour.
     */
    protected void analyzeStructure(ProgramAST program) {
        // rest of the analysis is implemented with the visitor pattern
        program.accept(this);
    }

    /**
     * Checks that the expression is of the given type and reports
     * an error if this isn't the case.
     */
    protected void checkExpressionType(Type type, ExpressionAST expr, String msg) {
        Type t = inference.inferType(expr);
        if(t != null && !t.equals(type)) {
            addError(msg, expr.getLocationToken());
        }
    }
    
    /**
     * Returns the methods in the given type that match
     * the given signature.
     * 
     * @param owner parent class
     * @param name of the method
     * @param argumentTypes types of the methods arguments (can be ANY etc)
     */
    protected List<MethodAST> findMethods(
            Type owner, String name, List<Type> argumentTypes) {

        List<MethodAST> result = new LinkedList<MethodAST>();
        
        if(!methodTypeMappings.containsKey(owner)) {
            return result;
        }
        
        List<MethodAST> methods = methodTypeMappings.get(owner);
        search: for (MethodAST method : methods) {
            if(method.getName().getText().equals(name) && 
                    argumentTypes.size() == method.getParamDecls().size()) {
                // method name and number of arguments match
                Iterator<Type> requiredTypes = argumentTypes.iterator();
                boolean matches = true;
                for (ParamDeclAST param : method.getParamDecls()) {
                    Type required = requiredTypes.next();
                    if(required.equals(Type.VOID) ||
                            (!required.equals(param.getType()) &&
                                    !param.getType().equals(Type.ANY) &&
                                    !required.equals(Type.ANY))
                    ) {
                        matches = false;
                        break;
                    }
                }
                if(matches) {
                    result.add(method);
                }
            }
        }
        
        return result;
    }

    /**
     * Finds a local variable by the given name.
     */
    protected VariableDeclAST findLocalVariable(Token name) {
        
        ListIterator<List<VariableDeclAST>> it =
            localVariables.iterateFromTop();
        while(it.hasPrevious()) {
            List<VariableDeclAST> vars = it.previous();
            for (VariableDeclAST var : vars) {
                if(var.getName().equals(name)) {
                    return var;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Returns true if the given feature is not visible in the current
     * context.
     */
    protected boolean featureNotVisible(FeatureAST feature, Type owner) {
        return !currentType.equals(owner) &&
               !feature.getVisibility().contains(currentType) &&
               !feature.getVisibility().contains(Type.ANY);
    }
    
    /**
     * Registers a reference to a method or a variable.
     */
    private void registerReference(FeatureAST feature, Token token) {
        List<Source.Position> positions = references.get(feature);
        if(positions == null) {
            positions = new LinkedList<Source.Position>();
            references.put(feature, positions);
        }
        positions.add(token.getPosition());
    }

    /**
     * Registers a local variable.
     */
    private void registerLocalVariable(VariableDeclAST var) {
        // register this variable with the map that maps
        // methods to lists of local variable inside them
        List<VariableDeclAST> vars = localVariablesForMethod.get(currentMethod);
        if(vars == null) {
            vars = new LinkedList<VariableDeclAST>();
            localVariablesForMethod.put(currentMethod, vars);
        }
        vars.add(var);
        // also, add the variable to the local variable stack
        localVariables.peek().add(var);
    }
    
    /**
     * Returns a human-readable form of the method call,
     * for example: <code>name(TYPE1,TYPE2)</code>
     */
    private String methodToString(Token name, List<Type> paramTypes) {
        StringBuilder builder = new StringBuilder();
        builder.append(name.getText()).append('(');
        for(Iterator<Type> i = paramTypes.iterator(); i.hasNext(); ) {
            builder.append(i.next().getName());
            if(i.hasNext()) {
                builder.append(',');
            }
        }
        builder.append(')');
        return builder.toString();
    }
    
    /**
     * Converts a list of params to a list of their types.
     */
    private List<Type> paramsToTypes(List<ParamDeclAST> params) {
        List<Type> types = new LinkedList<Type>();
        for (ParamDeclAST param : params) {
            types.add(param.getType());
        }
        return types;
    }
    
    private void printVariable(String indent, VariableDeclAST var) {
        System.out.println(
                indent +
                "    {variable} " +
                var.getName().getText() +
                " : " + var.getType() +
                " @ " +
                var.getLocationToken().getPosition()
        );
        printReferences(indent, var);
    }
    
    private void printMethod(MethodAST method) {
        System.out.println(
                "    {method} " +
                methodToString(
                        method.getName(),
                        paramsToTypes(method.getParamDecls())
                ) +
                (method.getReturnType() != null ?
                        " : " + method.getReturnType() : "") +
                " at " +
                method.getLocationToken().getPosition()
        );
        printReferences("", method);
        List<VariableDeclAST> localVars =
            localVariablesForMethod.get(method);
        if(localVars != null && localVars.size() > 0) {
            System.out.println("    * Local variables:");
            for (VariableDeclAST localVar : localVars) {
                printVariable("  ", localVar);
            }
        }
        System.out.println();
    }
    
    private void printReferences(String indent, FeatureAST feature) {
        List<Source.Position> pointsOfReference =
            references.get(feature);
        if(pointsOfReference == null || pointsOfReference.isEmpty()) {
            System.out.println(indent + "    - not referenced");
        } else {
            System.out.println(
                    indent + 
                    "    - referenced " +
                    pointsOfReference.size() +
                    " time(s) @ " +
                    pointsOfReference
            );
        }
    }
    
}
