package minieiffel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minieiffel.Source.Position;

/**
 * A token in a Mini-Eiffel source file, produced by a {@link Lexer}.
 */
public class Token {

    /** the type (identifier, operator, char_literal etc) of this token */
    private TokenType type;
    
    /** the textual content of this token */
    private String text;
    
    /** predefined value (like Value.AND_THEN for "and then") for this token */
    private Value value;
    
    /** the starting position of this token */
    private Position position;
    
    public Token(Value value) {
        setValue(value);
    }
    
    public Token(TokenType type, String text) {
        if(type == null) {
            throw new IllegalArgumentException("Type must be non-null");
        }
        this.type = type;
        this.text = text;
    }

    public Token(TokenType type) {
        this(type, null);
    }
    
    public TokenType getType() {
        return type;
    }
    
    public String getText() {
        return text;
    }
    
    public Value getValue() {
        return value;
    }
    
    public void setValue(Value value) {
        if(value == null) {
            throw new IllegalArgumentException("Value must be non-null");
        }
        this.value = value;
        this.type = value.getType();
        this.text = value.toString();
    }
    
    public Position getPosition() {
        return position;
    }
    
    public void setPosition(Position pos) {
        this.position = pos;
    }
    
    public boolean equals(Object o) {
        if(!(o instanceof Token)) return false;
        Token other = (Token)o;
        if(value != null) {
            return value == other.value;
        }
        if(type == other.type) {
            return (text != null ? text.equals(other.text) : other.text == null);
        }
        return false;
    }

    public String toString() {
        return "Token {" + type + (text != null ? ", " + text : "") + "}";
    }
    
    /**
     * Token types.
     */
    public enum TokenType {
        
        IDENTIFIER,
        KEYWORD,
        OPERATOR,
        EOF,
        ERROR,
        NEWLINE,
        CHAR_LITERAL,
        INT_LITERAL,
        REAL_LITERAL,
        BOOLEAN_LITERAL,
        LITERAL(CHAR_LITERAL, INT_LITERAL, REAL_LITERAL, BOOLEAN_LITERAL),
        OTHER;

        /** category types (e.g. LITERAL) are not types in themselves, but
         *  act as a category for other types */
        private List<TokenType> types;
        
        private TokenType() { }

        private TokenType(TokenType... types) {
            this.types = Arrays.asList(types);
        }
        
        /**
         * Returns true if <code>other</other> equals this type OR
         * if this type is a category type and <code>other</code> belongs to it.
         */
        public boolean isCompatibleWith(TokenType other) {
            return this.equals(other) ||
                   (this.types != null && this.types.contains(other));
        }
        
    }

    /**
     * A predefined value of a token.
     */
    public enum Value {

        // Operators
        DOT(".", TokenType.OPERATOR, 8),
        PLUS("+", TokenType.OPERATOR, 4),
        MINUS("-", TokenType.OPERATOR, 4),
        MULTIPLY("*", TokenType.OPERATOR, 5),
        DIVIDE("/", TokenType.OPERATOR, 5),
        POWER("^", TokenType.OPERATOR, 6),
        EQUALITY("=", TokenType.OPERATOR, 3),
        LESS("<", TokenType.OPERATOR, 3),
        GREATER(">", TokenType.OPERATOR, 3),
        REMAINDER("\\\\", TokenType.OPERATOR, 5),
        LESS_OR_EQUAL("<=", TokenType.OPERATOR, 3),
        GREATER_OR_EQUAL(">=", TokenType.OPERATOR, 3),
        INEQUALITY("/=", TokenType.OPERATOR, 3),
        OR(TokenType.OPERATOR, 1),
        XOR(TokenType.OPERATOR, 1),
        AND_THEN("and then", TokenType.OPERATOR, 2),
        AND(TokenType.OPERATOR, 2),
        OR_ELSE("or else", TokenType.OPERATOR, 1),
        
        // Unary operators
        NOT(TokenType.OPERATOR, 7),
        UNARY_MINUS(TokenType.OPERATOR, 7),
        
        // Keywords
        CLASS(TokenType.KEYWORD),
        END(TokenType.KEYWORD),
        FEATURE(TokenType.KEYWORD),
        IS(TokenType.KEYWORD),
        LOCAL(TokenType.KEYWORD),
        DO(TokenType.KEYWORD),
        IF(TokenType.KEYWORD),
        THEN(TokenType.KEYWORD),
        ELSE(TokenType.KEYWORD),
        ELSEIF(TokenType.KEYWORD),
        FROM(TokenType.KEYWORD),
        UNTIL(TokenType.KEYWORD),
        LOOP(TokenType.KEYWORD),
        
        // Boolean literals
        TRUE(TokenType.BOOLEAN_LITERAL),
        FALSE(TokenType.BOOLEAN_LITERAL),
        
        // Other language constructs
        CONSTRUCTION("!!", TokenType.OTHER),
        LEFT_BRACE("{", TokenType.OTHER),
        RIGHT_BRACE("}", TokenType.OTHER),
        LEFT_PAREN("(", TokenType.OTHER),
        RIGHT_PAREN(")", TokenType.OTHER),
        COMMA(",", TokenType.OTHER),
        COLON(":", TokenType.OTHER),
        SEMICOLON(";", TokenType.OTHER),
        ASSIGNMENT(":=", TokenType.OTHER);
        
        private final String text;
        private final TokenType type;
        
        /* for operators */
        private final int precedence;
        
        private Value(TokenType type) {
            this(type, -1);
        }
        
        private Value(TokenType type, int precedence) {
            this.text = name().toLowerCase();
            this.type = type;
            this.precedence = precedence;
        }
        
        private Value(String text, TokenType type) {
            this(text, type, -1);
        }
        
        private Value(String text, TokenType type, int precedence) {
            this.text = text;
            this.type = type;
            this.precedence = precedence;
        }
        
        public TokenType getType() {
            return type;
        }
        
        public int getPrecedence() {
            return precedence;
        }
        
        public String toString() {
            return text;
        }
        
    }

    /**
     * Returns the constant value for the given text, if any.
     */
    public static Value valueFor(String text) {
        return valuesByText.get(text);
    }
    
    private static final Map<String, Value> valuesByText =
        new HashMap<String, Value>();
    
    private static void initializeValueMappings() {
        Value[] values = Value.values();
        for (Value value : values) {
            valuesByText.put(value.toString(), value);
        }
    }

    static {
        initializeValueMappings();
    }
    
}
