package minieiffel;

import java.io.Reader;

/**
 * A unified view to source code character streams from arbitrary sources.
 * 
 * <p>Automatically converts all newlines (Unix/Windows/Mac -style)
 * to Unix-style ones ('\n') to make life easier for the
 * {@link minieiffel.Lexer lexer}.</p>
 * 
 * <p>Keeps track of the 'cursor' position in the source code stream and
 * makes it available thru the {@link #currentPosition()} method.</p>
 */
public class Source {

    private static final int LINE_SEPARATOR = '\n';
    
    private Reader reader;
    private int line = 0;
    private int column = 0;
    private boolean lastWasLineSeparator = false;
    private Integer peeked = null;
    
    /**
     * Creates a new source code reader.
     * 
     * @param reader the character stream to read from
     */
    public Source(Reader reader) {
        if(reader == null) {
            throw new IllegalArgumentException("Reader must be non-null");
        }
        this.reader = reader;
    }
    
    /**
     * Returns the next character from the source or -1 if the end
     * of the source code has been reached. Basically a decorator
     * over {@link java.io.Reader#read()} with unified newline
     * handling added.
     * 
     * @see java.io.Reader#read()
     */
    public int nextChar() {
        try {
            int current;
            if(peeked != null) {
                // if we peeked a char last time and didn't use it, use it now
                current = peeked;
                peeked = null;
            } else {
                // else read a new char
                current = reader.read();
            }
            if(line == 0) {
                // this is the first char read, initialize line/column
                line = 1;
                column = 1;
            } else {
                if(lastWasLineSeparator) {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            lastWasLineSeparator = false;
            // EOF check
            if(current == -1) {
                return -1;
            }
            // line separator handling
            if(isLineSeparator(current)) {
                lastWasLineSeparator = true;
                return LINE_SEPARATOR;
            }
            return current;
        } catch (java.io.IOException e) {
            throw new IOException("Reading from the input reader failed", e);
        }
    }
    
    /**
     * Returns the position at which this reader is situated
     * at the moment (the position of the last character returned
     * from {@link #nextChar()}, null before any characters have
     * been obtained).
     */
    public Position currentPosition() {
        if(line == 0) {
            // no chars have been retrieved yet
            return null;
        }
        return new Position(line, column);
    }
    
    /**
     * Checks whether the current character (or current+next)
     * make up some line separator (mac/windows/*nix).
     */
    private boolean isLineSeparator(int current) throws java.io.IOException {
        char currentChar = (char)current;
        if(currentChar == '\n') {
            // *NIX (\n)
            return true;
        } else if(currentChar == '\r') {
            peeked = reader.read();
            if(peeked != -1) {
                if((char)peeked.intValue() == '\n') {
                    // Windows (\r\n), consume peeked, return '\n'
                    peeked = null;
                    return true;
                }
            }
            // Mac (\r), peeked was not consumed
            return true;
        }
        return false;
    }
    
    /**
     * Position in the source code, (line,column) -pair.
     */
    public static final class Position {
        
        private int line;
        private int column;

        public Position(int line, int column) {
            this.line = line;
            this.column = column;
        }
        
        public int getLine() {
            return line;
        }
        
        public int getColumn() {
            return column;
        }

        public boolean equals(Object o) {
            if(o instanceof Position) {
                Position other = (Position)o;
                return other.line == this.line &&
                       other.column == this.column;
            }
            return false;
        }

        public String toString() {
            return String.format("[L%d,C%d]", line, column);
        }
        
    }

}
