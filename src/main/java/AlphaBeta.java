import java.util.List;

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
	private static final int MAX_DEPTH = 6;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = maxR(getCurrentState(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE).move.get();

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	/**
	 * For our |move| on |currentState|, calculate "worst-case" [min] scoring
	 * of opposing players, given our optimal [max] behavior.
	 * @throws Game definition exceptions if GDL specification is malformed.
	 */
	private Score minR(final MachineState currentState, final Move move, int depth, int a, int b)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine machine = getStateMachine();
        Score best = new Score(Integer.MAX_VALUE);
        for (List<Move> jointMove : machine.getLegalJointMoves(currentState, getRole(), move)) {
			if (b <= a) break;
        	MachineState nextState = machine.getNextState(currentState, jointMove);
        	best = Util.min(maxR(nextState, depth + 1, a, b), best);
			b = Util.min(b, best.value);
        }
        return new Score(best.value, move);
    }

	/**
	 * For |currentState|, calculate our best [max] action, given
	 * worst-case behavior [min] of opposing players.
	 * @throws Game definition exceptions if GDL specification is malformed.
	 */
	private Score maxR(final MachineState currentState, int depth, int a, int b)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.isTerminal(currentState)) {
			return new Score(machine.getGoal(currentState, getRole()));
		} else if (depth == MAX_DEPTH) {
			return new Score(evaluate());
		}
		Score best = new Score(Integer.MIN_VALUE);
		for (Move move : machine.getLegalMoves(currentState, getRole())) {
			if (b <= a) break;
			best = Util.max(minR(currentState, move, depth, a, b), best);
			a = Util.max(a, best.value);
		}
		return best;
	}

	@Override
	protected int evaluate() throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		return Heuristics.combination(this);
	}
}