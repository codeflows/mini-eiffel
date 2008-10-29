package minieiffel.semantics;

import java.util.HashMap;
import java.util.Map;

import minieiffel.Token.TokenType;

/**
 * A type (class) in a Mini-Eiffel program. Either a
 * predefined type (such as <code>INTEGER</code> or <code>ANY</code>)
 * or a user-defined one.
 */
public class Type implements Comparable<Type> {

    public static final Type INTEGER = new Type("INTEGER", TokenType.INT_LITERAL);
    public static final Type REAL = new Type("REAL", TokenType.REAL_LITERAL);
    public static final Type CHARACTER = new Type("CHARACTER", TokenType.CHAR_LITERAL);
    public static final Type BOOLEAN = new Type("BOOLEAN", TokenType.BOOLEAN_LITERAL);
    
    /** special type representing the 'void' return type of functions */
    public static final Type VOID = new Type("VOID", true);
    
    public static final Type NONE = new Type("NONE", false);
    public static final Type ANY = new Type("ANY", false);

    /** all builtin types mapped by their name */
    public static final Map<String, Type> BUILTIN_TYPES =
        buildMap(INTEGER, REAL, CHARACTER, BOOLEAN, VOID, NONE, ANY);
    
    private static Map<String, Type> buildMap(Type... types) {
        Map<String, Type> result = new HashMap<String, Type>();
        for (Type type : types) {
            result.put(type.getName(), type);
        }
        return result;
    }

    /** name of this type */
    private String name;
    
    /** is this type primitive? */
    private boolean primitive;

    /** if primitive, the corresponding literal type */
    private TokenType literalType;
    
    public Type(String name) {
        this(name, false);
    }
    
    public Type(String name, boolean primitive) {
        this.name = name;
        this.primitive = primitive;
    }
    
    public Type(String name, TokenType literalType) {
        this(name, true);
        this.literalType = literalType;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isPrimitive() {
        return primitive;
    }
    
    public TokenType getLiteralType() {
        return literalType;
    }

    public String toString() {
        return name;
    }
    
    public boolean equals(Object o) {
        if(o instanceof Type) {
            Type t = (Type)o;
            return this.name.equals(t.name) &&
                   this.primitive == t.primitive;
        }
        return false;
    }
    
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Types are compared by their names.
     */
    public int compareTo(Type t) {
        return name.compareTo(t.name);
    }
    
    public boolean isCompatibleWith(Type type) {
        return (this.equals(type) ||
                this == Type.ANY ||
                type == Type.ANY) &&
               (this != Type.VOID &&
                type != Type.VOID);
    }
    
}
