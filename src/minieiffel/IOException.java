package minieiffel;

/**
 * An exception indicating I/O problems (see the Javadoc package
 * description to learn why this exception is unchecked).
 */
public class IOException extends RuntimeException {

    public IOException(String message, Throwable cause) {
        super(message, cause);
    }

    public IOException(String message) {
        super(message);
    }

}
