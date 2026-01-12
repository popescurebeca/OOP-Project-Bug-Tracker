package main.visitor;

import main.model.ticket.Bug;
import main.model.ticket.FeatureRequest;
import main.model.ticket.UIFeedback;

/**
 * Interface for the Visitor pattern.
 * Defines visit methods for different ticket types.
 */
public interface Visitor {
    /**
     * Visit a Bug ticket.
     *
     * @param bug The bug ticket to visit.
     */
    void visit(Bug bug);

    /**
     * Visit a Feature Request ticket.
     *
     * @param featureRequest The feature request ticket to visit.
     */
    void visit(FeatureRequest featureRequest);

    /**
     * Visit a UI Feedback ticket.
     *
     * @param uiFeedback The UI feedback ticket to visit.
     */
    void visit(UIFeedback uiFeedback);
}
