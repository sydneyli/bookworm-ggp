import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;

/**
 * Manager for heuristics. May eventually want to record
 * state.
 */
public class Heuristics {
	// Dumb sample heuristic (Problem 1)
	public static int dumb(StateMachineGamer gamerState) throws GoalDefinitionException {
		StateMachine stateMachine = gamerState.getStateMachine();
		MachineState currentState = gamerState.getCurrentState();
		if (stateMachine.isTerminal(currentState)) {
			return 0;
		} else {
			return stateMachine.getGoal(currentState, gamerState.getRole());
		}
	}
}