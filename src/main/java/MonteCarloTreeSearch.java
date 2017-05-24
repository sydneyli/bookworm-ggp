import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MonteCarloTreeSearch {
	public static final int N_DEPTH_CHARGES = 8;
    abstract class Node {
    	protected volatile int visits = 0;
    	protected volatile double utility = 0;
    	protected final MachineState state;
    	final Optional<Node> parent;
    	volatile List<Node> children = new ArrayList<>();

    	protected Node(MachineState state, Optional<Node> parent) {
    		this.state = state;
    		this.parent = parent;
    		setInGrandparentsUnvisitedList();
    	}

    	@Override
    	public boolean equals(Object o) {
    		if (o instanceof Node) {
    			return state.equals(((Node)o).state);
    		}
    		return false;
    	}

    	public void addInfo(Node n) {
    		visits += n.visits;
    		utility += n.utility;
    		changed = true;
    	}

    	public abstract void combineTree(Node otherRoot);

    	private void setInGrandparentsUnvisitedList() {
    		parent.ifPresent(
                node -> node.unvisitedGrandchildren.add(this));
    		parent.ifPresent(parent ->
                parent.parent.ifPresent(
                    node -> node.unvisitedGrandchildren.add(this)));
    	}

    	protected Node(MachineState state) {
    		this(state, Optional.empty());
    	}

    	// 1 or -1 to multiply with the utility!
    	protected int polarity() { return 1; }

    	private boolean changed = true;
    	private double selectCache = 0;

    	// Should never be called on root
        private double selectFn() {
        	if (!changed) return selectCache;
            selectCache = polarity() * (utility/(double)visits) +
                   Math.sqrt(2 * Math.log(parent.get().visits)/visits);
            changed = false;
            return selectCache;
        }

        class Result {
        	final Node n; final boolean recurse;
        	public Result(Node n, boolean r) { this.n = n; this.recurse = r;}
        	public Result(Node n) { this(n, false); }
        }

        private Queue<Node> unvisitedGrandchildren = new LinkedList();

        private synchronized Result selectSync() {
     		if (visits == 0) return new Result(this);
     		// explore node if not all grandchildren are expanded
     		if (!unvisitedGrandchildren.isEmpty()) {
     			return new Result(unvisitedGrandchildren.remove());
     		}
     		Instant t1 = Instant.now();
    		double score = 0;
    		Node result = this;
    		for (Node child : children) {
                double newScore = child.selectFn();
                if (newScore > score) {
                    score = newScore;
                    result = child;
                }
    		}
     		Instant t2 = Instant.now();
     		stats.subselect = stats.subselect.plus(Duration.between(t1, t2));

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
    		changed = true;
    		parent.ifPresent(n -> n.backpropagate(score));
    	}
    }

    class MinNode extends Node {
    	final Move action;
		public MinNode(MachineState state, MaxNode parent, Move action) {
			super(state, Optional.of(parent));
			this.action = action;
        }

 		Map<MachineState, Node> childrenMap = new HashMap<MachineState, Node>();

 		private Node addChild(MachineState nextState) {
            Node node = new MaxNode(nextState, this);
            children.add(node);
            childrenMap.put(nextState, node);
            return node;
 		}

		@Override
		public synchronized void expand() throws MoveDefinitionException, TransitionDefinitionException {
    		for (List<Move> moves : stateMachine.getLegalJointMoves(state, role, action)) {
    			MachineState nextState = stateMachine.findNext(moves, state);
    			addChild(nextState);
    		}
    	}

		@Override
		protected int polarity() { return -1; }

		@Override
		public void combineTree(Node otherRoot) {
			this.addInfo(otherRoot);
			for (MachineState key : ((MinNode)otherRoot).childrenMap.keySet()) {
				if (!childrenMap.containsKey(key)) {
					addChild(key);
				}
				((MinNode)otherRoot).childrenMap.get(key).combineTree(childrenMap.get(key));
			}
		}
    }

    class MaxNode extends Node {
 		public MaxNode(MachineState state) { super(state); }
 		public MaxNode(MachineState state, MinNode parent) { super(state, Optional.of(parent)); }

 		Map<Move, Node> childrenMap = new HashMap<Move, Node>();

 		private Node addChild(Move nextMove) {
            Node node = new MinNode(state, this, nextMove);
            children.add(node);
            childrenMap.put(nextMove, node);
            return node;
 		}

		@Override
		public synchronized void expand() throws MoveDefinitionException, TransitionDefinitionException {
			for (Move m : stateMachine.getLegalMoves(state, role)) {
				addChild(m).expand();
			}
		}

		@Override
		public void combineTree(Node otherRoot) {
			this.addInfo(otherRoot);
			for (Move key : ((MaxNode)otherRoot).childrenMap.keySet()) {
				if (!childrenMap.containsKey(key)) {
					addChild(key);
				}
				((MaxNode)otherRoot).childrenMap.get(key).combineTree(childrenMap.get(key));
			}
		}
    }

	Node root;
	final StateMachine stateMachine;
	final Role role;
	Stats stats = new Stats();

	private class Stats {
		int numDepthCharges = 0;
		Duration select = Duration.ZERO;
		Duration subselect = Duration.ZERO;
		Duration expand = Duration.ZERO;
		Duration sim = Duration.ZERO;
		Duration prop = Duration.ZERO;
		Stats() {}

		public void reset() {
		 numDepthCharges = 0; select = Duration.ZERO; expand = Duration.ZERO;
		 subselect = Duration.ZERO;
		 sim = Duration.ZERO; prop = Duration.ZERO; }
	}

	public MonteCarloTreeSearch(StateMachine machine, Role role, MachineState startState) {
		this.stateMachine = machine;
		this.role = role;
		this.root = new MaxNode(startState);
	}

	public MonteCarloTreeSearch(StateMachine machine, Role role) {
		this(machine, role, machine.getInitialState());
	}

	public void updateRoot(MachineState state) {
		stats.reset();
		if (state.equals(root.state)) return;
		for (Node child : root.children) {
			for (Node grandchild : child.children) {
				if (state.equals(grandchild.state)) {
					root = grandchild;
					return;
				}
			}
		}
		root = new MaxNode(state);
	}

	public void search() throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        Instant t1 = Instant.now();
        Node node = root.select();
        Instant t2 = Instant.now();
        node.expand();
        Instant t3 = Instant.now();
        double score = monteCarlo(node.state);
        Instant t4 = Instant.now();
        node.backpropagate(score);
        Instant t5 = Instant.now();
        stats.select = stats.select.plus(Duration.between(t1, t2));
        stats.expand = stats.expand.plus(Duration.between(t2, t3));
        stats.sim = stats.sim.plus(Duration.between(t3, t4));
        stats.prop = stats.prop.plus(Duration.between(t4, t5));
	}

	public void search(Duration searchTime) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		Instant start = Instant.now();
		while (Duration.between(start, Instant.now()).compareTo(searchTime) < 0) {
			search();
		}
	}

	public Move chooseMove() {
		double bestScore = -1;
		Optional<Move> bestMove = Optional.empty();
		System.out.println(root.children.size());
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
    	stats.numDepthCharges ++;
		if (stateMachine.findTerminalp(state))
			return stateMachine.findReward(role, state);
		MachineState randomState = stateMachine.getNextState(state, stateMachine.getRandomJointMove(state));
		return depthCharge(randomState);
	}

    public void printStats() {
    	System.out.println("=========MCTS STATS=========");
    	System.out.println("Depth charges: " + stats.numDepthCharges);
    	System.out.println("select time: " + stats.select.toMillis());
    	System.out.println("\tsub time: " + stats.subselect.toMillis());
    	System.out.println("expand time: " + stats.expand.toMillis());
    	System.out.println("sim time: " + stats.sim.toMillis());
    	System.out.println("prop time: " + stats.prop.toMillis());
    	System.out.println("============================");
    }
}
