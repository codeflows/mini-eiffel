package minieiffel.cg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import jode.decompiler.Decompiler;
import junit.framework.TestCase;
import minieiffel.Lexer;
import minieiffel.Parser;
import minieiffel.Source;
import minieiffel.ast.ProgramAST;
import minieiffel.semantics.DefaultSemanticAnalyzer;
import minieiffel.semantics.SemanticAnalyzer;
import minieiffel.semantics.Type;

/**
 * Not really a test case, more of a test driver
 * for the ASM code generator class, i.e. the results
 * are not verified, this class just exercises the
 * code generator with different source files.
 * 
 * Parses a source file, runs it thru semantic analysis,
 * generates bytecode for it and decompiles it back to
 * a source form.
 */
public class CodeGeneratorTestCase extends TestCase {
    
    public void testExample() throws FileNotFoundException {
        
        Source source = new Source(new FileReader("test/examples/bytecodetest.meif"));
        Parser parser = new Parser(new Lexer(source));
        
        ProgramAST program = parser.handleProgram();
        SemanticAnalyzer analyzer = new DefaultSemanticAnalyzer();
        analyzer.analyze(program);
        
        if(!analyzer.getErrors().isEmpty()) {
            System.out.println(analyzer.getErrors());
            return;
        }
        
        CodeGenerator generator = new ASMCodeGenerator();
        
        Map<Type, byte[]> generatedClasses = generator.generateClasses(program);
        TestClassLoader cl = new TestClassLoader();
        
        for(Type t : generatedClasses.keySet()) {
            byte[] bytecode = generatedClasses.get(t);
            System.out.println("Bytecode for " + t +
                    " is " + bytecode.length + " bytes long");
            dumpClass(bytecode, t.getName());
            decompile(t.getName(), bytecode);
            Class klass = cl.loadClass(t.getName(), bytecode);
            try {
                Object o = klass.newInstance();
                System.out.println("Instance: " + o);
            } catch (Exception e) {
                System.out.println("Error instantiating class: " + e);
            }
        }
        
    }
    
    private void decompile(String name, byte[] bytecode) {
        Decompiler decompiler = new Decompiler();
        decompiler.setClassPath(System.getProperty("java.io.tmpdir"));
        StringWriter writer = new StringWriter();
        try {
            decompiler.decompile(name, writer, null);
            System.out.println(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void dumpClass(byte[] bytecode, String name) {
        File tempFile = null;
        FileOutputStream fos = null;
        try {
            tempFile = new File(System.getProperty("java.io.tmpdir") + File.separatorChar + name + ".class");
            System.out.println("Temp class file is " + tempFile);
            fos = new FileOutputStream(tempFile);
            fos.write(bytecode);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.out.println("Error decompiling class: " + e);
        } finally {
            if(fos != null) {
                try { fos.close(); } catch(Exception e) {}
            }
        }
    }

    private static final class TestClassLoader extends ClassLoader {
        Class loadClass(String name, byte[] code) {
            return defineClass(name, code, 0, code.length);
        }
    }
    
}
