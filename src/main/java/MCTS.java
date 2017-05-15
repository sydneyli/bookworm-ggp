import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

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
	private volatile MCNode root = null;
	private static final int numThreads = 5;

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

    class ExpandTree extends Thread {
        ExpandTree() {
        }

        @Override
		public void run() {
        	try {
				expandTree();
			} catch (MoveDefinitionException | TransitionDefinitionException | GoalDefinitionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
    	Semaphore semaphore;
    	public MCNode(MachineState state, int visits, double utility, MCNode parent, ArrayList<MCNode> children, Move move) {
    		this.move = move;
    		this.state = state;
    		this.visits = visits;
    		this.parent = parent;
    		this.utility = utility;
    		this.children = children;
    		semaphore = new Semaphore(1);
    	}
    }

    double selectfn(MCNode node) {
    	return new Random().nextDouble();//Math.log(5 + node.utility/node.visits) + 50 * Math.log(node.parent.visits)/node.visits;
    }

    public MCNode select (MCNode node, int depth, boolean settle) {
    	if (depth == 1 && !node.semaphore.tryAcquire()) {
    		return null;
    	}
    	if ((node.visits == 0 && node.parent == null) || (settle && depth > 1 && node.move == null)) {
    		return node;
    	}
    	if (node.utility < 0) {
    		System.err.println("Utility is " +  node.utility);
    	}
    	if (depth > 495) {
    		System.out.println(node.state + " " + node.move + " " + node.utility + " " + node.utility);
    	}
    	if (depth > 500) {
    		System.out.println("Depth limit");
    		return null;
    	}
    	if (node.visits==0 && node.move == null) {
    		return node;
    	}
    	if (getStateMachine().isTerminal(node.state)) {
    		return null;
    	}
	    for (int i=0; i<node.children.size(); i++) {
	    	MCNode child = node.children.get(i);
	    	 if (child.visits==0) {
	    		 return select(child, depth + 1, settle);
	    	 }
	     }
	     double score = -1;
	     MCNode result = node;
	     if (node.children.size() == 0) {
	    	 return null;
	     }
	     for (int i=0; i<node.children.size(); i++) {
	    	 double newscore = selectfn(node.children.get(i));
	    	  if (depth > 495) {
	    		  System.out.println("new score " + newscore + " old score " + score + " node " + node.children.get(i).state);
	    	  }
	          if (newscore>score) {
	        	  score = newscore;
	        	  result=node.children.get(i);
	          }
	     }

	     if (result == node || score < 0) {
	    	 return null;
	     }
	     return select(result, depth + 1, settle);
     }

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
    	for (List<Move> jointMove: moves) {
    		MachineState state = getStateMachine().getNextState(node.state, jointMove);
    		//System.out.println("Made move " + move + " from " + node.state + " to " + state);
    		newnode.children.add(new MCNode(state, 0, 0, newnode, new ArrayList<MCNode>(), null));
    	}
    }

    public double minMax(MCNode node, int depth) {
    	if (depth == 0) {
    		return node.utility;
    	}
    	double minVal = 101;
    	for (MCNode child: node.children) {
    		for (MCNode grandChild: child.children) {
    			minVal = Math.min(minMax(grandChild, depth - 1), minVal);
    		}
    	}
    	if (minVal == 101) {
    		return node.utility;
    	}
    	return minVal;
    }

    public boolean backpropagateH(MCNode node, double score, MCNode base, int depth) {
    	if (node.parent == null) {
    		return true; // No need to update parent; this would cause concurrent update
    	}
    	if (score < 0) {
    		System.err.println("wtf");
    	}
    	node.visits = node.visits+1;
	    if (node.move != null) {
		    node.utility = node.utility+score;
	    } else {
	    	double min = 101;
    		for (MCNode child: node.children) {
    			min = Math.min(minMax(child, 1), min);
    		}
	    	if (min == 101) {
	    		min = node.utility;
	    		if (new Random().nextFloat() < 0.01) {
	    			System.out.println("bad backprop");
	    		}
	    	}
	    	node.utility = min;
	    }
	    if (node.parent != null) {
	    	backpropagateH(node.parent,score, node, depth + 1);
	    }
	    return true;
    }

    public boolean backpropagate (MCNode node, double score) {
    	return backpropagateH(node, score, node, 0);
	}

    private void expandTree() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int num = 0;
		int numBad = 0;
		int numContinues = 0;
		while (!shouldStop) {
			boolean settle = numContinues > num;
			MCNode s = select(root, 0, settle);
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
			releaseLocks(s);
			num++;
			if (num % 5000 == 0) {
				System.out.println("Performed " + num + " depth charges " + numBad + " errors, got score " + score);
			}
		}
		System.out.println("Stopped");
    }

	private void releaseLocks(MCNode s) {
		// TODO Auto-generated method stub
		if (s == null) {
			return;
		}
		if (s.parent != null && s.parent.parent == null) {
			if (s.semaphore.availablePermits() != 0) {
				System.out.println("Bad permit");
			} else {
				s.semaphore.release();
			}
		}
		releaseLocks(s.parent);
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		shouldStop = false;
		Timeout t = new Timeout(timeout);
		t.start();
		root = new MCNode(getCurrentState(), 0, 0, null, new ArrayList<MCNode>(), null);
		expand(root);
		root.visits++;

		for (int i = 0; i < numThreads; i++) {
			ExpandTree et = new ExpandTree();
			et.start();
			if (i == 0) {
				try { // First thread gets head start
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		expandTree();
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
		if (selection == null) {
			System.out.println("null move!");
			selection = moves.get(0);
		}
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}
}