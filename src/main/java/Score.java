import java.util.Optional;

import org.ggp.base.util.statemachine.Move;

/**
 * Immutable struct to potentially associate a Move with a score.
 */
public class Score implements Comparable<Score> {
    public final int value;
    public final Optional<Move> move;
    public Score(int value, Move move) {
        this.value = value;
        this.move = Optional.of(move);
    }

    public Score(int value) {
        this.value = value;
        this.move = Optional.empty();
    }

    @Override
    public int compareTo(Score o) {
        return Integer.compare(value, o.value);
    }

}
