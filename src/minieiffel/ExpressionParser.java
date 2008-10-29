package minieiffel;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import minieiffel.Token.TokenType;
import minieiffel.Token.Value;
import minieiffel.ast.BinaryExpressionAST;
import minieiffel.ast.ExpressionAST;
import minieiffel.ast.InvocationAST;
import minieiffel.ast.SimpleExpressionAST;
import minieiffel.ast.UnaryExpressionAST;
import minieiffel.util.Stack;

/**
 * A specialized Shift-Reduce parser for expressions. Modelled after the
 * <a href="http://www.cs.rpi.edu/~gregod/Sugar/doc/design.html">Sugar</a>
 * expression parser.
 * 
 * <p>A single ExpressionParser instance can be re-used for parsing many
 * expressions but if syntax errors occur the instance might be left
 * in an incoherent state and should therefore not be used again.</p>
 */
public class ExpressionParser {

    /** token source */
    private final Lexer lexer;

    /** stack of operands */
    private final Stack<ExpressionAST> operandStack = new Stack<ExpressionAST>();
    
    /** stack of operators */
    private final Stack<Operator> operatorStack = new Stack<Operator>();
    
    /** set when parsing a sub-expression (i.e. method arguments) */
    private final boolean parsingSubExpression;

    /** keeps track of the state of this parser */
    private ParserState state;

    /** trivial way of keeping track of the balance of parenthesis */
    private int parenBalance = 0;
    
    /**
     * Creates an expression parser for the given lexer.
     */
    public ExpressionParser(Lexer lexer) {
        this(lexer, false);
    }

    /**
     * Private constructor.
     */
    private ExpressionParser(Lexer lexer, boolean parsingSubExpression) {
        this.lexer = lexer;
        this.parsingSubExpression = parsingSubExpression;
        if(lexer.currentToken() == null) {
            lexer.nextToken();
        }
    }
    
    /**
     * Parses an expression into an abstract syntax node.
     */
    public ExpressionAST handleExpression() {
        state = new ParserState();
        while(lexer.currentToken().getType() != TokenType.EOF) {
            // if set to false, don't read new token at the end of the loop
            // as the previous one hasn't been consumed yet
            boolean advance = true;
            if(TokenType.LITERAL.isCompatibleWith(lexer.currentToken().getType())) {
                // push to operand stack
                handleOperand(new SimpleExpressionAST(lexer.currentToken()));
            } else if(lexer.currentToken().getType() == TokenType.IDENTIFIER) {
                // push operand in either case (might be a single identifier
                // but also the start of a function call)
                handleOperand(new SimpleExpressionAST(lexer.currentToken()));
                lexer.nextToken();
                if(lexer.currentToken().getValue() == Value.LEFT_PAREN) {
                    // function invocation
                    handleInvocation();
                } else {
                    // just an identifier, don't consume current token
                    advance = false;
                }
            } else if(lexer.currentToken().getValue() == Value.LEFT_PAREN) {
                handleOperator(new Operator(OperatorType.CONFIX_OPEN, lexer.currentToken()));
            } else if(lexer.currentToken().getValue() == Value.RIGHT_PAREN) {
                if(parenBalance == 0) {
                    if(!parsingSubExpression) {
                        // top-level, unmatched closing paren
                        throw new SyntaxException("Unmatched parenthesis", lexer.currentToken());
                    } else {
                        // sub-expression inside method arguments
                        break;
                    }
                }
                handleOperator(new Operator(OperatorType.CONFIX_FUNCTION_CLOSE, lexer.currentToken()));
            } else if(lexer.currentToken().getType() == TokenType.OPERATOR) {
                if(lexer.currentToken().getValue() == Value.NOT) {
                    // NOT is always unary
                    handleOperator(new Operator(OperatorType.PREFIX, lexer.currentToken()));
                } else if(lexer.currentToken().getValue() == Value.MINUS) {
                    if(state.prefixAllowed()) {
                        // lexer doesn't (and can't) differentiate between
                        // unary and binary minus signs, we have to do it here
                        lexer.currentToken().setValue(Value.UNARY_MINUS);
                        handleOperator(new Operator(OperatorType.PREFIX, lexer.currentToken()));
                    } else {
                        handleOperator(new Operator(OperatorType.INFIX, lexer.currentToken()));
                    }
                } else {
                    handleOperator(new Operator(OperatorType.INFIX, lexer.currentToken()));
                }
            } else {
                // some other token: consider this expression ended
                break;
            }
            if(advance) {
                lexer.nextToken();
            }
        }
        // exited loop, at end of expression
        if(parenBalance != 0) {
            throw new SyntaxException("Unmatched parenthesis", lexer.currentToken());
        }
        if(operandStack.isEmpty()) {
            throw new SyntaxException("Empty expression", lexer.currentToken());
        }
        while(!operatorStack.isEmpty()) {
            // at this point, the action is always reduce
            reduce(null);
        }
        assert operandStack.size() == 1 : "Operand stack should have only one item";
        assert operatorStack.isEmpty() : "Operator stack should be empty";
        // the resulting expression is at the top of the stack
        return operandStack.pop();
    }
    
    /* private implementation follows */
    
    /**
     * Handles an operator found in the token stream:
     * <ul>
     *  <li>if the operator stack is empty,
     *      operator is pushed to the stack immediately</li>
     *  <li>otherwise the current token and the one at
     *      the top of the stack are examined and the
     *      corresponding action is taken (shift, reduce etc)</li>
     * </ul>
     */
    private void handleOperator(Operator op) {
        // ensure current operator is valid in this context
        state.handleOperator(op);
        if(operatorStack.isEmpty()) {
            operatorStack.push(op);
        } else {
            Operator top = operatorStack.peek();
            Action action = Action.MAPPINGS[top.type.ordinal()][op.type.ordinal()];
            switch(action) {
                case SHIFT:
                    shift(op); break;
                case REDUCE:
                    reduce(op); break;
                case PRECEDENCE:
                    precedence(op); break;
                case PRECEDENCE_ASSOCIATIVITY:
                    precedenceAssociativity(op); break;
                default: throw new UnsupportedOperationException("No such parser action: " + action);
            }
        }
        // keep track of balance of parenthesis
        switch(op.type) {
          case CONFIX_OPEN:
          case FUNCTION_OPEN:
              parenBalance++;
              break;
          case CONFIX_FUNCTION_CLOSE:
              parenBalance--;
              break;
        }
    }

    /**
     * Handles an operand: operands are always pushed to
     * the operand stack immediately.
     */
    private void handleOperand(SimpleExpressionAST expr) {
        // ensure operands are allowed in the current context
        state.handleOperand();
        operandStack.push(expr);
    }

    /**
     * Handles a function invocation.
     */
    private void handleInvocation() {
        List<ExpressionAST> arguments = handleArguments();
        Token functionName = ((SimpleExpressionAST)operandStack.pop()).getLocationToken();
        operandStack.push(new InvocationAST(functionName, arguments));
    }
    
    /**
     * Handles a comma-separated list of function arguments
     * (each of which is an expression)
     */
    private List<ExpressionAST> handleArguments() {
        lexer.nextToken();
        if(lexer.currentToken().getValue() == Value.RIGHT_PAREN) {
            // empty arguments
            return Collections.emptyList();
        } else {
            LinkedList<ExpressionAST> arguments = new LinkedList<ExpressionAST>();
            // parse each of the arguments as its own subexpression
            ExpressionParser argumentParser = new ExpressionParser(lexer, true);
            while(true) {
                ExpressionAST argument = argumentParser.handleExpression();
                arguments.add(argument);
                if(lexer.currentToken().getValue() == Value.RIGHT_PAREN) {
                    // end of arguments
                    break;
                } else if(lexer.currentToken().getValue() == Value.COMMA) {
                    // comma, read next argument
                    lexer.nextToken();
                } else {
                    throw new SyntaxException(
                            "Expecting closing parenthesis or comma after arguments",
                            lexer.currentToken()
                    );
                }
            }
            return arguments;
        }
    }
    
    /**
     * Parser action REDUCE: depending on the operator on
     * the top of the operator stack, pops operands off
     * the operand stack and and applies the operator to them
     * while pushing the result back on the operand stack.
     * Current input operator is pushed to the top of the
     * operator stack (unless its null, i.e. we're at the
     * end of the expression).
     */
    private void reduce(Operator current) {
        Operator top = operatorStack.pop();
        if(top.type == OperatorType.CONFIX_FUNCTION_CLOSE) {
            Operator open = operatorStack.pop();
            if(open.type != OperatorType.CONFIX_OPEN) {
                reduceOperators(open);
            } else {
                if(!operatorStack.isEmpty() &&
                        operatorStack.peek().type != OperatorType.CONFIX_OPEN) {
                    // reduce if operator stack not empty, but don't reduce
                    // two consecutive opening parenthesis
                    reduceOperators(operatorStack.pop());
                }
            }
        } else if(top.type == OperatorType.CONFIX_OPEN) {
            // do nothing
        } else {
            reduceOperators(top);
        }
        if(current != null) {
            operatorStack.push(current);
        }
    }

    /**
     * Reduces an infix/prefix operator.
     */
    private void reduceOperators(Operator op) {
        if(op.type == OperatorType.INFIX) {
            reduceInfixOperator(op);
        } else if(op.type == OperatorType.PREFIX) {
            reducePrefixOperator(op);
        } else {
            throw new RuntimeException("Not implemented: " + op.type);
        }
    }

    /**
     * Reduction of an infix (binary) operator.
     */
    private void reduceInfixOperator(Operator op) {
        if(operandStack.size() < 2) {
            throw new SyntaxException(
                    "Infix operator " + op.token.getText() +
                    " requires two parameters",
                    op.token
            );
        }
        ExpressionAST rhs = operandStack.pop();
        ExpressionAST lhs = operandStack.pop();
        operandStack.push(new BinaryExpressionAST(lhs, op.token, rhs));
    }

    /**
     * Reduction of an prefix (unary) operator.
     */
    private void reducePrefixOperator(Operator op) {
        ExpressionAST expr = operandStack.pop();
        if(op.token.getValue() == Value.UNARY_MINUS) {
            // UNARY_MINUS is only set temporarily to handle
            // operator precedence, set value back to MINUS
            op.token.setValue(Value.MINUS);
        }
        operandStack.push(new UnaryExpressionAST(op.token, expr));
    }

    /**
     * Parser action SHIFT: simply pushes the current operator
     * to the top of the operator stack.
     */
    private void shift(Operator current) {
        operatorStack.push(current);
    }
    
    /**
     * Parser action PRECEDENCE:
     * <ul>
     *   <li>if the operator on the top of the stack has
     *       higher precedence than the current input
     *       operator, REDUCE</li>
     *   <li>otherwise SHIFT</li>
     * </ul>
     */
    private void precedence(Operator current) {
        if(operatorStack.peek().compareTo(current) > 0) {
            // operator on top of stack has higher precedence than
            // the current input token, reduce
            reduce(current);
        } else {
            shift(current);
        }
    }

    /**
     * Parser action PRECEDENCE_ASSOCIATIVITY:
     * Precedence is first compared according to the precedence operation,
     * if the precedences are equivalent, associativity is considered:
     * <ul>
     *  <li>If top associates left of current, REDUCE.</li>
     *  <li>If top associates right of current, SHIFT.</li>
     * </ul>
     * Note: all operators are actually left-associative, so if the
     *       precedences are equivalent, REDUCE is always executed
     */
    private void precedenceAssociativity(Operator current) {
        int precedence = operatorStack.peek().compareTo(current);
        if(precedence == 0) {
            reduce(current);
        } else {
            if(precedence > 0) {
                reduce(current);
            } else {
                shift(current);
            }
        }
    }

    /**
     * An enumeration presenting the different parser actions.
     */
    private enum Action {

        SHIFT,
        REDUCE,
        PRECEDENCE,
        PRECEDENCE_ASSOCIATIVITY;
        
        /**
         * Maps pairs of operator types to the corresponding parser action:
         * the first dimension is the type of the operator on top of the
         * operator stack and the second is that of the current input operator.
         */
        private static final Action[][] MAPPINGS = {
            { SHIFT, PRECEDENCE, SHIFT, REDUCE, PRECEDENCE },
            { SHIFT, PRECEDENCE_ASSOCIATIVITY, SHIFT, REDUCE, PRECEDENCE },
            { SHIFT, SHIFT, SHIFT, SHIFT, SHIFT },
            { REDUCE, REDUCE, REDUCE, REDUCE, REDUCE },
            { SHIFT, SHIFT, SHIFT, SHIFT, SHIFT }
        };
        
    }

    /**
     * Operator types from the viewpoint of this parser implementation.
     */
    private enum OperatorType {
        PREFIX,
        INFIX,
        CONFIX_OPEN,
        CONFIX_FUNCTION_CLOSE,
        FUNCTION_OPEN
    }
    
    /**
     * Represents an operator in the meaning used in this parser:
     * in addition to the standard MiniEiffel operators,
     * the algorithms in this parser consider parenthesis
     * to be operators, for example.
     */
    private static class Operator implements Comparable<Operator> {
        
        private OperatorType type;
        private Token token;
        
        private Operator(OperatorType type, Token token) {
            this.type = type;
            this.token = token;
        }

        public String toString() {
            return "Operator { " + type + "/" + token.getText() + " }";
        }

        public int compareTo(Operator o) {
            return this.token.getValue().getPrecedence() -
                o.token.getValue().getPrecedence();
        }
        
    }

    /**
     * Valid parser states.
     */
    private enum State { PRE, POST }
    
    /**
     * A simple state machine that's used to validate expressions.
     * For example: an expression can't start with an infix operator.
     */
    private final class ParserState {
        
        private State currentState = State.PRE;
        
        private void handleOperator(Operator op) {
            if(currentState == State.PRE) {
                if(op.type == OperatorType.PREFIX ||
                        op.type == OperatorType.CONFIX_OPEN) {
                    // stay
                } else {
                    throw new SyntaxException(
                        "Operator " + op.token.getText() + " not allowed here",
                        lexer.currentToken()
                    );
                }
            } else if(currentState == State.POST) {
                if(op.type == OperatorType.CONFIX_FUNCTION_CLOSE ||
                        op.type == OperatorType.FUNCTION_OPEN) {
                    // stay
                } else if(op.type == OperatorType.INFIX) {
                    currentState = State.PRE;
                } else {
                    throw new SyntaxException(
                            "Operator " + op.token.getText() + " not allowed here",
                            lexer.currentToken()
                    );
                }
            }
        }
        
        private void handleOperand() {
            if(currentState == State.PRE) {
                currentState = State.POST;
            } else if(currentState == State.POST) {
                throw new SyntaxException(
                        "Operand not allowed here",
                        lexer.currentToken()
                );
            }
        }
        
        /**
         * Are prefix operators allowed in the current state?
         */
        private boolean prefixAllowed() {
            return currentState == State.PRE;
        }
        
    }
    
}
