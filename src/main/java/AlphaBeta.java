import java.util.List;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class AlphaBeta extends MiniMax {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Alpha beta";
	}

	@Override
	protected Move chooseMove() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		return maxR(getCurrentState(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE).move.get();
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
			b = Util.min(b, best.score);
        }
        return new Score(best.score, move);
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
			return new Score(this.heuristic.apply(currentState));
		}
		Score best = new Score(Integer.MIN_VALUE);
		for (Move move : machine.getLegalMoves(currentState, getRole())) {
			if (b <= a) break;
			best = Util.max(minR(currentState, move, depth, a, b), best);
			a = Util.max(a, best.score);
		}
		return best;
	}

}