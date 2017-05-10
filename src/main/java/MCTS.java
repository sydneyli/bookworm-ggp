import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTS extends SampleGamer {
	private volatile boolean shouldStop = false;
	private volatile boolean debug = false;

	class Timeout extends Thread {
		private static final int buffer = 3000;
		long timeout;
		Timeout(long timeout) {
			this.timeout = timeout;
		}

		@Override
		public void run() {
			while (timeout - System.currentTimeMillis() > buffer) {
				try {
					Thread.sleep(timeout - System.currentTimeMillis() - buffer);
				} catch (InterruptedException e) {

				}
			}
			System.out.println("Stopping");
			shouldStop = true;
		}
	}

	class MCNode {
		// If move is not null, then we only consider children in which we make that move.
		// If move is null, there will be one child for each move we can make
		Move move = null;
		int visits = 0;
		double utility = 0;
		MCNode parent = null;
		MachineState state;
		ArrayList<MCNode> children;
		public MCNode(MachineState state, int visits, double utility, MCNode parent, ArrayList<MCNode> children, Move move) {
			this.move = move;
			this.state = state;
			this.visits = visits;
			this.parent = parent;
			this.utility = utility;
			this.children = children;
		}
	}

	double selectfn(MCNode node) {
		return Math.log((node.utility + 5)/node.visits) + 15 * Math.sqrt(Math.log(node.parent.visits)/node.visits);
	}

	public MCNode select (MCNode node, int depth, boolean settle) {
		// if initial node unvisited
		if (node.visits == 0 && node.parent == null) {
			return node;
		}
		// settle??? if not initial node and there is no next move???
		if (settle && depth > 1 && node.move == null) {
			return node;
		}
		// depth limited at 500
		if (depth > 500) {
			if (new Random().nextDouble() < 0.001) {
				System.out.println("Depth limit");
			}
			return null;
		}
		// if node unvisited and there is no next move???
		if (node.visits==0 && node.move == null) {
			return node;
		}
		// if the state is terminal, return null???
		if (getStateMachine().isTerminal(node.state)) {
			return null;
		}
		// for each child, if any child node is unvisited, select that node
		for (int i=0; i<node.children.size(); i++) {
			MCNode child = node.children.get(i);
			if (child.visits==0) {
				return select(child, depth + 1, settle);
			}
		}
		// uses the selectfn function to find the values for the child nodes to maximize the value
		double score = 0;
		MCNode result = node;
		if (node.children.size() == 0) {
			return null;
		}
		for (int i=0; i<node.children.size(); i++) {
			double newscore = selectfn(node.children.get(i));
			if (newscore>score) {
				score = newscore;
				result=node.children.get(i);
			}
		};
		return select(result, depth + 1, settle);
	}

	/**
	 * The successors of the state chosen during the selection phase are added to the tree.
	 * @param node
	 * @return boolean
	 */
	public boolean expand (MCNode node) throws MoveDefinitionException, TransitionDefinitionException {
		if (getStateMachine().isTerminal(node.state)) {
			return true;
		}
		List<Move> actions = getStateMachine().getLegalMoves(node.state, getRole());
		for (int i=0; i < actions.size(); i++) {
			addNewStates(node, actions.get(i));
		};
		return true;
	}

	public void addNewStates(MCNode node, Move move) throws MoveDefinitionException, TransitionDefinitionException {
		if (node.move != null) {
			throw new MoveDefinitionException(node.state, getRole());
		}
		List<List<Move>> moves = getStateMachine().getLegalJointMoves(node.state, getRole(), move);
		MCNode newnode = new MCNode(node.state, 0, 0, node, new ArrayList<MCNode>(), move);
		node.children.add(newnode);
		// simulation
		for (List<Move> jointMove: moves) {
			MachineState state = getStateMachine().getNextState(node.state, jointMove);
			//System.out.println("Made move " + move + " from " + node.state + " to " + state);
			newnode.children.add(new MCNode(state, 0, 0, newnode, new ArrayList<MCNode>(), null));
		}
	}

	public boolean backpropagateH(MCNode node, double score, MCNode base, int depth) {
		node.visits = node.visits+1;
		node.utility = node.utility+score;
		if (node.move != null) {
			if (node.move.toString().contains("mark 1 1")) {
				//System.out.print("MARK ");
			}
			//System.out.println("node " + node.state + " " + node.move + " score " + score + " base " + base.state + " " + base.move + " depth " + depth);
		}
		if (node.parent != null) {
			backpropagateH(node.parent,score, node, depth + 1);
		}
		return true;
	}

	public boolean backpropagate (MCNode node, double score) {
		return backpropagateH(node, score, node, 0);
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		shouldStop = false;
		Timeout t = new Timeout(timeout);
		t.start();
		MCNode root = new MCNode(getCurrentState(), 0, 0, null, new ArrayList<MCNode>(), null);
		int num = 0;
		int numBad = 0;
		int numContinues = 0;
		while (!shouldStop) {
			boolean settle = numContinues > num;
			MCNode s = select(root, 0, settle);
			// if terminal/no more children, continue
			if (s == null) {
				numContinues++;
				continue;
			}
			expand(s);
			int[] theDepth = new int[1];
			MachineState terminal = getStateMachine().performDepthCharge(s.state, theDepth);
			double score = 0;
			if (terminal != null) {
				score = (double) getStateMachine().getGoal(terminal, getRole());
			} else {
				System.out.println("hmm, terminal state is null");
			}
			//System.out.println("Score " + score + " state " + terminal);
			backpropagate(s, score);
			num++;
			if (num % 50 == 0) {
				System.out.println("Performed " + num + " depth charges " + numBad + " errors, got score " + score);
			}
		}
		System.out.println("Stopped");

		double bestScore = -1;
		Move bestMove = null;
		for (int i = 0; i < root.children.size(); i++) {
			MCNode n = root.children.get(i);
			System.out.println("Considering node, visited " + n.visits + " times, move " + n.move + ", value " + n.utility / n.visits + ", num children " + n.children.size());
			if ((n.utility / n.visits) > bestScore) {
				bestScore = (n.utility / n.visits);
				bestMove = n.move;
			}
		}

		System.out.println("Utility: " + bestScore);
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

		Move selection = bestMove;

		long stop = System.currentTimeMillis();
		t.stop();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}
}