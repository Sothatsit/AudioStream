package net.sothatsit.function;

/**
 * A pair of values.
 *
 * @author Paddy Lamont
 */
public class Pair<A, B> {

    public final A first;
    public final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }
}
