package minieiffel;

import java.util.LinkedList;

import minieiffel.Source.Position;
import minieiffel.Token.TokenType;
import minieiffel.Token.Value;

/**
 * Lexer for the Mini-Eiffel language.
 */
public class Lexer {
    
    /** source character stream */
    private Source source;
    
    /** when EOF has been reached, this flag is set */
    private boolean reachedEOF;
    
    /** char read from the source most recently */
    private char currentChar;
    
    /** textual content of the token being constructed ATM */
    private StringBuilder currentTokenText = new StringBuilder();
    
    /** starting position of the token being read at the moment */
    private Position tokenStartPos;

    /** position of the last char retrieved from nextChar() */
    private Position lastCharPos;
    
    /** a peeked token (peeked either by a explicit call to peekToken()
     *  or by the code that handles the tokens "and then" and "or else")
     *  that's returned by the next call to nextToken() */
    private Token peekedToken;
    
    /** current token, i.e. the last one returned from a
     *  call to nextToken() */
    private Token lastToken;

    /** chars that were read during lookaheads but weren't consumed yet */
    private LinkedList<LookaheadChar> lookaheadBuffer = new LinkedList<LookaheadChar>();
    
    /** buffer used while peeking ahead */
    private LinkedList<LookaheadChar> peekBuffer = new LinkedList<LookaheadChar>();
    
    /**
     * Creates a new lexer that reads characters from the given {@link Source}.
     */
    public Lexer(Source source) {
        if(source == null) {
            throw new IllegalArgumentException("Source must be non-null");
        }
        this.source = source;
        // advance to first char
        advance();
    }
    
    /**
     * Returns the current token, that is, the token that was last
     * returned from a call to {@link #nextToken()} (null if no
     * calls have been made)
     */
    public Token currentToken() {
        return lastToken;
    }
    
    /**
     * Peeks at the next token in the source but doesn't consume it.
     */
    public Token peekToken() {
        if(peekedToken == null) {
            peekedToken = getNextToken();
            return peekedToken;
        } else {
            return peekedToken;
        }
    }
    
    /**
     * Scans the source for the next token and returns it. If EOF is
     * reached, a Token with type of {@link Token.TokenType#EOF} is returned.
     * If an erroneous token is reached, a Token with type of
     * {@link Token.TokenType#ERROR} is returned.
     */
    public Token nextToken() {
        Token token;
        if(peekedToken != null) {
            // if there's an unused peeked token, consume it
            token = peekedToken;
            peekedToken = null;
        } else {
            // get a fresh token
            token = getNextToken();
        }
        lastToken = token;
        return token;
    }
    
    /**
     * Gets the next token in the stream and sets its position.
     */
    private Token getNextToken() {
        Token token = scanToken();
        token.setPosition(tokenStartPos);
        return token;
    }
    
    /**
     * Does the actual scanning for tokens.
     */
    private Token scanToken() {
        // while we haven't reached the EOF, skip over any whitespace and comments
        while(!reachedEOF && (Character.isWhitespace(currentChar) || currentChar == '-')) {
            if(currentChar == '\n' && (lastToken == null || lastToken.getType() != TokenType.NEWLINE)) {
                // only return a newline token if the previous token
                // returned wasn't of the same type
                tokenStartPos = lastCharPos;
                advance();
                return new Token(TokenType.NEWLINE, "\n");
            } else if(currentChar == '-') {
                if(peek('-')) {
                    // start of a comment, skip the rest of the line
                    while(!reachedEOF && currentChar != '\n') advance();
                } else {
                    // handle the '-'
                    break;
                }
            } else {
                // insignificant whitespace
                advance();
            }
        }
        // set token start position
        tokenStartPos = lastCharPos;
        // if EOF has been reached, report it accordingly
        if(reachedEOF) {
            return new Token(TokenType.EOF);
        }
        // reset the token text buffer
        currentTokenText.setLength(0);
        currentTokenText.append(currentChar);
        switch(currentChar) {
            // multiple-char operators
            case '\\':
                // \ must be followed by \, i.e. \\
                if(!require('\\')) return tokenScanned(TokenType.ERROR);
                return handleOperator();
            case '<':
                // - if peek('=') succeeds, we have '<='
                // - if it fails, we have '<' only
                // - both are valid operators, same technique is used below
                peek('=');
                return handleOperator();
            case '>':
                peek('=');
                return handleOperator();
            case '/':
                peek('=');
                return handleOperator();
            // single-char operators
            case '.': case '+': case '-': case '*':
            case '^': case '=':
                return handleOperator();
            // char literals
            case '\'':
                return handleCharLiteral();
            // other language constructs (!!, :=, braces, parenthesis etc)
            case '!':
                if(!require('!')) return tokenScanned(TokenType.ERROR);
                return handleOther();
            case ':':
                peek('=');
                return handleOther();
            case '{': case '}': case '(': case ')':
            case ',': case ';':
                return handleOther();
        }
        if(isDigit(currentChar)) {
            return handleNumericLiteral();
        }
        if(isIdentifierStart(currentChar)) {
            // seems like a valid identifier
            return handleIdentifier(true);
        } else if(isIdentifierCharacter(currentChar)) {
            // handle as an invalid identifier
            return handleIdentifier(false);
        }
        // EOF or invalid token
        return tokenScanned(TokenType.ERROR);
    }
    
    /**
     * Scans an identifier and returns the corresponding token.
     * Checks for textual operators (or, and etc) and keywords (if, else etc)
     * and handles them approriately.
     * 
     * <p>Also handles invalid identifiers (like _123abc) correctly if
     * the param validIdentifier is false.</p>
     * 
     * @param validIdentifier whether the start of the id was valid
     */
    private Token handleIdentifier(boolean validIdentifier) {
        while(advance() && isIdentifierCharacter(currentChar)) {
            currentTokenText.append(currentChar);
        }
        // invalid start, return an error token
        if(!validIdentifier) {
            return new Token(TokenType.ERROR, currentTokenText.toString());
        }
        // valid start, check if identifier, keyword, operator etc
        String text = currentTokenText.toString();
        Value value = Token.valueFor(text);
        if(value != null) {
            // check for two-part ops ("and then", "or else")
            if(currentChar == ' ') {
                if(value == Value.AND && secondPartMatches("then")) {
                    return new Token(Value.AND_THEN);
                } else if(value == Value.OR && secondPartMatches("else")) {
                    return new Token(Value.OR_ELSE);
                }
            }
            // token with a predefined value
            return new Token(value);
        } else {
            // arbitrary identifier
            return new Token(TokenType.IDENTIFIER, currentTokenText.toString());
        }
    }
    
    /**
     * For checking whether the 2nd part of a two-part operator matches.
     */
    private boolean secondPartMatches(String end) {
        Position secondPartStart = new Position(lastCharPos.getLine(), lastCharPos.getColumn() + 1);
        if(peek(end.toCharArray())) {
            if(!advance() || !isIdentifierCharacter(currentChar)) {
                return true;
            } else {
                // only the first part will be returned, scan
                // the second (non-matching part) completely,
                // it will be consumed on next call to nextToken()
                currentTokenText.setLength(0);
                currentTokenText.append(end).append(currentChar);
                peekedToken = handleIdentifier(true);
                peekedToken.setPosition(secondPartStart);
            }
        }
        return false;
    }
    
    /**
     * Scans a character literal.
     */
    private Token handleCharLiteral() {
        if(peek('\'', '\'', '\'')) {
            // four adjacent '-chars make up one ' char literal
            advance();
            return new Token(TokenType.CHAR_LITERAL, "'");
        } else if(advance()) { // past opening '
            char value = currentChar;
            currentTokenText.append(currentChar);
            if(currentChar != '\'' && advance()) { // the value, can't be '
                currentTokenText.append(currentChar);
                if(currentChar == '\'') { // past closing '
                    advance();
                    return new Token(TokenType.CHAR_LITERAL, String.valueOf(value));
                }
            }
        }
        return tokenScanned(TokenType.ERROR);
    }
    
    /**
     * Scans a numeric (int,real) literal.
     */
    private Token handleNumericLiteral() {
        // consume digits
        while(advance() && isDigit(currentChar)) {
            currentTokenText.append(currentChar);
        }
        boolean fail = false;
        boolean isReal = false;
        if(!reachedEOF && currentChar == '.') {
            // seems like a real number, count digits after dot
            currentTokenText.append('.');
            int digitsAfterDot = 0;
            for(; advance() && isDigit(currentChar); digitsAfterDot++) {
                currentTokenText.append(currentChar);
            }
            if(digitsAfterDot > 0) {
                isReal = true;
            } else {
                // no digits after dot, fail, but try to consume
                // any identifier chars after the dot first (the loop below)
                // to skip stuff like 1.abc
                fail = true;
            }
        }
        // check for invalid identifiers of form 123abc, 1.1x 1.abc (for example)
        if(!reachedEOF && isIdentifierCharacter(currentChar)) {
            currentTokenText.append(currentChar);
            return handleIdentifier(false);
        }
        TokenType type = fail ? TokenType.ERROR :
                        (isReal ? TokenType.REAL_LITERAL : TokenType.INT_LITERAL);
        return new Token(type, currentTokenText.toString());
    }
    
    /**
     * Called after an operator has been scanned.
     */
    private Token handleOperator() {
        advance();
        Value value = Token.valueFor(currentTokenText.toString());
        if(value == null || value.getType() != TokenType.OPERATOR) {
            throw new RuntimeException("Unknown operator " + currentTokenText.toString());
        }
        return new Token(value);
    }
    
    /**
     * For other language constructs.
     */
    private Token handleOther() {
        advance();
        Value value = Token.valueFor(currentTokenText.toString());
        if(value == null || value.getType() != TokenType.OTHER) {
            throw new RuntimeException("Unknown construct " + currentTokenText.toString());
        }
        return new Token(value);
    }

    /**
     * Called after a token of the specified type has been scanned.
     */
    private Token tokenScanned(TokenType type) {
        advance();
        return new Token(type, currentTokenText.toString());
    }
    
    /**
     * Advances to the next character in the source, setting the
     * appropriate flag if EOF is reached. Returns true if
     * advancing was successful, false if EOF was reached.
     */
    private boolean advance() {
        int next = nextChar();
        if(next == -1) {
            reachedEOF = true;
        } else {
            currentChar = (char)next;
        }
        return !reachedEOF;
    }
    
    /**
     * Returns the next char in the source.
     */
    private int nextChar() {
        int next;
        if(!lookaheadBuffer.isEmpty()) {
            // use a character from previous unsuccessful lookaheads
            LookaheadChar c = lookaheadBuffer.removeFirst();
            next = c.character;
            lastCharPos = c.position;
        } else {
            // simply return next character from the source
            next = source.nextChar();
            lastCharPos = source.currentPosition();
        }
        return next;
    }
    
    /**
     * Requires that specific character(s) should occur next in
     * the source stream. If the required character(s) do not occur,
     * returns or the EOF is reached, fails with an exception.
     */
    private boolean require(char... required) {
        for (int i = 0; i < required.length; i++) {
            if(!advance()) {
                return false;
            }
            currentTokenText.append(currentChar);
            if(required[i] != currentChar) {
                return false;
            }
        }
        return true;
    }

    /**
     * Peeks at the next character(s) in the source.
     * If the peek is not successful, the position in the source
     * is reset to what it was before calling this method
     * (and <code>false</code> is returned).
     * If the peek is successful, position is advanced, the
     * matching characters are added to <code>currentTokenText</code>
     * and <code>true</code> is returned.
     */
    private boolean peek(char... required) {
        boolean peekSuccessful = true;
        for (int i = 0; i < required.length; i++) {
            int peeked = nextChar();
            peekBuffer.addLast(new LookaheadChar(peeked, lastCharPos));
            if( peeked == -1 || (required[i] != (char)peeked) ) {
                peekSuccessful = false;
                break;
            }
        }
        if(peekSuccessful) {
            // peek was successful, add peeked chars to current token's text
            for(LookaheadChar peeked : peekBuffer) {
                currentTokenText.append((char)peeked.character);
            }
            peekBuffer.clear();
        } else {
            // peek failed, roll back and push the unused
            // lookahead chars to the front of the lookahead buffer
            while(!peekBuffer.isEmpty()) {
                lookaheadBuffer.addFirst(peekBuffer.removeLast());
            }
        }
        return peekSuccessful;
    }
    
    /**
     * Returns true if the given character is a valid identifier
     * character, i.e. [A-Za-z0-9_]
     */
    private static final boolean isIdentifierCharacter(char c) {
        return isLetter(c) || isDigit(c) || c == '_';
    }
    
    /**
     * Returns true if the given character is a valid identifier
     * start character, i.e. a letter.
     */
    private static final boolean isIdentifierStart(char c) {
        return isLetter(c);
    }

    /**
     * [a-zA-Z]
     */
    private static final boolean isLetter(char c) {
        return ('a' <= c && c <= 'z') ||
               ('A' <= c && c <= 'Z');
    }

    /**
     * [0-9]
     */
    private static final boolean isDigit(char c) {
        return ('0' <= c && c <= '9');
    }
    
    /**
     * Wraps an unused lookahead character with its position in the stream.
     */
    private static final class LookaheadChar {
        private int character;
        private Position position;
        private LookaheadChar(int c, Position p) {
            this.character = c;
            this.position = p;
        }
    }
    
}
