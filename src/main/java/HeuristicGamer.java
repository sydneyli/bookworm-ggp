import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;

public abstract class HeuristicGamer extends SampleGamer {
	protected abstract int evaluate() throws GoalDefinitionException;
}