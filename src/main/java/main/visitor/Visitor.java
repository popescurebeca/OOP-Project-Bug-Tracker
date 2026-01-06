package main.visitor;

import main.model.ticket.Bug;
import main.model.ticket.FeatureRequest;
import main.model.ticket.UIFeedback;

public interface Visitor {
    void visit(Bug bug);
    void visit(FeatureRequest featureRequest);
    void visit(UIFeedback uiFeedback);
}