import java.util.List;

/**
 * Util class for random stuff that should be in standard libraries but isn't.
 */
public class Util {

	/**
	 * Generic min/max functions for Comparable.
	 */

    public static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) > 0 ? a : b;
    }

    public static <T extends Comparable<T>> T min(T a, T b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

	public static <T> T chooseRandom(List<T> list) {
		return list.get((int)(Math.random() * list.size()));
	}

}