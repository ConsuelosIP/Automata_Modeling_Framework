package amf.gui.parts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ShortestPathConnectionRouter;
import org.eclipse.draw2d.geometry.Rectangle;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;

 
import amf.model.Attribute;
import amf.model.AttributeProvdier;
import amf.model.Automaton;
import amf.model.BranchNode;
import amf.model.EndingNode;
import amf.model.ModelElement;
import amf.model.MovableElement;
import amf.model.Node;
import amf.model.StartingNode;
import amf.model.StateNode;
import amf.model.commands.NodeCreateCommand;
import amf.model.commands.NodeMoveCommand;

/**
 * EditPart for the a ShapesDiagram instance.
 * <p>
 * This edit part server as the main diagram container, the white area where
 * everything else is in. Also responsible for the container's layout (the way
 * the container rearanges is contents) and the container's capabilities (edit
 * policies).
 * </p>
 * <p>
 * This edit part must implement the PropertyChangeListener interface, so it can
 * be notified of property changes in the corresponding model element.
 * </p>

 */
class DiagramEditPart extends AbstractGraphicalEditPart implements
		PropertyChangeListener {

	/**
	 * Upon activation, attach to the model element as a property change
	 * listener.
	 */
	public void activate() {
		if (!isActive()) {
			super.activate();
			((ModelElement) getModel()).addPropertyChangeListener(this);
		}
	}

	protected void createEditPolicies() {
		// disallows the removal of this edit part from its parent
		installEditPolicy(EditPolicy.COMPONENT_ROLE,
				new RootComponentEditPolicy());
		// handles constraint changes (e.g. moving and/or resizing) of model
		// elements
		// and creation of new model elements
		installEditPolicy(EditPolicy.LAYOUT_ROLE,
				new ShapesXYLayoutEditPolicy());
	}

	protected IFigure createFigure() {
		Figure f = new FreeformLayer();
		f.setBorder(new MarginBorder(3));
		f.setLayoutManager(new FreeformLayout());

		// Create the static router for the connection layer
		ConnectionLayer connLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
		connLayer.setConnectionRouter(new ShortestPathConnectionRouter(f));

		return f;
	}

	/**
	 * Upon deactivation, detach from the model element as a property change
	 * listener.
	 */
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			((ModelElement) getModel()).removePropertyChangeListener(this);
		}
	}

	private Automaton getCastedModel() {
		return (Automaton) getModel();
	}

	 
	protected List getModelChildren() {
		List nodes = getCastedModel().getChildren();
		List<ModelElement> result = new ArrayList<ModelElement>();		
		result.addAll(nodes);		
		for (int i=0; i<nodes.size(); i++)
		{
			// return all attributes of the nodes
			if (nodes.get(i) instanceof AttributeProvdier)
			{				
				AttributeProvdier ap = (AttributeProvdier) nodes.get(i);
				result.addAll(ap.getAttributes());				
			}
			// return all attributes of the incoming transitions of the nodes
			if (nodes.get(i) instanceof Node)
			{				
				Node n = (Node) nodes.get(i);
				for (int j=0; j< n.getSourceTransitions().size(); j++)
				{
					AttributeProvdier p = (AttributeProvdier) n.getSourceTransitions().get(j);
					result.addAll(p.getAttributes());				
				}								
			}			
		}		
		return result;
		
	}

	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		// these properties are fired when Shapes are added into or removed from
		// the ShapeDiagram instance and must cause a call of refreshChildren()
		// to update the diagram's contents.
		if (Automaton.CHILD_ADDED_PROP.equals(prop)
				|| Automaton.CHILD_REMOVED_PROP.equals(prop)) {
			refreshChildren();
		}
	}
	public void refreshChildren()
	{
		super.refreshChildren();
	}

	private static class ShapesXYLayoutEditPolicy extends XYLayoutEditPolicy {

		protected Command createChangeConstraintCommand(
				ChangeBoundsRequest request, EditPart child, Object constraint) {
			if (
					(child instanceof ShapeEditPart
					|| child instanceof AttributeEditPart
					)&& constraint instanceof Rectangle
					) {
				// return a command that can move and/or resize a Shape
				return new NodeMoveCommand((MovableElement) child.getModel(),
						request, (Rectangle) constraint);
			}
 
			return super.createChangeConstraintCommand(request, child,
					constraint);
		}

		protected Command createChangeConstraintCommand(EditPart child, Object constraint) {
			// not used in this example
			return null;
		}

		protected Command getCreateCommand(CreateRequest request) {
			Object childClass = request.getNewObjectType();
			if ((childClass == StateNode.class) ||
					(childClass == StartingNode.class) ||
					(childClass == EndingNode.class) ||
					(childClass == BranchNode.class)) {
				// return a command that can add a Shape to a ShapesDiagram
				return new NodeCreateCommand((Node) request.getNewObject(),
						(Automaton) getHost().getModel(),
						(Rectangle) getConstraintFor(request));
			} 
			return null;
		}
	}

}
