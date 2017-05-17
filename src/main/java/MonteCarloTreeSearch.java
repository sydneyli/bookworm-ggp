import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MonteCarloTreeSearch {
    abstract class Node {
    	protected int visits = 0;
    	protected double utility = 0;
    	protected final Optional<Node> parent;
    	protected final MachineState state;
    	protected List<Node> children = new ArrayList<>();

    	protected Node(MachineState state, Optional<Node> parent) {
    		this.state = state;
    		this.parent = parent;
    	}

    	protected Node(MachineState state) {
    		this(state, Optional.empty());
    	}

    	// 1 or -1 to multiply with the utility!
    	protected int polarity() { return 1; }

    	// Should never be called on root
        public double selectFn() {
            assert(parent.isPresent());
            assert(visits > 0);
            return polarity() * (utility/(double)visits) +
                   Math.sqrt(2 * Math.log(parent.get().visits)/visits);
        }

    	public Node select() {
     		if (visits == 0) return this;
    		for (Node child : children) if (child.visits == 0) return child;
    		double score = 0;
    		Node result = this;
    		for (Node child : children) {
    			double newScore = child.selectFn();
    			if (newScore > score) {
    				score = newScore;
    				result = child;
    			}
    		}
    		if (result == this) return this;
    		return result.select();
    	}

    	public abstract void expand() throws MoveDefinitionException, TransitionDefinitionException;

    	public void backpropagate(double score) {
    		visits++;
    		utility += score;
    		parent.ifPresent(n -> n.backpropagate(score));
    	}

    }

    class MinNode extends Node {
    	final Move action;
		public MinNode(MachineState state, MaxNode parent, Move action) {
			super(state, Optional.of(parent));
			this.action = action;
        }

		@Override
		public void expand() throws MoveDefinitionException, TransitionDefinitionException {
    		for (List<Move> moves : stateMachine.getLegalJointMoves(state, role, action)) {
    			children.add(new MaxNode(stateMachine.findNext(moves, state), this));
    		}
    	}

		@Override
		protected int polarity() { return -1; }

    	@Override
		public String toString() {
    		String s = action.toString() + "[ ";
    		for (Node n : children)
    			s += n.toString() + " ";
    		return s + "]";
    	}
    }

    class MaxNode extends Node {
 		public MaxNode(MachineState state) { super(state); }
 		public MaxNode(MachineState state, MinNode parent) { super(state, Optional.of(parent)); }

		@Override
		public void expand() throws MoveDefinitionException, TransitionDefinitionException {
			for (Move m : stateMachine.getLegalMoves(state, role)) {
				children.add(new MinNode(state, this, m));
			}
		}

    	@Override
		public String toString() {
    		return "";
    	}
   }

	Node root;
	final StateMachine stateMachine;
	final Role role;

	public MonteCarloTreeSearch(StateMachine machine, Role role) {
		this.stateMachine = machine;
		this.role = role;
	}

	public void updateRoot(MachineState state) throws MoveDefinitionException, TransitionDefinitionException {
		root = new MaxNode(state);
	}

	public void search() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		Node node = root.select();
		node.expand();
		double score = monteCarlo(node.state, 5); // TODO make 5 a constant
		node.backpropagate(score);
	}

	public Move chooseMove() {
		double bestScore = -1;
		Optional<Move> bestMove = Optional.empty();
		for (Node n : root.children) {
			double score = (n.utility / (double) n.visits);
			if (score > bestScore) {
				bestScore = score;
				bestMove = Optional.of(((MinNode)n).action);
			}
		}
		return bestMove.get();
	}

	private double monteCarlo(MachineState state, int count) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		double total = 0;
		for (int i = 0; i < count; i++) {
			total += depthCharge(state);
		}
		return total/count;
	}

    private double depthCharge(MachineState state)
			throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		if (stateMachine.findTerminalp(state))
			return stateMachine.findReward(role, state);
		MachineState randomState = stateMachine.getNextState(state, stateMachine.getRandomJointMove(state));
		return depthCharge(randomState);
	}
}
