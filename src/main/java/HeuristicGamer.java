import java.util.function.Function;

import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;

public abstract class HeuristicGamer extends SampleGamer {
	protected abstract Function<StateMachineGamer, Integer> getHeuristic();

	protected int evaluate() {
		return getHeuristic().apply(this);
	}
}