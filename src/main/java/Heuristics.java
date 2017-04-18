import org.ggp.base.util.statemachine.MachineState;

public class Heuristics {
	public interface Heuristic {
		public int evaluate(MachineState state);
	}

	public static class Dumb implements Heuristic {
		@Override
		public int evaluate(MachineState state) {
			return 50;
		}
	}

	public static Heuristic getDefault() {
		return new Dumb();
	}
}
