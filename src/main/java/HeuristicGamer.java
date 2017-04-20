import java.util.function.Function;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;

public abstract class HeuristicGamer extends SampleGamer {
	/**
	 * @return
	 */
	protected abstract Function<MachineState, Integer> getHeuristic();

	protected int evaluate(MachineState state) {
		return getHeuristic().apply(state);
	}
}