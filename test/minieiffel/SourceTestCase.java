package minieiffel;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;
import minieiffel.Source.Position;

public class SourceTestCase extends TestCase {
    
    private Source source;
    
    /**
     * Creates a new source stream with the given content.
     */
    private void createSource(String source) {
        this.source = new Source(new StringReader(source));
    }

    /**
     * Returns the rest of the source stream as a string.
     */
    private String consumeAll() {
        StringBuilder buffer = new StringBuilder();
        int c;
        while((c = source.nextChar()) != -1) {
            buffer.append((char)c);
        }
        return buffer.toString();
    }
    
    public void testReaderMustBeNonNull() {
        try {
            new Source(null);
            fail("Creating a Source with null Reader should've failed");
        } catch(IllegalArgumentException e) {
            assertEquals("Reader must be non-null", e.getMessage());
        }
    }
    
    public void testInstantEOF() throws IOException {
        createSource("");
        assertNull(source.currentPosition());
        assertEquals(-1, source.nextChar());
    }
    
    public void testCrossPlatformLineSeparatorHandling() {
        createSource("Mac\rWin\r\n*nix\n");
        assertEquals("Mac\nWin\n*nix\n", consumeAll());
    }
    
    public void testPosition() {
        createSource("abc\nde\r\n\nfg");
        assertNull(source.currentPosition());
        assertEquals('a', source.nextChar());
        assertEquals(new Position(1,1), source.currentPosition());
        assertEquals('b', source.nextChar());
        assertEquals(new Position(1,2), source.currentPosition());
        assertEquals('c', source.nextChar());
        assertEquals(new Position(1,3), source.currentPosition());
        assertEquals('\n', source.nextChar());
        assertEquals(new Position(1,4), source.currentPosition());
        assertEquals('d', source.nextChar());
        assertEquals(new Position(2,1), source.currentPosition());
        assertEquals('e', source.nextChar());
        assertEquals(new Position(2,2), source.currentPosition());
        assertEquals('\n', source.nextChar());
        assertEquals(new Position(2,3), source.currentPosition());
        assertEquals('\n', source.nextChar());
        assertEquals(new Position(3,1), source.currentPosition());
        assertEquals('f', source.nextChar());
        assertEquals(new Position(4,1), source.currentPosition());
        assertEquals('g', source.nextChar());
        assertEquals(new Position(4,2), source.currentPosition());
        assertEquals(-1, source.nextChar());
    }

}
