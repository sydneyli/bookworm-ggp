import java.time.Duration;
import java.time.Instant;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSPlayer extends SampleGamer {

	public static final int BUFFER_SECONDS = 4;
	MonteCarloTreeSearch tree;

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		Instant max = Instant.ofEpochMilli(timeout);
		max = max.minus(Duration.ofSeconds(BUFFER_SECONDS));
		Duration searchTime = Duration.between(Instant.now(), max);

		super.stateMachineMetaGame(timeout);
		tree = new MonteCarloTreeSearch(getStateMachine(), getRole());
        tree.search(searchTime);
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		Instant start = Instant.now();
		Instant max = Instant.ofEpochMilli(timeout);
		max = max.minus(Duration.ofSeconds(BUFFER_SECONDS));
		Duration searchTime = Duration.between(Instant.now(), max);

		tree.updateRoot(getCurrentState());
        tree.search(searchTime);
        System.out.println("Selecting move");
		Move selection = tree.chooseMove();
        System.out.println("Done!");

		Instant stop = Instant.now();
		notifyObservers(new GamerSelectedMoveEvent(getStateMachine().getLegalMoves(getCurrentState(), getRole()),
				selection, Duration.between(start, stop).toMillis()));
		return selection;
	}

}
