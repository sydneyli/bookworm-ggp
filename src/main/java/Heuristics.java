import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

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
			return stateMachine.getGoal(currentState, gamerState.getRole()); //changed
		} else {
			return 0;
		}
	}

	/**
	 * Implement a fixed-depth search player with a mobility heuristic.
	 * (Given a game that is not able to search completely, your player should favor
	 * moves that leave it with the most options.) There are multiple ways this can
	 * be done, e.g. one step mobility, n-step mobility, number of reachable states,
	 * number of legal actions. You may pick whichever of these you like.
	 * @throws MoveDefinitionException
	 * @throws TransitionDefinitionException
	 */
	public static int mobility(StateMachineGamer gamerState) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		StateMachine stateMachine = gamerState.getStateMachine();
		MachineState currentState = gamerState.getCurrentState();
		if (stateMachine.isTerminal(currentState)) {
			return stateMachine.getGoal(currentState, gamerState.getRole());
		} else {
			int nActions = stateMachine.getLegalMoves(currentState, gamerState.getRole()).size();
			int nTotalActions = stateMachine.findActions(gamerState.getRole()).size();
			return (int)((double)nActions / nTotalActions * 100);
		}
	}

	/**
	 * Implement a focus heuristic. (Given a game that is not able to search completely,
	 * your player should favor moves that leave it with the fewest options.)
	 */
	public static int focus(StateMachineGamer gamerState) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{
		return 100 - mobility(gamerState);
	}

	/**
	 * Implement a fixed-depth search player using a goal proximity heuristic. To
	 * measure goal proximity, try using the goal value of the current state or (harder)
	 * try to find "winning" terminal states and use similarity to these states as a
	 * measure of goal proximity.

	 * @throws GoalDefinitionException
	 */
	public static int goalProximity(StateMachineGamer gamerState) throws GoalDefinitionException {
		StateMachine stateMachine = gamerState.getStateMachine();
		MachineState currentState = gamerState.getCurrentState();
		return stateMachine.getGoal(currentState,gamerState.getRole());
	}


	/**
	 * Implement an opponent mobility heuristic or an opponent focus heuristic.
	 * Try your player out on a standard game of your choosing.
	 */
	public static int enemyMobility(StateMachineGamer gamerState) throws GoalDefinitionException, MoveDefinitionException {
		StateMachine stateMachine = gamerState.getStateMachine();
		MachineState currentState = gamerState.getCurrentState();
		if (stateMachine.isTerminal(currentState)) {
			return stateMachine.getGoal(currentState, gamerState.getRole());
		} else {
			double numLegal = 1.0;
			for (Role r: stateMachine.getRoles()) {
				if (r.equals(gamerState.getRole())) continue;
				numLegal *= stateMachine.getLegalMoves(currentState, r).size();
				numLegal /= stateMachine.findActions(r).size();
			}
			numLegal *= 100.0;
			return (int) numLegal;
		}
	}

	/**
	 * Implement an opponent mobility heuristic or an opponent focus heuristic.
	 * Try your player out on a standard game of your choosing.
	 */
	public static int enemyFocus(StateMachineGamer gamerState) throws GoalDefinitionException, MoveDefinitionException {
		return 100 - enemyMobility(gamerState);
	}


	/**
	 * Implement a method for evaluating moves based on a weighted combination
	 * of other heuristics. Use this together with your various mobility and focus
	 * heuristics. Once your player is ready to go, click on the link below to test
	 * it out. Try changing the weights and see what happens.
	 * @throws TransitionDefinitionException
	 * @throws MoveDefinitionException
	 * @throws GoalDefinitionException
	 */
	public static int combination(StateMachineGamer gamerState) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		double mobilityWeight = 0.1;
		double goalProximityWeight = 0.5;
		double enemyMobilityWeight = 0.4;
		return (int) (mobilityWeight * mobility(gamerState)
				+ goalProximityWeight * goalProximity(gamerState)
				+ enemyMobilityWeight * enemyMobility(gamerState));
	}


	/**
	 * Implement a player that uses the Monte Carlo Search technique.
	 * @throws GoalDefinitionException
	 * @throws MoveDefinitionException
	 * @throws TransitionDefinitionException
	 */

	public static int monteCarlo(StateMachineGamer gamerState) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		int probes = 4;
		StateMachine stateMachine = gamerState.getStateMachine();
		MachineState currentState = gamerState.getCurrentState();
		double total = 0;
		int[] theDepth = new int[1];
		for (int i = 0; i < probes; i++) {
			MachineState terminal = stateMachine.performDepthCharge(currentState, theDepth);
			total = total + stateMachine.getGoal(terminal, gamerState.getRole());
		}
		return (int) total/probes;
	}


}