package minieiffel.semantics;

import java.util.LinkedList;
import java.util.List;

import minieiffel.Token;
import minieiffel.Token.TokenType;
import minieiffel.Token.Value;
import minieiffel.ast.BinaryExpressionAST;
import minieiffel.ast.ExpressionAST;
import minieiffel.ast.ExpressionVisitor;
import minieiffel.ast.InvocationAST;
import minieiffel.ast.SimpleExpressionAST;
import minieiffel.ast.UnaryExpressionAST;

/**
 * Expression type inference class implemented as a visitor.
 * For example, <code>3.14*10</code> yields a result of type
 * <code>REAL</code>. Does all the necessary checks to prevent
 * invalid operations, e.g. the expression <code>true / 42</code>.
 */
public class TypeInference implements ExpressionVisitor {
    
    /** the analyzer instance in control of the analysis */
    private SemanticAnalyzer analyzer;

    public TypeInference(SemanticAnalyzer analyzer) {
        this.analyzer = analyzer;
    }
    
    /**
     * Infers the type of the given expression.
     */
    public Type inferType(ExpressionAST expr) {
        expr.accept(this);
        return expr.getType();
    }
    
    /**
     * Visits and resolves the type of a {@link SimpleExpressionAST}:
     * for literal values this is the corresponding pre-defined
     * {@link Type} (INTEGER, REAL etc) and for identifiers (in this
     * case, variables) it is the variable's type.
     */
    public void visit(SimpleExpressionAST expr) {
        switch(expr.getLocationToken().getType()) {
        case INT_LITERAL:
            expr.setType(Type.INTEGER); break;
        case REAL_LITERAL:
            expr.setType(Type.REAL); break;
        case CHAR_LITERAL:
            expr.setType(Type.CHARACTER); break;
        case BOOLEAN_LITERAL:
            expr.setType(Type.BOOLEAN); break;
        case IDENTIFIER:
            expr.setType(
                    analyzer.resolveVariableType(
                            null,
                            expr.getLocationToken()
                    )
            );
            break;
        default:
            throw new RuntimeException("Not implemented: " + expr.getLocationToken().getType());
        }
    }

    /**
     * Visits and resolves the type of a unary expression.
     * The expression enclosed in the unary operator is
     * visited first and then the type of the whole
     * expression is determined.
     */
    public void visit(UnaryExpressionAST expr) {
        expr.getExpression().accept(this);
        Token op = expr.getOperator();
        Type t = expr.getExpression().getType();
        if(t == null) {
            // semantic errors have already occured, don't try to analyze
            return;
        }
        if(op.getValue() == Value.MINUS) {
            if(t == Type.INTEGER || t == Type.CHARACTER ) {
                expr.setType(Type.INTEGER);
            } else if(t == Type.REAL) {
                expr.setType(Type.REAL);
            } else {
                analyzer.addError("Operation '-' not defined for " + t, op);
            }
        } else if(op.getValue() == Value.NOT) {
            if(t == Type.BOOLEAN) {
                expr.setType(Type.BOOLEAN);
            } else {
                analyzer.addError("Operation 'not' not defined for " + t, op);
            }
        } else {
            throw new RuntimeException("Unknown unary operator: " + op);
        }
    }

    /**
     * Visits and resolves the type of a binary (two operand)
     * expression. Both the left-hand and right-hand side
     * expressions are resolved first and then the type
     * of the whole expression is resolved.
     */
    public void visit(BinaryExpressionAST expr) {
        
        // types of lhs and rhs expressions
        Type a, b;
        
        Value op = expr.getOperator().getValue();
        
        expr.getLhs().accept(this);
        a = expr.getLhs().getType();
        
        // handle the case of user-defined LHS exprs here
        if(a != null && !a.isPrimitive()) {
            // user-defined type, the only legal operation is "."
            if(op == Value.DOT) {
                // rhs must either be an identifier or a invocation
                // (i.e. lhs.id or lhs.someMethod())
                boolean reportError = false;
                if (expr.getRhs() instanceof SimpleExpressionAST) {
                    SimpleExpressionAST rhs = (SimpleExpressionAST)expr.getRhs();
                    if(rhs.getLocationToken().getType() == TokenType.IDENTIFIER) {
                        expr.setType(
                                analyzer.resolveVariableType(
                                        a,
                                        rhs.getLocationToken()
                                )
                        );
                    } else {
                        reportError = true;
                    }
                } else if(expr.getRhs() instanceof InvocationAST) {
                    InvocationAST rhs = (InvocationAST)expr.getRhs();
                    expr.setType( resolveInvocationType(rhs, a) );
                } else {
                    reportError = true;
                }
                if(reportError) {
                    analyzer.addError(
                            "The right-hand side value of the operator " +
                            "'.' must be an identifier or a method call" +
                            " (e.g. \"a.someVar\" or \"a.someMethod()\")",
                            expr.getOperator()
                    );
                }
                return;
            } else if(op == Value.EQUALITY || op == Value.INEQUALITY) {
                // allow '=' and '/=' on all types
            } else {
                analyzer.addError(
                        "The operator '" + expr.getOperator().getText() +
                        "' can't be used on the user-defined type " +
                        a.getName(),
                        expr.getOperator()
                );
                return;
            }
        }

        // check whether '.' is being used on a primitive value
        if(a != null && a.isPrimitive() && op == Value.DOT) {
            analyzer.addError(
                    "The operator '.' is not allowed on primitive values",
                    expr.getOperator()
            );
            return;
        }

        // investigate rhs
        expr.getRhs().accept(this);
        b = expr.getRhs().getType();
        
        if(a == null || b == null) {
            // Semantic error determining type of lhs/rhs expressions,
            // no need to go further. This null check is done here
            // (instead of doing it for 'a' earlier) so that we can
            // report semantic errors on both lhs and rhs branches
            // at the same time
            return;
        }
        
        if(op == Value.EQUALITY || op == Value.INEQUALITY) {
            // allow equality and inequality for all types
            expr.setType(Type.BOOLEAN);
        } else if(b.isPrimitive()) {
            // promote character to INTEGER for all the operations
            // TODO is this the correct behaviour?
            if(a == Type.CHARACTER) {
                a = Type.INTEGER;
            }
            if(b == Type.CHARACTER) {
                b = Type.INTEGER;
            }
            // make sure we only get a pair of types in a single order,
            // i.e. always INTEGER,REAL never REAL,INTEGER
            if(a.compareTo(b) > 0) {
                Type tmp = b;
                b = a;
                a = tmp;
            }
            if(op == Value.PLUS || op == Value.MINUS ||
                    op == Value.MULTIPLY || op == Value.DIVIDE) {
                if(a == Type.INTEGER) {
                    if(b == Type.INTEGER) {
                        expr.setType(Type.INTEGER);
                    } else if(b == Type.REAL) {
                        expr.setType(Type.REAL);
                    }
                } else if(a == Type.REAL && b == Type.REAL) {
                    expr.setType(Type.REAL);
                }
            } else if(op == Value.REMAINDER) {
                // TODO remainder only defined for integers?
                if(a == Type.INTEGER && b == Type.INTEGER) {
                    expr.setType(Type.INTEGER);
                }
            } else if(op == Value.GREATER || op == Value.GREATER_OR_EQUAL
                    || op == Value.LESS || op == Value.LESS_OR_EQUAL) {
                if( (a == Type.INTEGER || a == Type.REAL) &&
                    (b == Type.INTEGER || b == Type.REAL)) {
                    expr.setType(Type.BOOLEAN);
                }
            } else if(op == Value.AND || op == Value.AND_THEN ||
                      op == Value.OR || op == Value.XOR || op == Value.OR_ELSE) {
                if(a == Type.BOOLEAN && b == Type.BOOLEAN) {
                    expr.setType(Type.BOOLEAN);
                }
            }
        }
        if(expr.getType() == null) {
            analyzer.addError(
                    "Operation '" + op + "' not defined for " +
                    expr.getLhs().getType() + ", " + expr.getRhs().getType(),
                    expr.getOperator()
            );
        }
    }

    /**
     * Visits and resolves the (return) type of an invocation.
     */
    public void visit(InvocationAST invocation) {
        invocation.setType( resolveInvocationType(invocation, null) );
    }
    
    /**
     * Resolves (and sets) the type of an invocation AST node.
     * 
     * @param invocation the ast node
     * @param parent type in context of which the invocation is made (e.g. "a" in "a.b()")
     */
    protected Type resolveInvocationType(InvocationAST invocation, Type parent) {
        List<Type> paramTypes = new LinkedList<Type>();
        for (ExpressionAST param : invocation.getArguments()) {
            param.accept(this);
            paramTypes.add(param.getType());
        }
        return analyzer.resolveMethodType(
                parent,
                invocation.getIdentifier(),
                paramTypes
        );
    }
        
}
