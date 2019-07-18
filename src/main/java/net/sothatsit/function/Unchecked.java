package net.sothatsit.function;

/**
 * A class to hide warnings.
 *
 * @author Paddy Lamont
 */
public class Unchecked {

    @SuppressWarnings("unchecked")
    public static <V> V cast(Object object) {
        return (V) object;
    }
}
