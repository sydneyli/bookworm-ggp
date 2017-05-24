import java.time.Duration;
import java.time.Instant;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSPlayer extends SampleGamer {

	public static final int BUFFER_SECONDS = 6;
	MonteCarloTreeSearch tree;

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		Instant max = Instant.ofEpochMilli(timeout);
		max = max.minus(Duration.ofSeconds(BUFFER_SECONDS));

		super.stateMachineMetaGame(timeout);
		tree = new MonteCarloTreeSearch(getStateMachine(), getRole());
		tree.setRoot(getCurrentState());
        while(max.compareTo(Instant.now()) > 0) {
			tree.search();
		}
        tree.printStats();
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		Instant start = Instant.now();
		Instant max = Instant.ofEpochMilli(timeout);
		max = max.minus(Duration.ofSeconds(BUFFER_SECONDS));

		tree.updateRoot(getCurrentState());
        while(max.compareTo(Instant.now()) > 0) {
			tree.search(max);
		}
		Move selection = tree.chooseMove();
        tree.printStats();

		Instant stop = Instant.now();
		notifyObservers(new GamerSelectedMoveEvent(getStateMachine().getLegalMoves(getCurrentState(), getRole()),
				selection, Duration.between(start, stop).toMillis()));
		return selection;
	}

}
