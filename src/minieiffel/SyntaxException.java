package minieiffel;

/**
 * Indicates a syntax error in the source code (see the Javadoc package
 * description to learn why this exception is unchecked).
 */
public class SyntaxException extends RuntimeException {

    private Object expected;
    private Token offendingToken;
    
    /**
     * Creates a syntax error exception.
     * 
     * @param expected expected value (mainly for display purposes, thus the overly general type, Object)
     * @param offendingToken token that offended the grammar
     * @param explanation further explanation on the error
     */
    public SyntaxException(Object expected, Token offendingToken, String explanation) {
        super(formatErrorMessage(expected, offendingToken, explanation));
        this.expected = expected;
        this.offendingToken = offendingToken;
    }
    
    /**
     * Creates a syntax error exception.
     * 
     * @param explanation of the error
     */
    public SyntaxException(String explanation, Token offendingToken) {
        super(offendingToken.getPosition() + " " + explanation);
        this.offendingToken = offendingToken;
    }
    
    /**
     * Formats the error message in a nice human-readable way.
     */
    private static String formatErrorMessage(Object e, Token t, String s) {
        StringBuilder builder = new StringBuilder();
        builder.append(t.getPosition()).append(" ");
        boolean hasExplanation = s != null;
        if(hasExplanation) {
            builder.append(s).append(" (");
        }
        builder.append("Expected <").append(e)
               .append("> but got <").append(t).append(">");
        if(hasExplanation) {
            builder.append(")");
        }
        return builder.toString();
    }
    
    public Object getExpected() {
        return expected;
    }
    
    public Token getOffendingToken() {
        return offendingToken;
    }

}
