package minieiffel.integration;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import minieiffel.Main;

/**
 * Integration test that runs the examples against the parser
 * as if a user was running them.
 */
public class ExamplesTestCase extends TestCase {
    
    public void testExamples() throws FileNotFoundException {
        List<String> examples = Arrays.asList(
                "account.meif",
                "example.meif",
                "gcd-iterative.meif",
                "gcd-recursive.meif",
                "linkedlist.meif",
                "none.meif"
        );
        for (String file : examples) {
            try {
                assertTrue(
                        "Parsing " + file + " failed",
                        Main.execute(true, "test/examples/" + file)
                );
            } catch(Exception e) {
                e.printStackTrace();
                fail(file + " - " + e.getMessage());
            }
        }
    }
    
}
