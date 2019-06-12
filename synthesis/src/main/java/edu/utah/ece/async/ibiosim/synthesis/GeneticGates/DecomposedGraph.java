package edu.utah.ece.async.ibiosim.synthesis.GeneticGates;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.sbolstandard.core2.FunctionalComponent;

import com.google.common.collect.Lists;

import edu.utah.ece.async.ibiosim.synthesis.GeneticGates.DecomposedGraphNode.NodeInteractionType;


/**
 * Create a DecomposedGraph from an SBOL document describing a genetic circuit made of NOT and NOR logic. 
 * 
 * @author Tramy Nguyen
 *
 */
public class DecomposedGraph {


	private DecomposedGraphNode outputNode;
	private List<DecomposedGraphNode> nodeList, leafNodeList;
	
	public DecomposedGraph() {
		nodeList = new ArrayList<>();
		leafNodeList = new ArrayList<>();
	}
	
	public void setNodeAsOutput(DecomposedGraphNode node) {
		this.outputNode = node;
	}
	
	public void setNodeAsLeaf(DecomposedGraphNode node) {
		if(leafNodeList.contains(node)) {
			return;
		}
		leafNodeList.add(node);
	}
	
	public List<DecomposedGraphNode> getLeafNodes(){
		return this.leafNodeList;
	}
	
	public DecomposedGraphNode getOutputNode() {
		return this.outputNode;
	}

	public void addNodeRelationship(DecomposedGraphNode parent, DecomposedGraphNode child, NodeInteractionType interactionType) {
		parent.childrenNodeList.put(child, interactionType);
		child.parentNodeList.put(parent, interactionType);
	}
	
	public void addAllNodes(DecomposedGraphNode... nodes) {
		for(DecomposedGraphNode n : nodes) {
			addNode(n);
		}
	}
	
	
	public void addNode(DecomposedGraphNode node) {
		if(!nodeList.contains(node)) { 
			nodeList.add(node);
		}
	}
	
	/**
	 * Sort from bottom leaf nodes to root nodes
	 * @return Nodes sorted in topological order
	 */
	public List<DecomposedGraphNode> topologicalSort()
	{
		List<DecomposedGraphNode> sortedElements = new ArrayList<DecomposedGraphNode>();
		Queue<DecomposedGraphNode> unsortedElements = new LinkedList<DecomposedGraphNode>();
		unsortedElements.addAll(leafNodeList);

		while(!unsortedElements.isEmpty())
		{
			DecomposedGraphNode currentUnsortedNode = unsortedElements.poll();
			if(sortedElements.contains(currentUnsortedNode))
				continue;
			sortedElements.add(currentUnsortedNode);
			for(DecomposedGraphNode currentUnsortedParentNode : currentUnsortedNode.parentNodeList.keySet()) {
				if(currentUnsortedParentNode.childrenNodeList.size() == 1) {
					unsortedElements.add(currentUnsortedParentNode);
					break;
				}
				else if(currentUnsortedParentNode.childrenNodeList.size() == 2){
					List<DecomposedGraphNode> childrenNodes = Lists.newArrayList(currentUnsortedParentNode.childrenNodeList.keySet());
					DecomposedGraphNode child1 = childrenNodes.get(0);
					DecomposedGraphNode child2 = childrenNodes.get(1);

					if(sortedElements.containsAll(childrenNodes)) {
						unsortedElements.add(currentUnsortedParentNode);
					}
					else if(childrenNodes.contains(currentUnsortedNode)) {
						DecomposedGraphNode temp = null;
						if(sortedElements.contains(child1) && !sortedElements.contains(child2)) {
							temp = child2;
						}
						else if(sortedElements.contains(child2) && !sortedElements.contains(child1)){
							temp = child1;
						}
						
						if(unsortedElements.contains(temp)) {
							sortedElements.add(temp);
							unsortedElements.add(currentUnsortedParentNode);
						}
						else {
							unsortedElements.add(currentUnsortedParentNode);
						}
						

					}
					else {
						unsortedElements.add(currentUnsortedNode);
					}
				}
			} 
		} 
		return sortedElements;
	}
	
	public DecomposedGraphNode getNode(FunctionalComponent fc) {
		List<DecomposedGraphNode> nodes = new ArrayList<>();
		for(DecomposedGraphNode n : nodeList) {
			if(n.getComponentDefinition().isPresent() && n.getComponentDefinition().get().equals(fc.getDefinition())) {
				nodes.add(n); 
			}
		}
		assert(nodes.size() == 1);
		return nodes.get(0);
	}
	
	public DecomposedGraphNode getNode(URI functionalComponentUri) {
		List<DecomposedGraphNode> nodes = new ArrayList<>();
		for(DecomposedGraphNode n : nodeList) {
			if(n.getFunctionalComponent().isPresent() && n.getFunctionalComponent().get().getIdentity().equals(functionalComponentUri)) {
				nodes.add(n); 
			}
		}
		assert(nodes.size() == 1);
		return nodes.get(0);
	}	
}
