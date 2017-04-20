import org.ggp.base.player.gamer.statemachine.StateMachineGamer;

/**
 * Manager for heuristics. May eventually want to record
 * state.
 */
public class Heuristics {
	// Dumb sample heuristic
	public static int dumb(StateMachineGamer gamerState) {
		return 50;
	}
}