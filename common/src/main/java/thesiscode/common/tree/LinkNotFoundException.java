package thesiscode.common.tree;

/**
 * Thrown if a link is not found by a {@link AbstractTreeAlgorithm}.
 */
public class LinkNotFoundException extends Exception {
    public LinkNotFoundException() {
        super();
    }

    public LinkNotFoundException(String format) {
        super(format);
    }
}
