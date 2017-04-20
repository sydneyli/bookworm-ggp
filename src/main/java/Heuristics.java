import java.util.function.Function;

import org.ggp.base.util.statemachine.MachineState;

/**
 * Manager for heuristics. May eventually want to record
 * state.
 */
public class Heuristics {
	// Stateless heuristics
	public static Function<MachineState, Integer> dumb() {
		return state -> 50;
	}
}