package expressionsolver;

import jadd.ADD;
import jadd.JADD;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.nfunk.jep.JEP;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.type.DoubleNumberFactory;

import expressionsolver.functions.ADDAdd;
import expressionsolver.functions.ADDDivide;
import expressionsolver.functions.ADDMultiply;
import expressionsolver.functions.ADDPower;
import expressionsolver.functions.ADDSubtract;
import expressionsolver.functions.LogicalAnd;
import expressionsolver.functions.LogicalNot;
import expressionsolver.functions.LogicalOr;
import expressionsolver.functions.UnaryMinus;

/**
 * @author thiago
 *
 */
public class ExpressionSolver {
    private static final Logger LOGGER = Logger.getLogger(ExpressionSolver.class.getName());

    private JADD jadd;

    /**
     * Solves expressions using the provided ADD manager.
     */
    public ExpressionSolver(JADD jadd) {
        this.jadd = jadd;
    }

    /**
     * Solves an expression with respect to the given interpretation of
     * variables. Here, variables are interpreted in the algebraic sense, not as
     * boolean ADD-variables.
     *
     * @param expression
     * @param interpretation
     *            A map from variable names to the respective values to be
     *            considered during evaluation.
     * @return a (possibly constant) function (ADD) representing all possible
     *         results according to the ADDs involved.
     */
    public ADD solveExpressionAsFunction(String expression, Map<String, ADD> interpretation) {
        Expression<ADD> parsedExpression = parseExpressionForFunctions(expression);
        if (parsedExpression == null) {
            return null;
        }
        return parsedExpression.solve(interpretation);
    }

    /**
     * Useful shortcut for expressions with no variables involved.
     *
     * @param expression
     * @return
     */
    public ADD solveExpressionAsFunction(String expression) {
        return solveExpressionAsFunction(expression, new HashMap<String, ADD>());
    }

    /**
     * Solves an expression with respect to the given interpretation of
     * variables. Here, variables are interpreted in the algebraic sense.
     *
     * @param expression
     * @param interpretation
     *            A map from variable names to the respective values to be
     *            considered during evaluation.
     * @return a floating-point result for the evaluated expression.
     */
    public Double solveExpression(String expression, Map<String, Double> interpretation) {
        Expression<Double> parsedExpression = parseExpression(expression);
        if (parsedExpression == null) {
            return null;
        }
        return parsedExpression.solve(interpretation);
    }

    /**
     * Useful shortcut for expressions with no variables involved.
     *
     * @param expression
     * @return
     */
    public Double solveExpression(String expression) {
        return solveExpression(expression, new HashMap<String, Double>());
    }

    /**
     * Encodes a propositional logic formula as a 0,1-ADD, which is roughly
     * equivalent to a BDD, but better suited to representing boolean functions
     * which interact with Real ADDs.
     *
     * If there are variables in the formula, they are interpreted as ADD
     * variables. These are internally indexed by name, so that multiple
     * references in (possibly multiple) expressions are taken to be the same.
     *
     * @param formula
     *            Propositional logic formula to be encoded. The valid boolean
     *            operators are && (AND), || (OR) and !(NOT).
     * @return
     */
    public ADD encodeFormula(String formula) {
        JEP parser = makeADDParser(jadd);
        parser.parseExpression(formula);
        if (parser.hasError()) {
            LOGGER.warning("Parser error: " + parser.getErrorInfo());
            return null;
        }

        parser.addVariableAsObject("true", jadd.makeConstant(1));
        parser.addVariableAsObject("True", jadd.makeConstant(1));
        parser.addVariableAsObject("false", jadd.makeConstant(0));
        parser.addVariableAsObject("False", jadd.makeConstant(0));
        SymbolTable symbolTable = parser.getSymbolTable();
        @SuppressWarnings("unchecked")
        Set<String> variables = new HashSet<String>(symbolTable.keySet());
        variables.remove("true");
        variables.remove("True");
        variables.remove("false");
        variables.remove("False");

        for (Object var : variables) {
            String varName = (String) var;
            ADD variable = jadd.getVariable(varName);
            parser.addVariableAsObject(varName, variable);
        }
        return (ADD) parser.getValueAsObject();
    }

    /**
     * Lower level alternative for {@link solveExpression(String)}.
     *
     * It returns a handle to an already parsed expression, in case it
     * must be evaluated more than once.
     *
     * @param expression
     * @return A handle to the parsed expression or {@code null} if there
     *      is a parsing error.
     */
    public Expression<Double> parseExpression(String expression) {
        JEP parser = makeFloatingPointParser();
        parser.parseExpression(expression);
        if (parser.hasError()) {
            LOGGER.warning("Parser error: " + parser.getErrorInfo());
            return null;
        }
        return new Expression<Double>(parser, Double.class);
    }

    /**
     * Lower level alternative for {@link solveExpressionAsFunction(String)}.
     *
     * @see {@link parseExpression(String)}
     *
     * @param expression
     * @return A handle to the parsed expression or {@code null} if there
     *      is a parsing error.
     */
    public Expression<ADD> parseExpressionForFunctions(String expression) {
        JEP parser = makeADDParser(jadd);
        parser.parseExpression(expression);
        if (parser.hasError()) {
            LOGGER.warning("Parser error: " + parser.getErrorInfo());
            return null;
        }
        return new Expression<ADD>(parser, ADD.class);
    }

    /**
     * @param jadd
     */
    private JEP makeADDParser(JADD jadd) {
        JEP parser = new JEP(false, true, true, new ADDNumberFactory(jadd));
        parser = addMathematicalFunctions(parser);
        parser = addLogicalFunctions(parser);
        
        return parser;
    }

	/**
	 * @param parser
	 */
	private JEP addMathematicalFunctions(JEP parser) {
        parser.addFunction("\"+\"", new ADDAdd());
        parser.addFunction("\"-\":2", new ADDSubtract());
        parser.addFunction("\"-\":1", new UnaryMinus());
        parser.addFunction("\"*\"", new ADDMultiply());
        parser.addFunction("\"^\"", new ADDPower(jadd));
        parser.addFunction("\"/\"", new ADDDivide());

    	return parser;
    }
	
	/**
	 * @param parser
	 */	
	private JEP addLogicalFunctions(JEP parser) {
		parser.addFunction("\"&&\"", new LogicalAnd());
		parser.addFunction("\"||\"", new LogicalOr());
		parser.addFunction("\"!\"", new LogicalNot());
		
		return parser;
	}

    /**
     * Makes a standard floating-point-based parser.
     */
    private JEP makeFloatingPointParser() {
        JEP parser = new JEP(false, true, true, new DoubleNumberFactory());
        parser.setAllowUndeclared(true);
        return parser;
    }

}
