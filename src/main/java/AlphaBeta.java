import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class AlphaBeta extends HeuristicGamer {
	/**
	 * Maximum recursive depth to search. See docs in |MiniMax|.
	 */
	private int MAX_DEPTH = 10;
	private boolean shouldStop = false;


    class Timeout extends Thread {
    	private static final int buffer = 2000;
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

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		shouldStop = false;
		Timeout t = new Timeout(timeout);
		t.start();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		long allocated_time = (timeout - System.currentTimeMillis() - Timeout.buffer) / moves.size();
		System.out.println("Time given: " + allocated_time);
		Score bestScore = maxR(getCurrentState(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, allocated_time);
		System.out.println(bestScore.value);
		Move selection = bestScore.move.get();

		long stop = System.currentTimeMillis();
		t.stop();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	/**
	 * For our |move| on |currentState|, calculate "worst-case" [min] scoring
	 * of opposing players, given our optimal [max] behavior.
	 * @throws Game definition exceptions if GDL specification is malformed.
	 */
	private Score minR(final MachineState currentState, final Move move, int depth, int a, int b, long allocated_time)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		long time_start = System.currentTimeMillis();
        StateMachine machine = getStateMachine();
        Score best = new Score(Integer.MAX_VALUE);
        int num_moves_processed = 0;
        List<List<Move>> jointMoves = machine.getLegalJointMoves(currentState, getRole(), move);
        for (List<Move> jointMove : jointMoves) {
        	num_moves_processed++;
			if (b <= a) break;
			long timeLeft = allocated_time - (System.currentTimeMillis() - time_start);
        	MachineState nextState = machine.getNextState(currentState, jointMove);
        	best = Util.min(maxR(nextState, depth + 1, a, b, timeLeft / (jointMoves.size() - (num_moves_processed - 1))), best);
			b = Util.min(b, best.value);
        }
        return new Score(best.value, move);
    }

	/**
	 * For |currentState|, calculate our best [max] action, given
	 * worst-case behavior [min] of opposing players.
	 * @throws Game definition exceptions if GDL specification is malformed.
	 */
	private Score maxR(final MachineState currentState, int depth, int a, int b, long allocated_time)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		long time_start = System.currentTimeMillis();
		StateMachine machine = getStateMachine();
		if (machine.isTerminal(currentState)) {
			return new Score(machine.getGoal(currentState, getRole()));
		} else if (depth == MAX_DEPTH || shouldStop || allocated_time < 0) {
			if (new Random().nextFloat() < 0.01) {
				System.out.println("Ending at depth " + depth);
			}
			return new Score(evaluate());
		}
		Score best = new Score(Integer.MIN_VALUE);
		int num_moves_processed = 0;
		List<Move> moves = machine.getLegalMoves(currentState, getRole());
		for (Move move : moves) {
			num_moves_processed++;
			long timeLeft = allocated_time - (System.currentTimeMillis() - time_start);
			if (b <= a) {
				break;
			}
			best = Util.max(minR(currentState, move, depth, a, b, timeLeft / (moves.size() - (num_moves_processed - 1))), best);
			a = Util.max(a, best.value);

		}
		if (new Random().nextFloat() < 0.01) {
			System.out.println("Time remaining: " + (allocated_time - (System.currentTimeMillis() - time_start)));
		}
		return best;
	}

	@Override
	protected int evaluate() throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
//		try {
			return Heuristics.goalProximity(this);
//		} catch (Exception e) {
//			System.out.println("BAD");
//		}
//		return 0;
	}
}