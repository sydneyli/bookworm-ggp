import java.util.List;
import java.util.function.Function;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MiniMax extends HeuristicGamer {
	/**
	 * Maximum recursive depth to search. At this depth, will evaluate
	 * game state using |Heuristics.getDefault()|.
	 *
	 * Enter -1 for infinite depth (i.e. evaluate entire tree)!
	 */
	private static final int MAX_DEPTH = -1;

	/**
	 * For our |move| on |currentState|, calculate "worst-case" [min] scoring
	 * of opposing players, given our optimal [max] behavior.
	 * @throws Game definition exceptions if GDL specification is malformed.
	 */
	private Score minR(final MachineState currentState, final Move move, int depth)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine machine = getStateMachine();
        Score best = new Score(Integer.MAX_VALUE);
        for (List<Move> jointMove : machine.getLegalJointMoves(currentState, getRole(), move)) {
        	MachineState nextState = machine.getNextState(currentState, jointMove);
        	best = Util.min(maxR(nextState, depth + 1), best);
        }
        return new Score(best.value, move);
    }

	/**
	 * For |currentState|, calculate our best [max] action, given
	 * worst-case behavior [min] of opposing players.
	 * @throws Game definition exceptions if GDL specification is malformed.
	 */
	private Score maxR(final MachineState currentState, int depth)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.isTerminal(currentState)) {
			return new Score(machine.getGoal(currentState, getRole()));
		} else if (depth == MAX_DEPTH) {
			return new Score(evaluate(currentState));
		}
		Score best = new Score(Integer.MIN_VALUE);
		for (Move move : machine.getLegalMoves(currentState, getRole())) {
			best = Util.max(minR(currentState, move, depth), best);
		}
		return best;
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = maxR(getCurrentState(), 0).move.get();

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	@Override
	protected Function<MachineState, Integer> getHeuristic() {
		return Heuristics.dumb();
	}
}
