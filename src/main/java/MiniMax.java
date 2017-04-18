import java.util.ArrayList;
import java.util.Collections;
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
	private static final int MAX_DEPTH = -1;

	/**
	 * Our heuristic function.
	 */
	private final Function<MachineState, Integer> heuristic;

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
	 * Immutable struct to contain a move selection and its score.
	 */
	private class Result implements Comparable<Result> {
		public final int score;
		public final Optional<Move> move;
		public Result(int score, Move move) {
			this.score = score;
			this.move = Optional.of(move);
		}
		public Result(int score) {
			this.score = score;
			this.move = Optional.empty();
		}

		@Override
		public int compareTo(Result o) {
			return o.score - score;
		}

		@Override
		public String toString() {
			return "(" + move.map(move -> move.toString()).orElse("Noop") + ", " + score + ")";
		}
	}

	/**
	 * For a |move| on |currentState|, calculate "worst-case" [min] behavior
	 * of opposing players.
	 * @throws Game definition exceptions if GDL specification is malformed.
	 */
	private Result minR(final MachineState currentState, final Move move, int depth) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine machine = getStateMachine();
        List<Result> results = new ArrayList<Result>();
        for (List<Move> jointMove : machine.getLegalJointMoves(currentState, getRole(), move)) {
            MachineState state = machine.getNextState(currentState, jointMove);
            results.add(maxR(state, depth - 1));
        }
        // Propagate score along with current move.
        return new Result(Collections.min(results).score, move);
    }

	/**
	 * For a |move| on |currentState|, calculate best [max] action, given
	 * worst-case behavior of opposing players.
	 * @throws Game definition exceptions if GDL specification is malformed.
	 */
	private Result maxR(final MachineState currentState, int depth) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.isTerminal(currentState)) {
			return new Result(machine.getGoal(currentState, getRole()));
		} else if (depth == 0) {
			return new Result(this.heuristic.apply(currentState));
		}
        List<Result> my_results = new ArrayList<Result>();
		for (Move move : machine.getLegalMoves(currentState, getRole())) {
			my_results.add(minR(currentState, move, depth));
		}
		return Collections.max(my_results);
	}

	private Move chooseMiniMaxMove() throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		Result m = maxR(getCurrentState(), MAX_DEPTH);
		System.out.println("MOVE " + m.toString() + " by " + getRole().toString());
		return m.move.get();
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = chooseMiniMaxMove();

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
