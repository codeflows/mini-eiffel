package minieiffel;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import minieiffel.Token.TokenType;
import minieiffel.Token.Value;
import minieiffel.ast.AssignmentAST;
import minieiffel.ast.ClassAST;
import minieiffel.ast.ConditionalAST;
import minieiffel.ast.ConstructionAST;
import minieiffel.ast.ExpressionAST;
import minieiffel.ast.FeatureAST;
import minieiffel.ast.FeatureBlockAST;
import minieiffel.ast.IfStatementAST;
import minieiffel.ast.InstructionAST;
import minieiffel.ast.InstructionsAST;
import minieiffel.ast.IterationAST;
import minieiffel.ast.MethodAST;
import minieiffel.ast.ParamDeclAST;
import minieiffel.ast.ProgramAST;
import minieiffel.ast.VariableDeclAST;

/**
 * Parses a Mini-Eiffel source file and creates an abstract
 * syntax tree corresponding to the program's structure.
 * Delegates expression parsing to the
 * {@link minieiffel.ExpressionParser} class.
 */
public class Parser {
    
    /** source of all our tokens */
    private Lexer lexer;

    /**
     * Creates a parser that retrieves token from the given lexer and
     * reports events to the given semantics module.
     */
    public Parser(Lexer lexer) {
        if(lexer == null) {
            throw new IllegalArgumentException("Lexer must be non-null");
        }
        this.lexer = lexer;
        lexer.nextToken();
    }

    /**
     * Called when a token has been successfully consumed.
     * Returns the consumed token and advances to the next token.
     */
    private Token tokenConsumed() {
        Token consumedToken = lexer.currentToken();
        lexer.nextToken();
        return consumedToken;
    }
    
    /**
     * Consumes the current token and check that it has the correct predefined value.
     * Additional explanation for error messages can be defined.
     */
    private Token consumeToken(Value value, String explanation) {
        if(lexer.currentToken().getValue() != value) {
            throw new SyntaxException(value, lexer.currentToken(), explanation);
        }
        return tokenConsumed();
    }
    
    private Token consumeToken(Value value) {
        return consumeToken(value, null);
    }
    
    /**
     * Consumes the current token and check that it has the correct type.
     * Additional explanation for error messages can be defined.
     */
    private Token consumeToken(TokenType type, String explanation) {
        if(!type.isCompatibleWith(lexer.currentToken().getType())) {
            throw new SyntaxException(type, lexer.currentToken(), explanation);
        }
        return tokenConsumed();
    }

    private Token consumeToken(TokenType type) {
        return consumeToken(type, null);
    }
    
    private void skipPotentialNewline() {
        if(lexer.currentToken().getType() == TokenType.NEWLINE) lexer.nextToken();
    }

    /**
     * Handles an identifier list (used in param/variable decls) of form
     *  (id)+ :
     * and returns the identifiers. NOTE: the current token must be the
     * first identifier.
     */
    private List<Token> handleIdentifierList() {
        List<Token> names = new LinkedList<Token>();
        while(true) {
            // add name of current identifier
            names.add(consumeToken(TokenType.IDENTIFIER));
            // if comma, loop again
            if(lexer.currentToken().getValue() == Value.COMMA) {
                lexer.nextToken();
            } else {
                break;
            }
        }
        consumeToken(Value.COLON);
        return names;
    }

    /* Methods for actual parsing follow */
    
    /**
     * Handles object construction (e.g. <code>!! my</code>).
     */
    public ConstructionAST handleConstruction() {
        consumeToken(Value.CONSTRUCTION);
        Token identifier = consumeToken(
                TokenType.IDENTIFIER,
                "!! must be followed by the name of the object to be created"
        );
        return new ConstructionAST(identifier);
    }

    /**
     * Handle variable declarations.
     */
    public List<VariableDeclAST> handleVariableDecl() {
        return handleVariableDecl(true);
    }
    
    public List<VariableDeclAST> handleVariableDecl(boolean skipNewlines) {
        List<VariableDeclAST> decls = new LinkedList<VariableDeclAST>();
        // loop while the current token is an identifier that is the start
        // of a variable declaration (i.e. followed by a comma and more
        // identifiers or by a colon and the type of the variable)
        while(lexer.currentToken().getType() == TokenType.IDENTIFIER &&
                (lexer.peekToken().getValue() == Value.COLON ||
                 lexer.peekToken().getValue() == Value.COMMA)) {
            List<Token> names = handleIdentifierList();
            Token type = consumeToken(
                    TokenType.IDENTIFIER,
                    "Invalid type name for a variable");
            Token value = handleConstantDecl();
            for (Token name : names) {
                decls.add(new VariableDeclAST(name, type, value));
            }
            if(skipNewlines) skipPotentialNewline();
        }
        return decls;
    }

    /**
     * Handles a CONSTANT_DECL, returning the literal value token
     * (or null if the decl is empty).
     */
    public Token handleConstantDecl() {
        if(lexer.currentToken().getValue() == Value.IS) {
            lexer.nextToken();
            return consumeToken(TokenType.LITERAL, "Invalid value for a variable");
        }
        return null;
    }
    
    /**
     * Handles a TYPE_NAME (in practice just an identifier)
     */
    public Token handleTypeName() {
        return consumeToken(TokenType.IDENTIFIER, "Type name missing");
    }
    
    /**
     * Handles a RETURN_TYPE, returning the type (identifier) name or
     * null if return type is empty.
     */
    public Token handleReturnType() {
        if(lexer.currentToken().getValue() == Value.COLON) {
            lexer.nextToken();
            return handleTypeName();
        }
        return null;
    }
    
    /**
     * Parses a parameter list to list of ParamDeclAST nodes.
     */
    public List<ParamDeclAST> handleParamList() {
        List<ParamDeclAST> params = new LinkedList<ParamDeclAST>();
        while(lexer.currentToken().getType() == TokenType.IDENTIFIER) {
            List<Token> identifiers = handleIdentifierList();
            Token type = consumeToken(TokenType.IDENTIFIER, "Type name missing");
            for (Token id : identifiers) {
                params.add(new ParamDeclAST(id, type));
            }
            if(lexer.currentToken().getValue() == Value.SEMICOLON) {
                // we have a semicolon (;), there should be an identifier after that
                lexer.nextToken();
                if(lexer.currentToken().getType() != TokenType.IDENTIFIER) {
                    throw new SyntaxException(
                            TokenType.IDENTIFIER,
                            lexer.currentToken(),
                            "More params must follow after a semicolon"
                    );
                }
            }
        }
        return params;
    }

    /**
     * See {@link #handleParamList()}, same thing but adds a layer to the top
     * (allows empty list).
     */
    public List<ParamDeclAST> handleParams() {
        if(lexer.currentToken().getValue() == Value.LEFT_PAREN) {
            lexer.nextToken();
            List<ParamDeclAST> params = handleParamList();
            consumeToken(Value.RIGHT_PAREN, "Missing right paren after parameters");
            return params;
        }
        return Collections.emptyList();
    }

    /**
     * Handles a local declaration block.
     */
    public List<VariableDeclAST> handleLocalDeclarations() {
        if(lexer.currentToken().getValue() == Value.LOCAL) {
            lexer.nextToken();
            skipPotentialNewline();
            return handleVariableDecl();
        }
        return Collections.emptyList();
    }

    /**
     * Handles the visibility -part of a FEATURE returning
     * a list of typenames to which the feature is visible.
     * Can be null (the same as specifying "{ ANY }") or
     * empty ("{}", same as "{ NONE }").
     */
    public List<Token> handleVisibility() {
        List<Token> typeNames = null;
        if(lexer.currentToken().getValue() == Value.LEFT_BRACE) {
            typeNames = new LinkedList<Token>();
            lexer.nextToken();
            if(lexer.currentToken().getValue() == Value.RIGHT_BRACE) {
                // {}
                lexer.nextToken();
            } else {
                while(true) {
                    typeNames.add(handleTypeName());
                    if(lexer.currentToken().getValue() == Value.COMMA) {
                        // comma, there should be more
                        lexer.nextToken();
                    } else {
                        consumeToken(
                                Value.RIGHT_BRACE,
                                "Expecting } after visibility listing"
                        );
                        break;
                    }
                }
            }
        }
        return typeNames;
    }

    /**
     * Expression parsing.
     */
    public ExpressionAST handleExpression() {
        // TODO re-use? (but re-create if errors occur)
        return new ExpressionParser(lexer).handleExpression();
    }
    
    /**
     * Handles "id := expr"
     */
    public AssignmentAST handleAssignment() {
        Token id = consumeToken(TokenType.IDENTIFIER, "Identifier should come first in an assignment");
        consumeToken(Value.ASSIGNMENT, "Missing the assignment symbol :=");
        return new AssignmentAST(id, handleExpression());
    }
    
    /**
     * Handles INSTRUCTION
     */
    public InstructionAST handleInstruction() {
        InstructionAST result;
        if(lexer.currentToken().getValue() == Value.IF) {
            result = handleConditional();
        } else if(lexer.currentToken().getValue() == Value.FROM) {
            result = handleIteration();
        } else if(lexer.currentToken().getValue() == Value.CONSTRUCTION) {
            result = handleConstruction();
        } else if(lexer.currentToken().getType() == TokenType.IDENTIFIER
               && lexer.peekToken().getValue() == Value.ASSIGNMENT) {
            result = handleAssignment();
        } else {
            result = handleExpression();
        }
        skipPotentialNewline();
        return result;
    }
    
    /**
     * Handles INSTRUCTIONS
     */
    public InstructionsAST handleInstructions() {
        if(lexer.currentToken().getValue() == Value.DO) {
            lexer.nextToken();
            skipPotentialNewline();
            List<VariableDeclAST> localDecls = handleLocalDeclarations();
            List<InstructionAST> instructions = new LinkedList<InstructionAST>();
            do {
                instructions.add(handleInstruction());
            } while(lexer.currentToken().getValue() != Value.END);
            lexer.nextToken(); // skip over end
            return new InstructionsAST(localDecls, instructions);
        }
        return null;
    }
    
    /**
     * Handles loops.
     */
    public IterationAST handleIteration() {
        consumeToken(Value.FROM);
        skipPotentialNewline();
        InstructionsAST from = handleInstructions();
        skipPotentialNewline();
        consumeToken(Value.UNTIL, "Until-condition missing in loop");
        skipPotentialNewline();
        ExpressionAST until = handleExpression();
        skipPotentialNewline();
        consumeToken(Value.LOOP, "Loop body missing");
        skipPotentialNewline();
        InstructionsAST loop = handleInstructions();
        skipPotentialNewline();
        consumeToken(Value.END, "Loop is missing end (check that you have 'end' for both 'loop' and 'do')");
        return new IterationAST(from, until, loop);
    }

    /**
     * Handles the else-clause (optional).
     */
    public InstructionsAST handleElse() {
        if(lexer.currentToken().getValue() == Value.ELSE) {
            consumeToken(Value.ELSE);
            skipPotentialNewline();
            return handleInstructions();
        }
        return null;
    }
    
    /**
     * Handles the elseif clause.
     */
    public IfStatementAST handleElseIfs() {
        consumeToken(Value.ELSEIF);
        skipPotentialNewline();
        ExpressionAST guard = handleExpression();
        skipPotentialNewline();
        consumeToken(Value.THEN, "Missing 'then' after 'elseif expr'");
        skipPotentialNewline();
        return new IfStatementAST(guard, handleInstructions());
    }
    
    /**
     * Handles if-elseif-else
     */
    public ConditionalAST handleConditional() {
        consumeToken(Value.IF);
        skipPotentialNewline();
        ExpressionAST guard = handleExpression();
        skipPotentialNewline();
        consumeToken(Value.THEN, "Missing 'then' after 'if expr'");
        skipPotentialNewline();
        // if ... then ...
        IfStatementAST ifStatement =
            new IfStatementAST(guard, handleInstructions());
        skipPotentialNewline();
        // elseifs
        List<IfStatementAST> elseIfs = new LinkedList<IfStatementAST>();
        while(lexer.currentToken().getValue() == Value.ELSEIF) {
            elseIfs.add(handleElseIfs());
            skipPotentialNewline();
        }
        // else (may be null)
        InstructionsAST elseStatement = handleElse();
        skipPotentialNewline();
        consumeToken(Value.END, "Missing 'end' after if-elseif-else conditional");
        return new ConditionalAST(ifStatement, elseIfs, elseStatement);
    }
    
    /**
     * Handles a method definition.
     */
    public MethodAST handleMethod() {
        Token id = consumeToken(TokenType.IDENTIFIER);
        List<ParamDeclAST> params = handleParams();
        Token returnType = handleReturnType();
        consumeToken(Value.IS, "Missing 'is' before method body");
        return handleMethod(id, params, returnType);
    }
    
    /**
     * Handles the rest of a method definition given the
     * identifier, params and return type (needed because
     * handleFeature() does some additional steps in recognizing
     * methods and might not recognize one until the "is" keyword).
     */
    protected MethodAST handleMethod(Token id, List<ParamDeclAST> params, Token returnType) {
        skipPotentialNewline();
        List<VariableDeclAST> localVariableDecls = handleLocalDeclarations();
        skipPotentialNewline();
        InstructionsAST instructions = handleInstructions();
        return new MethodAST(id, params, returnType, localVariableDecls, instructions);
    }
    
    /**
     * Handle (FEATURE)*
     */
    public FeatureBlockAST handleFeature() {
        consumeToken(
                Value.FEATURE,
                "Feature should start with the keyword 'feature'"
        );
        List<Token> visibilityList = handleVisibility();
        skipPotentialNewline();
        List<FeatureAST> features = new LinkedList<FeatureAST>();
        while(lexer.currentToken().getType() == TokenType.IDENTIFIER) {
            // to handle all the different forms of variable declarations
            // and method declarations, we need to peek ahead more than
            // a single token's worth.
            Token identifier = lexer.currentToken();
            Token peek = lexer.peekToken();
            if(peek.getValue() == Value.LEFT_PAREN || peek.getValue() == Value.IS) {
                // "id(" or "id is" ==> method
                features.add(handleMethod());
            } else if(peek.getValue() == Value.COMMA) {
                // "id," ==> variable
                features.addAll(handleVariableDecl(false));
            } else if(peek.getValue() == Value.COLON) {
                // "id:" ==> variable or method with return type
                lexer.nextToken();
                consumeToken(Value.COLON);
                Token type = consumeToken(
                        TokenType.IDENTIFIER,
                        "Expecting name of type"
                );
                if(lexer.currentToken().getValue() != Value.IS) {
                    // "id:retType" not followed by "is" ==> variable
                    features.add(
                            new VariableDeclAST(
                                    identifier,
                                    type,
                                    null
                            )
                    );
                } else {
                    // "id:retType is" ==> variable or method
                    Token next = lexer.nextToken();
                    if(TokenType.LITERAL.isCompatibleWith(next.getType())) {
                        // "id:retType is LITERAL" => variable
                        features.add(
                                new VariableDeclAST(
                                        identifier,
                                        type,
                                        consumeToken(TokenType.LITERAL)
                                )
                        );
                    } else {
                        // finally, assume method
                        List<ParamDeclAST> emptyParams = Collections.emptyList();
                        features.add(handleMethod(
                                identifier,
                                emptyParams,
                                type
                        ));
                    }
                }
            } else {
                throw new SyntaxException(
                        "Expecting a valid variable " +
                        "declaration or a method",
                        peek
                );
            }
            skipPotentialNewline();
        }
        return new FeatureBlockAST(visibilityList, features);
    }
    
    /**
     * Handles (FEATURES)*
     */
    public List<FeatureBlockAST> handleFeatures() {
        List<FeatureBlockAST> featureBlocks = new LinkedList<FeatureBlockAST>();
        while(lexer.currentToken().getValue() == Value.FEATURE) {
            featureBlocks.add(handleFeature());
        }
        return featureBlocks;
    }
    
    /**
     * Handles CLASSDEF
     */
    public ClassAST handleClassDef() {
        consumeToken(Value.CLASS, "'class' keyword missing");
        Token name = consumeToken(TokenType.IDENTIFIER, "Class name missing");
        skipPotentialNewline();
        ClassAST ast = new ClassAST(name, handleFeatures());
        skipPotentialNewline();
        consumeToken(Value.END);
        return ast;
    }
    
    /**
     * Handles a PROGRAM, i.e. is the starting point of regular parsing.
     */
    public ProgramAST handleProgram() {
        skipPotentialNewline();
        List<ClassAST> classes = new LinkedList<ClassAST>();
        while(lexer.currentToken().getType() != TokenType.EOF) {
            classes.add(handleClassDef());
            skipPotentialNewline();
        }
        return new ProgramAST(classes);
    } 

}
