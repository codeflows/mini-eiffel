package minieiffel.cg;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import minieiffel.Token;
import minieiffel.Token.TokenType;
import minieiffel.Token.Value;
import minieiffel.ast.AssignmentAST;
import minieiffel.ast.BinaryExpressionAST;
import minieiffel.ast.ClassAST;
import minieiffel.ast.ConditionalAST;
import minieiffel.ast.ConstructionAST;
import minieiffel.ast.ExpressionAST;
import minieiffel.ast.ExpressionVisitor;
import minieiffel.ast.InvocationAST;
import minieiffel.ast.IterationAST;
import minieiffel.ast.MethodAST;
import minieiffel.ast.ParamDeclAST;
import minieiffel.ast.ProgramAST;
import minieiffel.ast.ProgramVisitor;
import minieiffel.ast.SimpleExpressionAST;
import minieiffel.ast.UnaryExpressionAST;
import minieiffel.ast.VariableDeclAST;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Default code generator implementation based on ASM.
 * Traverses a program by visiting its constructs
 * (that is, implements the interface {@link ProgramVisitor}).
 * 
 * <p>XXX: not completed</p>
 * 
 * <p>
 * <h4>A note on primitive value handling</h4>
 * Because 'void' (i.e. null) is a valid value for
 * primitive types as well as user-defined ones in Mini-Eiffel,
 * all primitive types are handled as their corresponding
 * Java object types. That is, the ME builtin type <code>INTEGER</code>
 * is handled as an object of type <code>java.lang.Integer</code>
 * in the generated code. This is actually the same way that
 * auto-boxing is handled in Java 5.0.
 * </p>
 */
public class ASMCodeGenerator implements
    CodeGenerator, ProgramVisitor, Opcodes {

    /** map of all generated classes so far */
    private Map<minieiffel.semantics.Type, byte[]> generatedClasses =
        new HashMap<minieiffel.semantics.Type, byte[]>();
    
    /** the class that's processed at the moment*/
    private ClassAST currentClass;
    
    /** AST type of the current class */
    private minieiffel.semantics.Type currentType;
        
    /** ASM ClassWriter that the class is being generated to */
    private ClassWriter classWriter;
    
    /** visitor for the current method we're in */
    private MethodVisitor methodVisitor;
    
    /** current method being generated */
    private MethodAST currentMethod;
    
    /** current local variable index (zero being 'this' etc.) */
    private int localVarIndex;
    
    /** map of local variables by their name */
    private Map<String, LocalVariable> localVariables =
        new LinkedHashMap<String, LocalVariable>();
    
    /**
     * Generates the bytecode for each of the classes in the
     * given program by visiting them each at a time thru the
     * visitor methods of {@link ProgramVisitor}.
     */
    public Map<minieiffel.semantics.Type, byte[]>
                        generateClasses(ProgramAST program) {
                            
        program.accept(this);
        return generatedClasses;
    }

    /**
     * Generates the signature and constructor of a class.
     */
    public void enteringClass(ClassAST klass) {
        
        currentClass = klass;
        currentType = currentClass.getType();
        classWriter = new ClassWriter(true);
        
        // generated classes adhere to JDK 1.3 format and are public
        classWriter.visit(
                V1_3,
                ACC_PUBLIC + ACC_SUPER,
                currentType.getName(),
                null, // signature
                "java/lang/Object",
                null // interfaces
        );
        
        // declare all member fields
        List<VariableDeclAST> constants = null;
        for (VariableDeclAST field : klass.getSignature().getVariables()) {
            
            int accessFlags = ACC_PUBLIC;
            
            // if field is a constant, declare as 'static final'
            if(field.getConstantValue() != null) {
                accessFlags += ACC_STATIC + ACC_FINAL;
                
                // constant value needs to be set later in
                // the static initializer block
                if(constants == null) {
                    constants = new LinkedList<VariableDeclAST>();
                }
                constants.add(field);
            }
            
            // declare field
            classWriter.visitField(
                    accessFlags,
                    field.getName().getText(),
                    convertType(field.getType()).toString(),
                    null,
                    null
            ).visitEnd();
            
        }

        // create a static initializer block for any constant values
        if(constants != null) {
            
            MethodVisitor staticInit =
                classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            staticInit.visitCode();

            for (VariableDeclAST constant : constants) {
                setValueOfConstant(
                        staticInit,
                        constant.getName().getText(),
                        constant.getConstantValue()
                );
            }
            
            staticInit.visitInsn(RETURN);
            staticInit.visitMaxs(0, 0);
            staticInit.visitEnd();
        }
        
        // all generated classes have an empty constructor, create one
        MethodVisitor mv =
            classWriter.visitMethod(
                    ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        // push first local var, i.e. 'this' to stack
        mv.visitVarInsn(ALOAD, 0);
        // invoke super(), i.e. java.lang.Object.<init>()
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Called when a class has been processed.
     */
    public void leavingClass() {
        // close the class
        classWriter.visitEnd();
        // generate and store the bytecode
        generatedClasses.put(currentType, classWriter.toByteArray());
        currentType = null;
    }

    /**
     * Generates the method signature bytecode.
     */
    public void enteringMethod(MethodAST method) {
        
        currentMethod = method;
        localVarIndex = 0;

        // get the textual descriptor for this method
        String desc = getMethodDescriptor(method);
        
        // visit the method's signature
        methodVisitor = classWriter.visitMethod(
                ACC_PUBLIC,
                method.getName().getText(),
                desc,  // description
                null,  // signature
                null   // exceptions
        );
        
        // start generating the method's code
        methodVisitor.visitCode();
        
        // declare "this" as the first local variable
        addLocalVariable(
                methodVisitor,
                "this",
                currentType,
                null,
                false
        );
        
        // declare each parameter as a local variable
        for (ParamDeclAST param : method.getParamDecls()) {
            addLocalVariable(
                    methodVisitor,
                    param.getName().getText(),
                    param.getType(),
                    null,
                    true
            );
        }

        // add special variable "result" if we have a return type
        if(method.getReturnType() != minieiffel.semantics.Type.VOID) {
            addLocalVariable(
                    methodVisitor,
                    "result",
                    method.getReturnType(),
                    null,
                    true
            );
        }

    }

    /**
     * Wraps up the generation of a method's bytecode.
     */
    public void leavingMethod() {
        
        Label returnLabel = new Label();
        methodVisitor.visitLabel(returnLabel);
        
        // if return type is not void, return variable "result"
        if(currentMethod.getReturnType() != minieiffel.semantics.Type.VOID) {
            methodVisitor.visitVarInsn(
                    ALOAD,
                    localVariables.get("result").index
            );
            methodVisitor.visitInsn(ARETURN);
        } else {
            methodVisitor.visitInsn(RETURN);
        }

        // mark end
        Label endLabel = new Label();
        methodVisitor.visitLabel(endLabel);
       
        // visit each local variable
        for (Map.Entry<String, LocalVariable> entry : localVariables.entrySet()) {
            String name = entry.getKey();
            LocalVariable var = entry.getValue();
            methodVisitor.visitLocalVariable(
                    name,
                    var.type.getDescriptor(),
                    null,
                    var.startLabel,
                    endLabel,
                    var.index
            );
        }
        
        // let ASM calculate max stack depth etc
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
        
        // cleanup
        methodVisitor = null;
        currentMethod = null;
        localVariables.clear();
        
    }

    public void enteringBlock() {
        // TODO Auto-generated method stub
        
    }

    public void leavingBlock() {
        // TODO Auto-generated method stub
        
    }

    /**
     * Called for each local variable inside a method's body.
     */
    public void visit(VariableDeclAST var) {
        addLocalVariable(
                methodVisitor,
                var.getName().getText(),
                var.getType(),
                var.getConstantValue(),
                true
        );
    }

    public void visit(AssignmentAST assignment) {
        String variableName = assignment.getIdentifier().getText();
        methodVisitor.visitLabel(new Label());
        if(localVariables.containsKey(variableName)) {
            // store to a local variable
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitVarInsn(
                    ASTORE,
                    localVariables.get(variableName).index
            );
        } else {
            // store to a member field
            VariableDeclAST field = findField(variableName);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitFieldInsn(
                    PUTFIELD,
                    currentType.getName(),
                    variableName,
                    convertType(field.getType()).getDescriptor()
            );
        }
    }

    public void visit(ConditionalAST conditional) {
        // TODO Auto-generated method stub
        
    }

    public void visit(ConstructionAST construction) {
        // TODO Auto-generated method stub
        
    }

    public void visit(IterationAST iteration) {
        // TODO Auto-generated method stub
        
    }
    
    public void visit(ExpressionAST expr) {
        if(expr.getType() == minieiffel.semantics.Type.VOID) {
            //throw new RuntimeException("Void not impl");
        } else if (expr.getType().isPrimitive()) {
            // primitive result, push new object for result
            Type type = convertType(expr.getType());
            Type primitiveType = getPrimitiveType(type.getClassName());
            methodVisitor.visitLabel(new Label());
            methodVisitor.visitTypeInsn(NEW, type.getInternalName());
            expr.accept(new ExpressionCodeGenerator());
            methodVisitor.visitMethodInsn(
                    INVOKESPECIAL,
                    type.getInternalName(),
                    "<init>",
                    "(" + primitiveType.getDescriptor() + ")V"
            );
        } else {
            //expr.accept(new ExpressionCodeGenerator());
        }
    }
    
    /* ExpressionVisitor implementation */
    
    private final class ExpressionCodeGenerator implements ExpressionVisitor {
        
        public void visit(SimpleExpressionAST expr) {
            if(TokenType.LITERAL.isCompatibleWith(expr.getLocationToken().getType())) {
                Object value = convertLiteralValue(expr.getLocationToken());
                methodVisitor.visitLdcInsn(value);
            } else if(expr.getLocationToken().getType() == TokenType.IDENTIFIER) {
                if(expr.getType().isPrimitive()) {
                    LocalVariable var =
                        localVariables.get(expr.getLocationToken().getText());
                    Type type = convertType(expr.getType());
                    if(var != null) {
                        // push local var to stack
                        methodVisitor.visitVarInsn(ALOAD, var.index);
                    } else {
                        // read member field
                        methodVisitor.visitVarInsn(ALOAD, 0);
                        methodVisitor.visitFieldInsn(
                                GETFIELD,
                                currentType.getName(),
                                expr.getLocationToken().getText(),
                                type.getDescriptor()
                        );
                    }
                    getPrimitiveValue(type);
                } else {
                    throw new RuntimeException("Not impl: non-primitvei: " + expr);
                }
            } else {
                throw new RuntimeException("Unknown token type");
            }
        }
        
        public void visit(UnaryExpressionAST expr) {
            throw new RuntimeException("Notimpl");
        }

        public void visit(BinaryExpressionAST expr) {

            if(expr.getOperator().getValue() == Value.EQUALITY
                    || expr.getOperator().getValue() == Value.INEQUALITY) {
                
                // (in)equality is defined for all objects (primitives included)
                // simply using Object.equals(obj)

                wrapPrimitive(expr.getLhs());
                wrapPrimitive(expr.getRhs());
                
                methodVisitor.visitMethodInsn(
                        INVOKEVIRTUAL,
                        convertType(expr.getLhs().getType()).getInternalName(),
                        "equals",
                        "(Ljava/lang/Object;)Z"
                );
                
            } else if(expr.getType().isPrimitive()) {
                
                boolean isReal = (expr.getType() == minieiffel.semantics.Type.REAL);
                
                expr.getLhs().accept(this);
                if(isReal) convertToRealIfNecessary(expr.getLhs());
                expr.getRhs().accept(this);
                if(isReal) convertToRealIfNecessary(expr.getRhs());

                if(expr.getType() == minieiffel.semantics.Type.INTEGER) {
                    switch(expr.getOperator().getValue()) {
                    case PLUS:
                        methodVisitor.visitInsn(IADD); break;
                    case MULTIPLY:
                        methodVisitor.visitInsn(IMUL); break;
                    case DIVIDE:
                        methodVisitor.visitInsn(IDIV); break;
                    case MINUS:
                        methodVisitor.visitInsn(ISUB); break;
                    case REMAINDER:
                        methodVisitor.visitInsn(IREM); break;
                    default:
                        throw new RuntimeException("Not impl: " + expr.getOperator());
                    }
                } else if(isReal) {
                    switch(expr.getOperator().getValue()) {
                    case PLUS:
                        methodVisitor.visitInsn(FADD); break;
                    case MULTIPLY:
                        methodVisitor.visitInsn(FMUL); break;
                    case DIVIDE:
                        methodVisitor.visitInsn(FDIV); break;
                    case MINUS:
                        methodVisitor.visitInsn(FSUB); break;
                    default:
                        throw new RuntimeException("Not impl: " + expr.getOperator());
                    }
                } else if(expr.getType() == minieiffel.semantics.Type.BOOLEAN) {
                    switch(expr.getOperator().getValue()) {
                    case LESS:
                    case LESS_OR_EQUAL:
                    case GREATER:
                    case GREATER_OR_EQUAL:
                    case AND:
                    case OR:
                    }
                } else {
                    throw new RuntimeException("Not impl: " + expr.getType());
                }
            } else {
                throw new RuntimeException("Non-primitive");
            }
        }
        
        public void visit(InvocationAST expr) {
            throw new RuntimeException("Notimpl");
        }
        
        private void convertToRealIfNecessary(ExpressionAST expr) {
            if(expr.getType() != minieiffel.semantics.Type.REAL) {
                methodVisitor.visitInsn(I2F);
            }
        }
        
        private void wrapPrimitive(ExpressionAST expr) {
            if(expr.getType().isPrimitive()) {
                Type type = convertType(expr.getType());
                Type primitiveType = getPrimitiveType(type.getClassName());
                methodVisitor.visitTypeInsn(NEW, type.getInternalName());
                methodVisitor.visitInsn(DUP);
                expr.accept(this);
                methodVisitor.visitMethodInsn(
                        INVOKESPECIAL,
                        type.getInternalName(),
                        "<init>",
                        "(" + primitiveType.getDescriptor() + ")V"
                );
            } else {
                expr.accept(this);
            }
        }

    }

    /* protected implementation */
    
    /**
     * Adds a local variable to the current method.
     * 
     * @param name of the variable
     * @param astType (Mini-Eiffel) type of the variable
     * @param literalValue of the variable (can be null)
     * @param initializeAsNull if true, set value to null
     * @return LocalVariable object containing the variable's information
     */
    protected LocalVariable addLocalVariable(
            MethodVisitor visitor, String name,
            minieiffel.semantics.Type astType,
            Token literalValue,
            boolean initializeAsNull)
    {
        
        if(localVariables.containsKey(name)) {
            throw new RuntimeException(
                    "Local variable '" + name +
                    "' already defined"
            );
        }
        
        Label label = new Label();
        visitor.visitLabel(label);
        LocalVariable var = new LocalVariable(
                name,
                convertType(astType),
                label,
                localVarIndex
        );
        localVariables.put(name, var);
        
        if(literalValue != null) {
            setValueOfLocalVariable(visitor, localVarIndex, literalValue);
        } else if(initializeAsNull) {
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitVarInsn(ASTORE, localVarIndex);
        }
        
        localVarIndex++;
        return var;
    }

    /**
     * Sets the value of a constant (static final) field
     * to the given literal value.
     */
    protected void setValueOfConstant(
            MethodVisitor visitor, String name, Token literal) {
        
        Object value = createLiteralValue(visitor, literal);
        visitor.visitFieldInsn(
                PUTSTATIC,
                currentType.getName(),
                name,
                Type.getDescriptor(value.getClass())
        );
    }

    /**
     * Sets the given literal as the value of the specified local variable.
     */
    protected void setValueOfLocalVariable(
            MethodVisitor visitor, int varIndex, Token literal) {
        
        createLiteralValue(visitor, literal);
        visitor.visitVarInsn(ASTORE, varIndex);
    }
    
    /**
     * Converts the given literal value to its Java counterpart
     * and pushes the result onto the stack.
     */
    protected Object createLiteralValue(MethodVisitor visitor, Token literal) {
        
        // convert Mini-Eiffel string constant value
        // to corresponding Java object value
        Object value = convertLiteralValue(literal);
        
        // get the corresponding Java primitive type
        Type primitiveType = getPrimitiveType(value.getClass().getName());
        
        // invoke new on the object type (e.g. Integer),
        // passing in the primitive value as parameter
        String internalName = Type.getInternalName(value.getClass());
        visitor.visitTypeInsn(NEW, internalName);
        visitor.visitInsn(DUP);
        visitor.visitLdcInsn(value);
        visitor.visitMethodInsn(
                INVOKESPECIAL,
                internalName,
                "<init>",
                "(" + primitiveType.getDescriptor() + ")V"
        );
        
        return value;

    }
    
    /**
     * Returns the method descriptor for the given Mini-Eiffel method.
     */
    protected String getMethodDescriptor(MethodAST method) {
        Type[] argumentTypes = new Type[method.getParamDecls().size()];
        int i = 0;
        for (ParamDeclAST arg : method.getParamDecls()) {
            argumentTypes[i++] = convertType(arg.getType());
        }
        return Type.getMethodDescriptor(
                convertType(method.getReturnType()),
                argumentTypes
        );
    }
    
    /**
     * Converts a Mini-Eiffel type to an ASM type.
     */
    protected Type convertType(minieiffel.semantics.Type type) {
        if(type.equals(minieiffel.semantics.Type.ANY)) {
            return Type.getType(Object.class);
        } else if(type.equals(minieiffel.semantics.Type.BOOLEAN)) {
            return Type.getType(Boolean.class);
        } else if(type.equals(minieiffel.semantics.Type.CHARACTER)) {
            return Type.getType(Character.class);
        } else if(type.equals(minieiffel.semantics.Type.INTEGER)) {
            return Type.getType(Integer.class);
        } else if(type.equals(minieiffel.semantics.Type.REAL)) {
            return Type.getType(Float.class);
        } else if(type.equals(minieiffel.semantics.Type.VOID)) {
            return Type.VOID_TYPE;
        } else {
            return Type.getType("L" + type.getName() + ";");
        }
    }
    
    /**
     * Converts a Mini-Eiffel literal value to its Java counterpart.
     */
    protected Object convertLiteralValue(Token literalToken) {
        String value = literalToken.getText();
        switch(literalToken.getType()) {
        case INT_LITERAL:
            return Integer.valueOf(value);
        case REAL_LITERAL:
            return Float.valueOf(value);
        case BOOLEAN_LITERAL:
            return Boolean.valueOf(value);
        case CHAR_LITERAL:
            return Character.valueOf(value.charAt(0));
        default:
            throw new RuntimeException(
                    "Unknown literal value type " +
                    literalToken.getType()
            );
        }
    }

    /**
     * Returns the primitive type corresponding to the
     * given class name, e.g. "java.lang.Integer"
     * => Type.INT_TYPE
     */
    protected Type getPrimitiveType(String name) {
        if("java.lang.Integer".equals(name)) return Type.INT_TYPE;
        if("java.lang.Float".equals(name)) return Type.FLOAT_TYPE;
        if("java.lang.Boolean".equals(name)) return Type.BOOLEAN_TYPE;
        if("java.lang.Character".equals(name)) return Type.CHAR_TYPE;
        throw new RuntimeException(
                "Unknown/unsupported primitive type " + name
        );
    }
    
    /**
     * Calls "intValue()" on "java.lang.Integer", i.e. pushes
     * the primitive value of a primitive Object value to the stack.
     */
    protected void getPrimitiveValue(Type type) {
        
        String name = type.getClassName();
        Type primitiveType = getPrimitiveType(name);
        
        String method;
        if("java.lang.Integer".equals(name)) method = "intValue";
        else if("java.lang.Float".equals(name)) method = "floatValue";
        else if("java.lang.Boolean".equals(name)) method = "booleanValue";
        else if("java.lang.Character".equals(name)) method = "charValue";
        else
            throw new RuntimeException(
                    "Unknown/unsupported primitive type " + name
            );
        
        methodVisitor.visitMethodInsn(
                INVOKEVIRTUAL,
                type.getInternalName(),
                method,
                "()" + primitiveType.getDescriptor()
        );
        
    }
    
    /**
     * Finds the field (i.e. the member variable) of the current
     * class with the given name.
     */
    protected VariableDeclAST findField(String name) {
        for (VariableDeclAST var :
                currentClass.getSignature().getVariables()) {
            if(var.getName().getText().equals(name)) {
                return var;
            }
        }
        // this shouldn't happen since semantic analysis
        // has already been done
        throw new RuntimeException("No such field: " + name);
    }
    
    /**
     * Wraps the information needed to handle a local variable in ASM.
     */
    private static final class LocalVariable {
        private String name;
        private Type type;
        private Label startLabel;
        private Label endLabel;
        private int index;
        private LocalVariable(String name, Type type, Label startLabel, int index) {
            this.name = name;
            this.type = type;
            this.startLabel = startLabel;
            this.index = index;
        }
        public String toString() {
            return name + ":" + type + ": [" + startLabel + "," + endLabel + "], idx: " + index;
        }
    }

    
}
