package minieiffel;

import java.io.FileNotFoundException;
import java.io.FileReader;

import minieiffel.ast.ProgramAST;
import minieiffel.semantics.DefaultSemanticAnalyzer;
import minieiffel.semantics.SemanticAnalyzer;
import minieiffel.semantics.SemanticError;

/**
 * Entry point when run from the command line.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Mini-Eiffel parser\n(C) Jari Aarniala (jari.aarniala@cs.helsinki.fi), 2005\n");
        if(args.length == 2 && "-verbose".equals(args[0])) {
            execute(true, args[1]);
        } else if(args.length == 1 && !"-verbose".equals(args[0])) {
            execute(false, args[0]);
        } else {
            System.out.println("Usage: java -jar minieiffel.jar [-verbose] filename");
        }
    }
    
    /**
     * Executes the parser against the given file.
     * 
     * @return true if parsing succeeds
     */
    public static boolean execute(boolean verbose, String fileName) {
        try {
            Source source = new Source(new FileReader(fileName));
            System.out.print("Parsing '" + fileName + "' ...");
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer);
            ProgramAST program = parser.handleProgram();
            System.out.println(" syntax OK");
            System.out.print("Analysing program semantics ... ");
            SemanticAnalyzer analyzer = new DefaultSemanticAnalyzer();
            analyzer.analyze(program);
            if(analyzer.getErrors().size() > 0) {
                System.out.println("errors found:");
                for (SemanticError error : analyzer.getErrors()) {
                    System.out.println(
                            " " + error.getOffendingToken().getPosition() +
                            " " + error.getMessage()
                    );
                }
                return false;
            } else {
                System.out.println("OK, no errors found");
                System.out.println("Generating Java bytecode - NOT IMPLEMENTED");
                if(verbose) {
                    analyzer.printSymbolTable();
                }
                return true;
            }
        } catch (FileNotFoundException fnfe) {
            System.err.println("Source file (" + fileName + ") couldn't be found");
        } catch (SyntaxException e) {
            System.out.println(" syntax errors found:");
            System.out.println(" " + e.getMessage());
        }
        return false;
    }
    
}
