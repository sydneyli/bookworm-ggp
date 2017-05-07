import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTS extends SampleGamer {
	private volatile boolean shouldStop = false;

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
    	int visits = 0;
    	double utility = 0;
    	MCNode parent = null;
    	MachineState state;
    	ArrayList<MCNode> children;
    	public MCNode(MachineState state, int visits, double utility, MCNode parent, ArrayList<MCNode> children) {
    		this.state = state;
    		this.visits = visits;
    		this.parent = parent;
    		this.utility = utility;
    		this.children = children;
    	}
    }

    double selectfn(MCNode node) {
    	return node.utility/node.visits + Math.sqrt(2*Math.log(node.parent.visits)/node.visits);
    }

    public MCNode select (MCNode node) {
    	if (node.visits==0) {
    		return node;
    	}
	     for (int i=0; i<node.children.size(); i++) {
	    	 if (node.children.get(i).visits==0) {
	    		 return node.children.get(i);
	    	 }
	     };
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
	     return select(result);
     }

    public boolean expand (MCNode node) throws MoveDefinitionException, TransitionDefinitionException {
    	if (getStateMachine().isTerminal(node.state)) {
    		return true;
    	}
    	List<List<Move>> actions = getStateMachine().getLegalJointMoves(node.state);
	    for (int i=0; i < actions.size(); i++) {
	    	 MachineState newstate = getStateMachine().getNextState(node.state, actions.get(i));
	         MCNode newnode = new MCNode(newstate, 0, 0, node, new ArrayList<MCNode>());
	         node.children.add(newnode);
	    };
	    return true;
    }
/*
    public int depthcharge (Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
	    if (getStateMachine().isTerminal(state)) {
	    	System.out.println("Performed depth charge: reward " + getStateMachine().findReward(role, state));
	    	return getStateMachine().findReward(role, state);
	    };
	    List<Move> move = new ArrayList<Move>();
	    List<Role> roles = getStateMachine().getRoles();
	    for (int i=0; i < roles.size(); i++) {
	    	List<Move> options = getStateMachine().getLegalMoves(state, roles.get(i));
	    	Random r = new Random();
	        move.add(options.get(r.nextInt(options.size())));
	    }
	    MachineState newstate = getStateMachine().getNextState(state, move);
	    return depthcharge(role, newstate);
    }*/

    public boolean backpropagate (MCNode node, double score) {
    	node.visits = node.visits+1;
	    node.utility = node.utility+score;
	    if (node.parent != null) {
	    	backpropagate(node.parent,score);
	    };
	    return true;
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		shouldStop = false;
		Timeout t = new Timeout(timeout);
		t.start();
		MCNode root = new MCNode(getCurrentState(), 0, 0, null, new ArrayList<MCNode>());
		int num = 0;
		int numBad = 0;
		while (!shouldStop) {
			MCNode s = select(root);
			if (s == null) {
				continue;
			}
			expand(s);
			int[] theDepth = new int[1];
			MachineState terminal = getStateMachine().performDepthCharge(s.state, theDepth);
			double score = 0;
			if (terminal != null) {
				score = (double) getStateMachine().getGoal(terminal, getRole());
				numBad++;
			}
			backpropagate(s, score);
			num++;
			if (num % 3000 == 0) {
				System.out.println("Performed " + num + " depth charges" + numBad + " errors, got score " + score);
			}
		}
		System.out.println("Stopped");

		double bestScore = -1;
		int bestIndex = 0;
		for (int i = 0; i < root.children.size(); i++) {
			MCNode n = root.children.get(i);
			if ((n.utility / n.visits) > bestScore) {
				bestScore = (n.utility / n.visits);
				bestIndex = i;
			}
		}

		System.out.println("Utility: " + bestScore);
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(bestIndex);

		long stop = System.currentTimeMillis();
		t.stop();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	/*
	@Override
	public StateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub
	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

	@Override
	public String getName() {
		return "Monte Carlo Tree Search";
	}
	*/
}