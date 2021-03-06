package ru.runa.gpd.lang.model.bpmn;

import java.util.List;

import ru.runa.gpd.lang.ValidationError;
import ru.runa.gpd.lang.model.IReceiveMessageNode;
import ru.runa.gpd.lang.model.Timer;

public class CatchEventNode extends AbstractEventNode implements IReceiveMessageNode, IBoundaryEvent, IBoundaryEventContainer {

    @Override
    public Timer getTimer() {
        return getFirstChild(Timer.class);
    }

    @Override
    protected void validateOnEmptyRules(List<ValidationError> errors) {
        if (getEventNodeType() == EventNodeType.error && getParent() instanceof IBoundaryEventContainer) {
            return;
        }
        super.validateOnEmptyRules(errors);
    }
}
