import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class MiniMax extends StateMachineGamer {
	/**
	 * Maximum recursive depth to search.
	 * Enter -1 for infinite depth (i.e. evaluate entire tree)!
	 */
	protected static final int MAX_DEPTH = -1;

	/**
	 * Our heuristic function. TODO implement this so it's not shit
	 */
	protected final Function<MachineState, Integer> heuristic;

	public MiniMax() {
		super();
		this.heuristic = x -> 0; // TODO implement heuristic
	}

	@Override
	public String getName() {
		return "Minimax";
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	/**
	 * Immutable struct to potentially associate a Move with a score.
	 */
	protected class Score implements Comparable<Score> {
		public final int score;
		public final Optional<Move> move;
		public Score(int score, Move move) {
			this.score = score;
			this.move = Optional.of(move);
		}

		public Score(int score) {
			this.score = score;
			this.move = Optional.empty();
		}

		@Override
		public String toString() {
			return "(" + move.map(move -> move.toString()).orElse("N/A") + ", " + score + ")";
		}

		@Override
		public int compareTo(Score o) {
			return Integer.compare(score, o.score);
		}
	}

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
        return new Score(best.score, move);
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
			return new Score(this.heuristic.apply(currentState));
		}
		Score best = new Score(Integer.MIN_VALUE);
		for (Move move : machine.getLegalMoves(currentState, getRole())) {
			best = Util.max(minR(currentState, move, depth), best);
		}
		return best;
	}

	protected Move chooseMove() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		return maxR(getCurrentState(), 0).move.get();
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = chooseMove();

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO: any metagame?

	}

	@Override
	public void stateMachineStop() {
		// TODO Any cleanup

	}

	@Override
	public void stateMachineAbort() {
		// TODO Any cleanup

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// No game previewing

	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}
}
