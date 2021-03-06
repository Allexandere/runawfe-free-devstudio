package ru.runa.gpd.editor.graphiti.create;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import ru.runa.gpd.editor.graphiti.CustomUndoRedoFeature;
import ru.runa.gpd.lang.NodeRegistry;
import ru.runa.gpd.lang.NodeTypeDefinition;
import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.Transition;

public class CreateTransitionFeature extends AbstractCreateConnectionFeature implements CustomUndoRedoFeature {
    private Transition transition;
    private Node sourceNode;
    private Node targetNode;
    private final NodeTypeDefinition transitionDefinition;
    private IFeatureProvider featureProvider;

    public CreateTransitionFeature() {
        super(null, "", "");
        this.transitionDefinition = NodeRegistry.getNodeTypeDefinition(Transition.class);
    }

    public void setFeatureProvider(IFeatureProvider featureProvider) {
        this.featureProvider = featureProvider;
    }

    @Override
    public IFeatureProvider getFeatureProvider() {
        return featureProvider;
    }

    @Override
    public String getCreateName() {
        return transitionDefinition.getLabel();
    }

    @Override
    public String getCreateImageId() {
        return transitionDefinition.getPaletteIcon();
    }

    @Override
    public boolean canStartConnection(ICreateConnectionContext context) {
        Object source = getBusinessObjectForPictogramElement(context.getSourcePictogramElement());
        if (source instanceof Node) {
            Node sourceNode = (Node) source;
            return sourceNode.canAddLeavingTransition();
        }
        return false;
    }

    @Override
    public boolean canCreate(ICreateConnectionContext context) {
        Object source = getBusinessObjectForPictogramElement(context.getSourcePictogramElement());
        Object target = getBusinessObjectForPictogramElement(context.getTargetPictogramElement());
        if (target instanceof Node) {
            return ((Node) target).canAddArrivingTransition((Node) source);
        }
        return false;
    }

    @Override
    public Connection create(ICreateConnectionContext context) {
        sourceNode = (Node) getBusinessObjectForPictogramElement(context.getSourcePictogramElement());
        targetNode = (Node) getBusinessObjectForPictogramElement(context.getTargetPictogramElement());
        // create new business object
        transition = transitionDefinition.createElement(sourceNode, false);
        transition.setTarget(targetNode);
        transition.setName(sourceNode.getNextTransitionName(transitionDefinition));
        sourceNode.addLeavingTransition(transition);
        // add connection for business object
        Anchor sourceAnchor = context.getSourceAnchor();
        if (sourceAnchor == null) {
            sourceAnchor = getChopboxAnchor(context.getSourcePictogramElement());
        }
        Anchor targetAnchor = context.getTargetAnchor();
        if (targetAnchor == null) {
            targetAnchor = getChopboxAnchor(context.getTargetPictogramElement());
        }
        AddConnectionContext addConnectionContext = new AddConnectionContext(sourceAnchor, targetAnchor);
        addConnectionContext.setNewObject(transition);
        return (Connection) getFeatureProvider().addIfPossible(addConnectionContext);
    }

    private Anchor getChopboxAnchor(PictogramElement pe) {
        if (pe instanceof AnchorContainer) {
            Anchor anchor = Graphiti.getPeService().getChopboxAnchor((AnchorContainer) pe);
            if (anchor != null) {
                return anchor;
            }
        }
        if (pe instanceof ContainerShape) {
            for (Shape shape : ((ContainerShape) pe).getChildren()) {
                Anchor anchor = getChopboxAnchor(shape);
                if (anchor != null) {
                    return anchor;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canUndo(IContext context) {
        return transition != null;
    }

    @Override
    public void postUndo(IContext context) {
        transition.getSource().removeLeavingTransition(transition);
    }

    @Override
    public boolean canRedo(IContext context) {
        return transition != null;
    }

    @Override
    public void postRedo(IContext context) {
        if (context instanceof ICreateConnectionContext) {
            sourceNode.addLeavingTransition(transition);
            getDiagramBehavior().refresh();
        }

    }

}
