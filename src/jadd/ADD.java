package jadd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bridj.Pointer;

import tool.UnknownFeatureException;
import bigcudd.BigcuddLibrary;
import bigcudd.BigcuddLibrary.Cudd_addApply_arg1_callback;
import bigcudd.BigcuddLibrary.DdGen;
import bigcudd.BigcuddLibrary.DdManager;
import bigcudd.DdNode;

/**
 * ADD - constant, variable or function alike.
 * @author thiago
 *
 */
public class ADD {
    private static double FLOATING_POINT_PRECISION = 1E-14;

    private Pointer<DdNode> function;
    private Pointer<DdManager> dd;
    private VariableStore variableStore;

    ADD(Pointer<DdManager> dd, Pointer<DdNode> function, VariableStore variableStore) {
        this.dd = dd;
        this.function = function;
        this.variableStore = variableStore;
        BigcuddLibrary.Cudd_Ref(this.function);
    }

    /**
     * Overriding finalize in order to free CUDD allocated memory.
     */
    @Override
    protected void finalize() throws Throwable {
//        BigcuddLibrary.Cudd_Deref(function);
        super.finalize();
    }

    public ADD plus(ADD other) {
        return apply(other, PLUS);
    }

    public ADD minus(ADD other) {
        return apply(other, MINUS);
    }

    public ADD times(ADD other) {
        return apply(other, TIMES);
    }

    public ADD dividedBy(ADD other) {
        return apply(other, DIVIDE);
    }

    public ADD and(ADD other) {
        return apply(other, TIMES);
    }

    public ADD or(ADD other) {
        return apply(other, LOGICAL_OR);
    }

    private ADD apply(ADD other, Cudd_addApply_arg1_callback operation) {
        Pointer<DdNode> result = BigcuddLibrary.Cudd_addApply(dd,
                                                              Pointer.getPointer(operation),
                                                              this.function,
                                                              other.function);
        return new ADD(dd, result, variableStore);
    }

    /**
     * @return negated form (corresponding to unary minus).
     */
    public ADD negate() {
        return new ADD(dd,
                       BigcuddLibrary.Cudd_addNegate(dd, this.function),
                       variableStore);
    }

    /**
     * @return complemented form (corresponding to logical not).
     */
    public ADD complement() {
        return new ADD(dd,
                       BigcuddLibrary.Cudd_addCmpl(dd, this.function),
                       variableStore);
    }

    /**
     * Implements if-then-else with the result of this boolean function
     * as the conditional.
     */
    public ADD ifThenElse(ADD ifTrue, ADD ifFalse) {
        Pointer<DdNode> result = BigcuddLibrary.Cudd_addIte(dd,
                                                            this.function,
                                                            ifTrue.function,
                                                            ifFalse.function);
        return new ADD(dd, result, variableStore);
    }

    /**
     * Overloading for constant fallbacks.
     */
    public ADD ifThenElse(ADD ifTrue, double ifFalse) {
        Pointer<DdNode> result = BigcuddLibrary.Cudd_addIte(dd,
                                                            this.function,
                                                            ifTrue.function,
                                                            BigcuddLibrary.Cudd_addConst(dd,
                                                                                         ifFalse));
        return new ADD(dd, result, variableStore);
    }

    /**
     * Implements if-then-else with {@code condition} as the conditional.
     */
    public static ADD ite(ADD condition, ADD ifTrue, ADD ifFalse) {
        return condition.ifThenElse(ifTrue, ifFalse);
    }

    public Set<String> getVariables() {
        Set<String> variables = new HashSet<String>();

        Pointer<Integer> variablesPtr = BigcuddLibrary.Cudd_SupportIndex(dd, this.function);
        int numVars = BigcuddLibrary.Cudd_ReadSize(dd);
        int[] variablesPresence = variablesPtr.getInts(numVars);
        for (short i = 0; i < numVars; i++) {
            if (variablesPresence[i] == 1) {
                variables.add(variableStore.getName(i));
            }
        }
        return variables;
    }

    public List<String> getVariableOrder() {
        List<String> variables = new ArrayList<String>();
        for (int pos = 0; pos < variableStore.getNumberOfVariables(); pos++) {
            int varIndex = BigcuddLibrary.Cudd_ReadInvPerm(dd, pos);
            String varName = variableStore.getName((short)varIndex);
            variables.add(varName);
        }
        return variables;
    }

    public double eval(String[] variables) throws UnrecognizedVariableException {
        int[] presenceVector = variableStore.toPresenceVector(variables);
        Pointer<DdNode> terminal = BigcuddLibrary.Cudd_Eval(dd,
                                                            function,
                                                            Pointer.pointerToInts(presenceVector));
        DdNode terminalNode = terminal.get();
        return terminalNode.type().value();
    }

    public double eval(List<String> variables) throws UnrecognizedVariableException {
        return eval(variables.toArray(new String[variables.size()]));
    }

    /**
     * Checks if a configuration is valid (non-zero).
     * @param configuration
     * @return
     * @throws UnknownFeatureException
     */
    public boolean isValidConfiguration(Collection<String> configuration) throws UnknownFeatureException {
        double validity;
        try {
            validity = eval(configuration.toArray(new String[configuration.size()]));
        } catch (UnrecognizedVariableException e) {
            throw new UnknownFeatureException(e.getVariableName());
        }
        return Double.doubleToRawLongBits(validity) != 0;
    }

    /**
     * Returns a stream of valid (non-zero) configurations for this ADD, expanding
     * "don't care" variables into possible concrete configurations.
     *
     * For instance, the configuration ["A", "(B)", "C"] would be returned as
     * two different configurations: ["A", "B", "C"] and ["A", "C"].
     * @return
     */
    public Stream<Collection<String>> getExpandedConfigurations() {
        return StreamSupport.stream(new CubeSpliterator(), true);
    }

    /**
    * Returns the number of internal nodes in this ADD.
    * @return
    */
    public int getNodeCount() {
        return BigcuddLibrary.Cudd_DagSize(function);
    }

    /**
     * Returns true if this ADD is a constant one.
     * @return
     */
    public boolean isConstant() {
        return 1 != BigcuddLibrary.Cudd_IsNonConstant(function);
    }

    static Collection<List<String>> expandDontCares(List<String> config) {
        return expandDontCares(config.iterator());
    }

    /**
     * Iterates {@code cursor} expanding "don't care" variables, i.e.,
     * doubling the configurations for each one encountered.
     *
     * It expands "don't care" variables in linear time.
     * @param cursor
     * @return
     */
    static Collection<List<String>> expandDontCares(Iterator<String> cursor) {
        Set<List<String>> expanded = new HashSet<List<String>>();
        List<String> prefix = new LinkedList<String>();
        while (cursor.hasNext()) {
            String variable = cursor.next();
            if (variable.startsWith("(")) {
                // We must generate two alternative prefixes: one with the
                // variable in positive form and another with it in negative
                // form (i.e., omitted).
                List<String> complementedPrefix = new LinkedList<String>(prefix);
                String deparenthesized = variable.substring(1, variable.length()-1);
                prefix.add(deparenthesized);
                // Then we must expand the rest of the configuration and append
                // each of the expanded sub-configurations to the alternative prefixes.
                Collection<List<String>> expandedTail = expandDontCares(cursor);
                for (List<String> expandedSubconfig : expandedTail) {
                    List<String> positive = new LinkedList<String>(prefix);
                    List<String> complemented = new LinkedList<String>(complementedPrefix);
                    positive.addAll(expandedSubconfig);
                    complemented.addAll(expandedSubconfig);
                    expanded.add(positive);
                    expanded.add(complemented);
                }
                return expanded;
            } else {
                prefix.add(variable);
            }
        }
        expanded.add(prefix);
        return expanded;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        ADD other = (ADD) obj;
        return this.function.equals(other.function)
                || (BigcuddLibrary.Cudd_EqualSupNorm(dd,
                                                     this.function,
                                                     other.function,
                                                     ADD.FLOATING_POINT_PRECISION,
                                                     1) == 1);
    }

    public int getDeadNodesCount() {
    	return BigcuddLibrary.Cudd_ReadDead(dd);
    }

    public int getTerminalsDifferentThanZeroCount() {
    	return BigcuddLibrary.Cudd_CountLeaves(function) - 1;
    }

    public double getPathsToNonZeroTerminalsCount() {
    	return BigcuddLibrary.Cudd_CountPathsToNonZero(function);
    }

    public double getPathsToZeroTerminalCount() {
    	return BigcuddLibrary.Cudd_CountPath(function) - getPathsToNonZeroTerminalsCount();
    }

    public int getReorderingsCount() {
    	return BigcuddLibrary.Cudd_ReadReorderings(dd);
    }

    public int getGarbageCollectionsCount() {
    	return BigcuddLibrary.Cudd_ReadGarbageCollections(dd);
    }

    public long getAddSizeInBytes() {
    	return BigcuddLibrary.Cudd_ReadMemoryInUse(dd);
    }

    @Override
    public int hashCode() {
        return this.function.hashCode();
    }

    Pointer<DdNode> getUnderlyingNode() {
        return this.function;
    }

    /**************************************************************
     *** Operators definitions
     *************************************************************/

    private static final BigcuddLibrary.Cudd_addApply_arg1_callback TIMES = new BigcuddLibrary.Cudd_addApply_arg1_callback() {
        @Override
        public Pointer<DdNode > apply(Pointer<BigcuddLibrary.DdManager > dd,
                                      Pointer<Pointer<DdNode > > node1,
                                      Pointer<Pointer<DdNode > > node2) {
            return BigcuddLibrary.Cudd_addTimes(dd, node1, node2);
        }
    };

    private static final BigcuddLibrary.Cudd_addApply_arg1_callback PLUS = new BigcuddLibrary.Cudd_addApply_arg1_callback() {
        @Override
        public Pointer<DdNode > apply(Pointer<BigcuddLibrary.DdManager > dd,
                                      Pointer<Pointer<DdNode > > node1,
                                      Pointer<Pointer<DdNode > > node2) {
            return BigcuddLibrary.Cudd_addPlus(dd, node1, node2);
        }
    };

    private static final BigcuddLibrary.Cudd_addApply_arg1_callback DIVIDE = new BigcuddLibrary.Cudd_addApply_arg1_callback() {
        @Override
        public Pointer<DdNode > apply(Pointer<BigcuddLibrary.DdManager > dd,
                                      Pointer<Pointer<DdNode > > node1,
                                      Pointer<Pointer<DdNode > > node2) {
            return BigcuddLibrary.Cudd_addDivide(dd, node1, node2);
        }
    };

    private static final BigcuddLibrary.Cudd_addApply_arg1_callback MINUS = new BigcuddLibrary.Cudd_addApply_arg1_callback() {
        @Override
        public Pointer<DdNode > apply(Pointer<BigcuddLibrary.DdManager > dd,
                                      Pointer<Pointer<DdNode > > node1,
                                      Pointer<Pointer<DdNode > > node2) {
            return BigcuddLibrary.Cudd_addMinus(dd, node1, node2);
        }
    };

    private static final BigcuddLibrary.Cudd_addApply_arg1_callback LOGICAL_OR = new BigcuddLibrary.Cudd_addApply_arg1_callback() {
        @Override
        public Pointer<DdNode > apply(Pointer<BigcuddLibrary.DdManager > dd,
                                      Pointer<Pointer<DdNode > > node1,
                                      Pointer<Pointer<DdNode > > node2) {
            return BigcuddLibrary.Cudd_addOr(dd, node1, node2);
        }
    };

    private class CubeSpliterator extends AbstractSpliterator<Collection<String>> {

        private Pointer<Pointer<Integer>> cubePtr;
        private Pointer<Double> valuePtr;
        private Pointer<DdGen> generator;
        private int numVars;
        private Iterator<List<String>> expandedIterator;

        protected CubeSpliterator() {
            super((long) BigcuddLibrary.Cudd_CountPathsToNonZero(function),
                  Spliterator.SIZED | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED);

            Pointer<Integer> dummy = Pointer.allocateInt();
            // A pointer to a freshly allocated pointer to int.
            // As Cudd_FirstCube and Cudd_NextCube allocate the returned cubes,
            // allocating a whole int[] here makes no sense. Thus, we allocate
            // only the position where the address to the generated cubes are
            // to be stored.
            cubePtr = Pointer.pointerToPointer(dummy);
            // A pointer to a freshly allocated double.
            valuePtr = Pointer.pointerToDouble(0);

            // So let's start the iteration!
            generator = BigcuddLibrary.Cudd_FirstCube(dd,
                                                      function,
                                                      cubePtr,
                                                      valuePtr);
            numVars = BigcuddLibrary.Cudd_ReadSize(dd);

        }

        @Override
        public boolean tryAdvance(Consumer<? super Collection<String>> action) {
            if (expandedIterator == null || !expandedIterator.hasNext()) {
                if (BigcuddLibrary.Cudd_IsGenEmpty(generator) == 0) {
                    Pointer<Integer> cube = cubePtr.getPointer(Integer.class);
                    int[] presenceVector = cube.getInts(numVars);
                    List<String> configuration = variableStore.fromPresenceVector(presenceVector);

                    Collection<List<String>> expanded = expandDontCares(configuration);
                    expandedIterator = expanded.iterator();

                    BigcuddLibrary.Cudd_NextCube(generator,
                            cubePtr,
                            valuePtr);
                } else {
                    if (generator != null) {
                        BigcuddLibrary.Cudd_GenFree(generator);
                        generator = null;
                    }
                    return false;
                }
            }

            action.accept(expandedIterator.next());
            return true;
        }

    }

}