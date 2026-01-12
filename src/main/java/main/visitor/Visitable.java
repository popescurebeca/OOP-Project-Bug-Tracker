package main.visitor;

/**
 * Interface for elements that can be visited by a Visitor.
 */
public interface Visitable {
    /**
     * Accepts a visitor.
     *
     * @param v The visitor instance.
     */
    void accept(Visitor v);
}
