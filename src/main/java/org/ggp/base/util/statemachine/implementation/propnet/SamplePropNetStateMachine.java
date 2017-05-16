package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
            propNet = OptimizingPropNetFactory.create(description);
            roles = propNet.getRoles();
            ordering = getOrdering();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
        return propNet.getTerminalProposition().getValue();
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @SuppressWarnings("null")
	@Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
    	Set<Proposition> goals = propNet.getGoalPropositions().get(role);
    	Proposition goal = null;
    	for (Proposition curr: goals) {
    		if (goal.getValue()) {
    			if (goal != null) {
    				throw new GoalDefinitionException(state, role);
    			}
    			goal = curr;
    		}
    	}
    	return getGoalValue(goal);
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
    	propNet.getInitProposition().setValue(true);
    	getStateFromBase(); //not sure if this is right?
    	return null;
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
        // TODO: Compute legal moves.
        return null;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
    	List<Move> moves = new ArrayList<Move>();
    	Set<Proposition> propositions = propNet.getLegalPropositions().get(role);
    	for (Proposition p: propositions) {
    		if (p.getValue()) {
    			moves.add(getMoveFromProposition(p));
    		}
    	}
        return moves;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
        // TODO: Compute the next state.
        return null;
    }

    /**
     * This should compute the topological ordering of propositions.
     * Each component is either a proposition, logical gate, or transition.
     * Logical gates and transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt
     * from this ordering.
     *
     * The base propositions values are set from the MachineState that
     * operations are performed on and the input propositions are set from
     * the Moves that operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */
    public List<Proposition> getOrdering()
    {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // All of the edges in the PropNet
        Set<Component> edges = new HashSet<>(propNet.getComponents());
        edges.removeAll(propositions);

        // Kahn's algorithm
        // 1. Init search to list of all nodes with no input
        Queue<Proposition> search = new LinkedList<>(propositions.stream()
        		.filter(prop -> prop.getInputs().isEmpty()).collect(Collectors.toList()));

        while (!search.isEmpty()) {
        	// 2. Add popped node to ordering
        	Proposition node = search.remove();
        	order.add(node);
            // 3. Remove this node's output edges from graph
        	for (Component edge : node.getOutputs()) {
        		assert(!(edge instanceof Proposition)); // neighbor gotta be logical gate or transition
        		edges.remove(edge);

        		// Functional
        		edge.getOutputs().stream()
                    // 3A. Remove this edge as input to connected nodes
                    .map(prop -> {prop.removeInput(edge); return (Proposition) prop;})
                    // 3B. Add connected nodes with no inputs to search queue
                    .filter(prop -> prop.getInputs().isEmpty())
                    .forEach(search::add);
        		// Imperative
        		// for (Component neighbor : edge.getOutputs()) {
        		// 	assert(neighbor instanceof Proposition);
        		// 	neighbor.removeInput(edge);
        		// 	if (neighbor.getInputs().isEmpty()) {
        		// 		search.add((Proposition) neighbor);
        		// 	}
        		// }
        	}
        }
        if (!edges.isEmpty()) {
        	throw new RuntimeException("Oh no!!! there are still edges left... detected cycle in propnet during toposort");
        }
        // 4. Exempt base/input props
        order.stream().filter(prop -> !isBaseOrInput(prop))
                      .collect(Collectors.toList());
        return order;
    }

    private boolean isBaseOrInput(Component p) {
    	return propNet.getBasePropositions().values().contains(p) ||
    		propNet.getInputPropositions().values().contains(p);
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves)
    {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++)
        {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p)
    {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition)
    {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    private void markBases(Map<GdlSentence, Boolean> baseMarks) {
    	baseMarks.entrySet().stream().forEach(
    			e -> propNet.getBasePropositions().get(e.getKey())
                            .setValue(e.getValue()));
    }

    private void markActions(Map<GdlSentence, Boolean> actionMarks) {
     	actionMarks.entrySet().stream().forEach(
    			e -> propNet.getInputPropositions().get(e.getKey())
                            .setValue(e.getValue()));
    }

    private void clearMarks() {
    	propNet.getBasePropositions().values().forEach(base -> base.setValue(false));
    }

    private boolean markProp(Component prop) {
    	if (isBaseOrInput(prop)) return prop.getValue();
    	return prop.mark();
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return PropNetMachineState
     */
    public MachineState getStateFromBase()
    {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values())
        {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }
        }
        return new MachineState(contents);
    }
}