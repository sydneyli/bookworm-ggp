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
	public static final int N_DEPTH_CHARGES = 10;
    abstract class Node {
    	protected volatile int visits = 0;
    	protected volatile double utility = 0;
    	protected final MachineState state;
    	final Optional<Node> parent;
    	volatile List<Node> children = new ArrayList<>();

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
        private double selectFn() {
            assert(parent.isPresent());
            assert(visits > 0);
            return polarity() * (utility/(double)visits) +
                   Math.sqrt(2 * Math.log(parent.get().visits)/visits);
        }

        class Result {
        	final Node n; final boolean recurse;
        	public Result(Node n, boolean r) { this.n = n; this.recurse = r;}
        	public Result(Node n) { this(n, false); }
        }

        private synchronized Result selectSync() {
     		if (visits == 0) return new Result(this);
     		// explore node if not all grandchildren are expanded
    		for (Node child : children) {
    			if (child.visits == 0) return new Result(child);
    			for (Node grandchild : child.children) {
    				if (grandchild.visits == 0) return new Result(grandchild);
    			}
    		}
    		double score = 0;
    		Node result = this;
    		for (Node child : children) {
                double newScore = child.selectFn();
                if (newScore > score) {
                    score = newScore;
                    result = child;
                }
    		}
    		if (result == this) return new Result(this);
    		return new Result(result, true);
        }

    	public Node select() {
    		Result result = selectSync();
    		if (result.recurse) return result.n.select();
    		return result.n;
    	}

    	public abstract void expand() throws MoveDefinitionException, TransitionDefinitionException;

    	public synchronized void backpropagate(double score) {
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
		public synchronized void expand() throws MoveDefinitionException, TransitionDefinitionException {
    		for (List<Move> moves : stateMachine.getLegalJointMoves(state, role, action)) {
    			children.add(new MaxNode(stateMachine.findNext(moves, state), this));
    		}
    	}

		@Override
		protected int polarity() { return -1; }
    }

    class MaxNode extends Node {
 		public MaxNode(MachineState state) { super(state); }
 		public MaxNode(MachineState state, MinNode parent) { super(state, Optional.of(parent)); }

		@Override
		public synchronized void expand() throws MoveDefinitionException, TransitionDefinitionException {
			for (Move m : stateMachine.getLegalMoves(state, role)) {
				Node node = new MinNode(state, this, m);
				children.add(node);
				node.expand();
			}
		}
    }

	volatile Node root;
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
		double score = monteCarlo(node.state);
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

	private double monteCarlo(MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		double total = 0;
		for (int i = 0; i < N_DEPTH_CHARGES; i++) {
			total += depthCharge(state);
		}
		return total/N_DEPTH_CHARGES;
	}

    private double depthCharge(MachineState state)
			throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		if (stateMachine.findTerminalp(state))
			return stateMachine.findReward(role, state);
		MachineState randomState = stateMachine.getNextState(state, stateMachine.getRandomJointMove(state));
		return depthCharge(randomState);
	}
}
