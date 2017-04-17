import java.util.LinkedList;
import java.util.List;

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

public class CompulsiveDeliberation extends StateMachineGamer {

	@Override
	public String getName() {
		return "Compulsive deliberation";
	}

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
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = bestWin(getCurrentState());

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	public Move bestWin(MachineState s) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		int scoreToBeat = -1;
		Move bestMove = null;

		if (getStateMachine().isTerminal(s)) {
			return null;
		}
		for (Move m: getStateMachine().getLegalMoves(s, getRole())) {
			List<Move> moves = new LinkedList<Move>();
			moves.add(m);
			int score = bestScore(getStateMachine().getNextState(s, moves));
			if (score > scoreToBeat) {
				scoreToBeat = score;
				bestMove = m;
			}
		}
		return bestMove;
	}

	public int bestScore(MachineState s) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		int scoreToBeat = -1;

		if (getStateMachine().isTerminal(s)) {
			return getStateMachine().getGoal(s, getRole());
		}
		for (Move m: getStateMachine().getLegalMoves(s, getRole())) {
			List<Move> moves = new LinkedList<Move>();
			moves.add(m);
			int score = bestScore(getStateMachine().getNextState(s, moves));
			if (score > scoreToBeat) {
				scoreToBeat = score;
			}
		}
		return scoreToBeat;
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

}
