package minieiffel;

import java.util.List;

import minieiffel.Token.TokenType;
import minieiffel.ast.SimpleExpressionAST;

import junit.framework.Assert;

/**
 * General unit test utility methods.
 */
public class TestCaseUtil {

    public static <E> void assertListContents(List<E> list, E... expected) {
        Assert.assertEquals("List size wrong", expected.length, list.size());
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals("List item at " + i + " wrong", expected[i], list.get(i));
        }
    }
    
    public static SimpleExpressionAST simpleExpr(TokenType type, String value) {
        return new SimpleExpressionAST(new Token(type, value));
    }
    
    
    public static Token id(String name) {
        return new Token(TokenType.IDENTIFIER, name);
    }

}
