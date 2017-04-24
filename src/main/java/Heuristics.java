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

	/**
	 * Implement a fixed-depth search player with a mobility heuristic.
	 * (Given a game that is not able to search completely, your player should favor
	 * moves that leave it with the most options.) There are multiple ways this can
	 * be done, e.g. one step mobility, n-step mobility, number of reachable states,
	 * number of legal actions. You may pick whichever of these you like. Alternatively,
	 * implement a focus heuristic. (Given a game that is not able to search completely,
	 * your player should favor moves that leave it with the fewest options.)
	 */
	public static int mobility(StateMachineGamer gamerState) {
		return 0;
	}

	/**
	 * Implement a fixed-depth search player using a goal proximity heuristic. To
	 * measure goal proximity, try using the goal value of the current state or (harder)
	 * try to find "winning" terminal states and use similarity to these states as a
	 * measure of goal proximity.
	 */
	public static int goalProximity(StateMachineGamer gamerState) {
		return 0;
	}

	/**
	 * Implement an opponent mobility heuristic or an opponent focus heuristic.
	 * Try your player out on a standard game of your choosing.
	 */
	public static int enemyMobility(StateMachineGamer gamerState) {
		return 0;
	}

	/**
	 * Implement a method for evaluating moves based on a weighted combination
	 * of other heuristics. Use this together with your various mobility and focus
	 * heuristics. Once your player is ready to go, click on the link below to test
	 * it out. Try changing the weights and see what happens.
	 */
	public static int combination(StateMachineGamer gamerStaet) {
		return 0;
	}
}