package sbol;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import main.Gui;

import org.sbolstandard.core.*;
import org.sbolstandard.core.impl.*;
import org.sbolstandard.core.util.SequenceOntology;

import biomodel.util.GlobalConstants;

public class SBOLAssembler {
	
	private SBOLAssemblyGraph assemblyGraph;
	private SequenceTypeValidator seqValidator;
//	boolean validate = Preferences.userRoot().get(GlobalConstants.CONSTRUCT_VALIDATION_PREFERENCE, "").equals("True");
	
	public SBOLAssembler(SBOLAssemblyGraph assemblyGraph, SequenceTypeValidator seqValidator) {
		this.assemblyGraph = assemblyGraph;
		this.seqValidator = seqValidator;
	}
	
	public DnaComponent exportDnaComponent(String exportFilePath, String saveDirectory) {
		DnaComponent assemblyComp = null;
		SBOLDocument sbolDoc = SBOLFactory.createDocument();
		SBOLUtility.addDNAComponent(assemblyComp, sbolDoc, false);
		SBOLUtility.writeSBOLDocument(exportFilePath, sbolDoc);
		return assemblyComp;
	}
	
	public DnaComponent assembleDNAComponent() {	
		// Orders list of subcomponents (to be assembled into composite component) 
		// by walking synthesis nodes
		List<SBOLAssemblyNode> orderedNodes = orderAssemblyNodes(new LinkedList<SBOLAssemblyNode>(assemblyGraph.getStartNodes()), 
				new LinkedList<SBOLAssemblyNode>(), new LinkedList<SBOLAssemblyNode>(), new LinkedList<SBOLAssemblyNode>(), 
				new HashSet<SBOLAssemblyNode>(), new HashSet<SBOLAssemblyNode>());
	
		if (assemblyGraph.getFlatGraph() != null) {
			SBOLAssemblyGraph temp = assemblyGraph;
			assemblyGraph = assemblyGraph.getFlatGraph();
			List<SBOLAssemblyNode> flatOrderedNodes = orderAssemblyNodes(new LinkedList<SBOLAssemblyNode>(assemblyGraph.getStartNodes()), 
					new LinkedList<SBOLAssemblyNode>(), new LinkedList<SBOLAssemblyNode>(), new LinkedList<SBOLAssemblyNode>(), 
					new HashSet<SBOLAssemblyNode>(), new HashSet<SBOLAssemblyNode>());
			assemblyGraph = temp;
			if (orderedNodes != null && SBOLUtility.loadNodeTypes(orderedNodes).size() > 0 
					&& flatOrderedNodes != null && SBOLUtility.loadNodeTypes(flatOrderedNodes).size() > 0)
				orderedNodes = compareOrderedNodes(orderedNodes, flatOrderedNodes);
			else
				orderedNodes = flatOrderedNodes;
		}
//		assemblyGraph.print(orderedNodes);
		if (orderedNodes == null || SBOLUtility.loadNodeTypes(orderedNodes).size() == 0) {
			JOptionPane.showMessageDialog(Gui.frame, "Failed to assemble composite DNA component with valid ordering of sequence types among its subcomponents.\n" +
					"(No orderings matching regular expressions for complete or partial genetic constructs were found.)", 
					"No Valid Sequence Type Order", JOptionPane.ERROR_MESSAGE);
			return null;
		} else { 
			// Create composite component and its sequence
			DnaComponent assembledComp = new DnaComponentImpl();
			assembledComp.addType(SequenceOntology.type("SO_0000804"));
			DnaSequence synthSeq = new DnaSequenceImpl();
			synthSeq.setNucleotides("");
			assembledComp.setDnaSequence(synthSeq);
			
			int position = 1;
			LinkedList<String> subCompTypes = new LinkedList<String>();
			for (SBOLAssemblyNode assemblyNode : orderedNodes) {
				List<DnaComponent> subComps = assemblyNode.getDNAComponents();
				if (assemblyNode.getStrand().equals(GlobalConstants.SBOL_ASSEMBLY_MINUS_STRAND))
					for (int i = subComps.size() - 1; i >= 0; i--) {
						position = addSubComponent(position, subComps.get(i), assembledComp, assemblyNode.getStrand());
						if (position == -1)
							return null;
						subCompTypes.addAll(0, SBOLUtility.loadDNAComponentTypes(subComps.get(i)));
					}
				else
					for (int i = 0; i < subComps.size(); i++) {
						position = addSubComponent(position, subComps.get(i), assembledComp, assemblyNode.getStrand());
						if (position == -1)
							return null;
						subCompTypes.addAll(SBOLUtility.loadDNAComponentTypes(subComps.get(i)));
					}
			}
			if (Preferences.userRoot().get(GlobalConstants.CONSTRUCT_VALIDATION_PREFERENCE, "").equals("True") &&
					Preferences.userRoot().get(GlobalConstants.CONSTRUCT_VALIDATION_WARNING_PREFERENCE, "").equals("True") &&
					!seqValidator.validateCompleteConstruct(subCompTypes, false)) {
				Object[] options = { "OK", "Cancel" };
				int choice = JOptionPane.showOptionDialog(null, "Assembled Composite DNA component has incomplete ordering of sequence types among its subcomponents.\n" +
						"(Ordering does not match preferred regular expression for complete genetic construct.)", 
						"Incomplete Sequence Type Order", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
				if (choice != JOptionPane.OK_OPTION)
					return null;
			}
			return assembledComp;
		}
	}
	
	private List<SBOLAssemblyNode> compareOrderedNodes(List<SBOLAssemblyNode> orderedNodes, List<SBOLAssemblyNode> flatOrderedNodes) {
		seqValidator.resetTerminalConstructValidator();
		List<List<SBOLAssemblyNode>> constructs = new LinkedList<List<SBOLAssemblyNode>>();
		int i = 0;
		while (i < orderedNodes.size()) {
			List<SBOLAssemblyNode> construct = new LinkedList<SBOLAssemblyNode>();
			int j = i;
			seqValidator.validateTerminalConstruct(SBOLUtility.loadNodeTypes(orderedNodes.get(j)), true);
			List<String> nextNodeTypes;
			do {
				construct.add(orderedNodes.get(j));
				seqValidator.validateTerminalConstruct(SBOLUtility.loadNodeTypes(orderedNodes.get(j)), true);
				j++;
				if (j < orderedNodes.size()) {
					nextNodeTypes = SBOLUtility.loadNodeTypes(orderedNodes.get(j));
					if (nextNodeTypes.size() > 1)
						nextNodeTypes = nextNodeTypes.subList(0, 1);
				} else 
					nextNodeTypes = new LinkedList<String>();
			} while (j < orderedNodes.size() 
					&& (!seqValidator.isTerminalConstructValid()
					|| seqValidator.validateTerminalConstruct(nextNodeTypes, false)));
			constructs.add(construct);
			i = j;
		}
		seqValidator.resetTerminalConstructValidator();
		List<List<SBOLAssemblyNode>> flatConstructs = new LinkedList<List<SBOLAssemblyNode>>();
		i = 0;
		while (i < flatOrderedNodes.size()) {
			List<SBOLAssemblyNode> flatConstruct = new LinkedList<SBOLAssemblyNode>();
			int j = i;
			seqValidator.validateTerminalConstruct(SBOLUtility.loadNodeTypes(flatOrderedNodes.get(j)), true);
			List<String> nextNodeTypes;
			do {
				flatConstruct.add(flatOrderedNodes.get(j));
				seqValidator.validateTerminalConstruct(SBOLUtility.loadNodeTypes(flatOrderedNodes.get(j)), true);
				j++;
				if (j < flatOrderedNodes.size()) {
					nextNodeTypes = SBOLUtility.loadNodeTypes(flatOrderedNodes.get(j));
					if (nextNodeTypes.size() > 1)
						nextNodeTypes = nextNodeTypes.subList(0, 1);
				} else 
					nextNodeTypes = new LinkedList<String>();
			} while (j < flatOrderedNodes.size() 
					&& (!seqValidator.isTerminalConstructValid()
					|| seqValidator.validateTerminalConstruct(nextNodeTypes, false)));
			flatConstructs.add(flatConstruct);
			i = j;
		}
		int initialJ = 0;
		if (constructs.size() > 1 && !seqValidator.validateCompleteConstruct(SBOLUtility.loadNodeTypes(constructs.get(initialJ)), false))
			initialJ++;
		int finalJ = constructs.size();
		if (constructs.size() > 1 && !seqValidator.validateCompleteConstruct(SBOLUtility.loadNodeTypes(constructs.get(finalJ - 1)), false))
			finalJ--;
		String flatConstructURISig = "";
		for (List<SBOLAssemblyNode> flatConstruct : flatConstructs) {
			List<URI> flatConstructURIs = SBOLUtility.loadNodeURIs(flatConstruct);
			for (URI flatConstructURI : flatConstructURIs)
				flatConstructURISig = flatConstructURISig + flatConstructURI.toString();
		}
		for (int j = initialJ; j < finalJ; j++) 
			for (int k = j; k < constructs.size(); k++) {
				constructs.add(finalJ - 1, constructs.remove(j));
				String constructURISig = "";
				for (List<SBOLAssemblyNode> construct : constructs) {
					List<URI> constructURIs = SBOLUtility.loadNodeURIs(construct);
					for (URI constructURI : constructURIs)
						constructURISig = constructURISig + constructURI.toString();
				}
				if (constructURISig.equals(flatConstructURISig))
					return orderedNodes;
			}
		return flatOrderedNodes;
	}
	
	private int addSubComponent(int position, DnaComponent subComp, DnaComponent parentComp, String strand) {	
		if (subComp.getDnaSequence() != null && subComp.getDnaSequence().getNucleotides() != null 
				&& subComp.getDnaSequence().getNucleotides().length() >= 1) {
			SequenceAnnotation annot = new SequenceAnnotationImpl();
			annot.setBioStart(position);
			position += subComp.getDnaSequence().getNucleotides().length() - 1;
			annot.setBioEnd(position);
			if (strand.equals(GlobalConstants.SBOL_ASSEMBLY_MINUS_STRAND))
				annot.setStrand(StrandType.NEGATIVE);
			else
				annot.setStrand(StrandType.POSITIVE);
			annot.setSubComponent(subComp);
			parentComp.addAnnotation(annot);
			position++;
			DnaSequenceImpl subSeq = (DnaSequenceImpl) subComp.getDnaSequence();
			if (strand.equals(GlobalConstants.SBOL_ASSEMBLY_MINUS_STRAND))
				parentComp.getDnaSequence().setNucleotides(parentComp.getDnaSequence().getNucleotides() 
					+ subSeq.getReverseComplementaryNucleotides());
			else
				parentComp.getDnaSequence().setNucleotides(parentComp.getDnaSequence().getNucleotides() 
						+ subSeq.getNucleotides());
		} else {
			JOptionPane.showMessageDialog(Gui.frame, "DNA Component " + subComp.getDisplayId() + " has no DNA sequence.", 
					"Missing DNA Sequence", JOptionPane.ERROR_MESSAGE);
			return -1;
		}	
		return position;
	}
	
	private List<SBOLAssemblyNode> orderAssemblyNodes(List<SBOLAssemblyNode> startNodes, List<SBOLAssemblyNode> currentNodes, 
			List<SBOLAssemblyNode> walkNodes, List<SBOLAssemblyNode> orderedNodes, 
			Set<SBOLAssemblyNode> localVisitedNodes, Set<SBOLAssemblyNode> globalVisitedNodes) {
		while (startNodes.size() > 0) {
			if (currentNodes.size() == 0)
				currentNodes.add(startNodes.get(0));
			while (currentNodes.size() > 0) {		
				List<String> currentNodeTypes = SBOLUtility.loadNodeTypes(currentNodes.get(0));
//				currentNodeTypes.size() == 0 || 
				if (seqValidator.validatePartialConstruct(currentNodeTypes, true)) {
					localVisitedNodes.add(currentNodes.get(0));
					seqValidator.validateTerminalConstruct(currentNodeTypes, true);
					List<SBOLAssemblyNode> nextNodes = new LinkedList<SBOLAssemblyNode>();
					for (SBOLAssemblyNode nextNode : assemblyGraph.getNextNodes(currentNodes.get(0))) {
//						Set<SBOLAssemblyNode> previousNodes = new HashSet<SBOLAssemblyNode>(assemblyGraph.getPreviousNodes(nextNode));
//						previousNodes.remove(currentNodes.get(0));
//						if (previousNodes.size() == 0 || (seqValidator.isPartialConstructStarted() && !seqValidator.isTerminalConstructValid()) 
//								|| (globalVisitedNodes.containsAll(previousNodes) && !globalVisitedNodes.contains(nextNode))
//								|| (localVisitedNodes.containsAll(previousNodes) && !localVisitedNodes.contains(nextNode))) {
//							List<String> nextNodeTypes = SBOLUtility.loadNodeTypes(nextNode);
//							if (nextNodeTypes.size() > 1)
//								nextNodeTypes = nextNodeTypes.subList(0, 1);
//							if (nextNodeTypes.size() > 0 && 
//									(!seqValidator.validatePartialConstruct(nextNodeTypes, false) 
//									|| !seqValidator.isPartialConstructStarted() 
//									|| (seqValidator.isTerminalConstructValid() 
//											&& !seqValidator.validateTerminalConstruct(nextNodeTypes, false)))) 
//								startNodes.add(nextNode);
//							else if (!localVisitedNodes.contains(nextNode))
//								nextNodes.add(nextNode);
//						}
						if (!localVisitedNodes.contains(nextNode)) {
							boolean nextValid = false;
							if (seqValidator.isPartialConstructStarted() && !seqValidator.isTerminalConstructValid())
								nextValid = true;
							else if (!globalVisitedNodes.contains(nextNode)) {
								Set<SBOLAssemblyNode> previousNodes = new HashSet<SBOLAssemblyNode>(assemblyGraph.getPreviousNodes(nextNode));
								previousNodes.remove(currentNodes.get(0));
								if (previousNodes.size() == 0 || localVisitedNodes.containsAll(previousNodes)
										|| globalVisitedNodes.containsAll(previousNodes))
									nextValid = true;
							}
							if (nextValid) {
								List<String> nextNodeTypes = SBOLUtility.loadNodeTypes(nextNode);
								if (nextNodeTypes.size() > 2)
									nextNodeTypes = nextNodeTypes.subList(0, 2);
								if (nextNodeTypes.size() > 0 && 
										(!seqValidator.validatePartialConstruct(nextNodeTypes, false) 
										|| !seqValidator.isPartialConstructStarted() 
										|| (seqValidator.isTerminalConstructValid() 
												&& !seqValidator.validateTerminalConstruct(nextNodeTypes, false)))) 
									startNodes.add(nextNode);
								else if (!localVisitedNodes.contains(nextNode))
									nextNodes.add(nextNode);
							}
								
						}
					}
					walkNodes.add(currentNodes.remove(0));
					if (nextNodes.size() == 0)
						while (currentNodes.size() > 0 && localVisitedNodes.contains(currentNodes.get(0)))
							currentNodes.remove(0);
					else if (nextNodes.size() == 1) 
						currentNodes.add(0, nextNodes.get(0));
					else if (nextNodes.size() > 1) {
						for (int i = 0; i < nextNodes.size(); i++) 
							for (int j = i; j < nextNodes.size(); j++) {
								nextNodes.add(nextNodes.remove(i));
								List<SBOLAssemblyNode> copyStartNodes = new LinkedList<SBOLAssemblyNode>(startNodes);
								List<SBOLAssemblyNode> copyCurrentNodes = new LinkedList<SBOLAssemblyNode>(currentNodes);
								copyCurrentNodes.addAll(0, nextNodes);
								List<SBOLAssemblyNode> copyWalkNodes = new LinkedList<SBOLAssemblyNode>(walkNodes);
								List<SBOLAssemblyNode> copyOrderedNodes = new LinkedList<SBOLAssemblyNode>(orderedNodes);
								Set<SBOLAssemblyNode> copyLocalVisitedNodes = new HashSet<SBOLAssemblyNode>(localVisitedNodes);
								Set<SBOLAssemblyNode> copyGlobalVisitedNodes = new HashSet<SBOLAssemblyNode>(globalVisitedNodes);
								seqValidator.savePartialConstructValidator();
								seqValidator.saveTerminalConstructValidator();
								List<SBOLAssemblyNode> branchSolution = orderAssemblyNodes(copyStartNodes, copyCurrentNodes,
										copyWalkNodes, copyOrderedNodes, copyLocalVisitedNodes, copyGlobalVisitedNodes);
								if (branchSolution == null) {
									seqValidator.loadPartialConstructValidator();
									seqValidator.loadTerminalConstructValidator();
								} else
									return branchSolution;
							} 
						return null;
					}
				} else {
//					JOptionPane.showMessageDialog(Gui.frame, "Composite DNA component assembled from model has invalid ordering of sequence types among its subcomponents.\n" +
//					"(Ordering does not match regular expression for complete or partial genetic construct.)", 
//					"Invalid Sequence Type Order", JOptionPane.ERROR_MESSAGE);
					return null;
				}
			} 
			seqValidator.resetTerminalConstructValidator();
			seqValidator.resetPartialConstructValidator();
			List<String> walkNodeTypes = SBOLUtility.loadNodeTypes(walkNodes);
			List<String> orderedNodeTypes = SBOLUtility.loadNodeTypes(orderedNodes);
			if (seqValidator.validateStartConstruct(walkNodeTypes, false) 
					&& seqValidator.validateTerminalConstruct(orderedNodeTypes, false))
				orderedNodes.addAll(walkNodes);
			else if (seqValidator.validateTerminalConstruct(walkNodeTypes, false)
					&& seqValidator.validateStartConstruct(orderedNodeTypes, false))
				orderedNodes.addAll(0, walkNodes);
			else if (walkNodeTypes.size() > 0 && orderedNodeTypes.size() > 0) {
//				JOptionPane.showMessageDialog(Gui.frame, "Failed to assemble DNA components associated with model into single sequence\n" +
//						"without introducing potentially unintended component interactions.", 
//						"Invalid SBOL Assembly", JOptionPane.ERROR_MESSAGE);
				return null;
			} else 
				orderedNodes.addAll(walkNodes);
			walkNodes.clear();
			globalVisitedNodes.addAll(localVisitedNodes);
			localVisitedNodes.clear();
			while (startNodes.size() > 0 && globalVisitedNodes.contains(startNodes.get(0)))
				startNodes.remove(0);
		}
		// Order nodes that belong to cycles
		if (globalVisitedNodes.size() < assemblyGraph.size()) {
			Set<SBOLAssemblyNode> cycleNodes = new HashSet<SBOLAssemblyNode>(assemblyGraph.getNodes());
			cycleNodes.removeAll(globalVisitedNodes);
			Set<String> startNodeTypes = seqValidator.getStartTypes();
			for (SBOLAssemblyNode cycleNode : cycleNodes) {
				List<String> cycleNodeTypes = SBOLUtility.loadNodeTypes(cycleNode);
				if (cycleNodeTypes.size() > 1 && startNodeTypes.contains(cycleNodeTypes.get(1))) 
					startNodes.add(cycleNode);
			}
			List<SBOLAssemblyNode> cycleSolution;
			for (SBOLAssemblyNode startNode : startNodes) {
				List<SBOLAssemblyNode> copyStartNode = new LinkedList<SBOLAssemblyNode>();
				copyStartNode.add(startNode);
				Set<SBOLAssemblyNode> copyGlobalVisitedNodes = new HashSet<SBOLAssemblyNode>(globalVisitedNodes);
				List<SBOLAssemblyNode> copyOrderedNodes = new LinkedList<SBOLAssemblyNode>(orderedNodes);
				seqValidator.savePartialConstructValidator();
				seqValidator.saveTerminalConstructValidator();
				cycleSolution = orderAssemblyNodes(copyStartNode, new LinkedList<SBOLAssemblyNode>(), new LinkedList<SBOLAssemblyNode>(),
						copyOrderedNodes, new HashSet<SBOLAssemblyNode>(), copyGlobalVisitedNodes);
				if (cycleSolution == null) {
					seqValidator.loadPartialConstructValidator();
					seqValidator.loadTerminalConstructValidator();
				} else
					return cycleSolution;
			}
			startNodes.clear();
			cycleNodes.removeAll(startNodes);
			startNodes.addAll(cycleNodes);
			for (SBOLAssemblyNode startNode : startNodes) {
				List<SBOLAssemblyNode> copyStartNode = new LinkedList<SBOLAssemblyNode>();
				copyStartNode.add(startNode);
				Set<SBOLAssemblyNode> copyGlobalVisitedNodes = new HashSet<SBOLAssemblyNode>(globalVisitedNodes);
				List<SBOLAssemblyNode> copyOrderedNodes = new LinkedList<SBOLAssemblyNode>(orderedNodes);
				seqValidator.savePartialConstructValidator();
				seqValidator.saveTerminalConstructValidator();
				cycleSolution = orderAssemblyNodes(copyStartNode, new LinkedList<SBOLAssemblyNode>(), new LinkedList<SBOLAssemblyNode>(),
						copyOrderedNodes, new HashSet<SBOLAssemblyNode>(), copyGlobalVisitedNodes);
				if (cycleSolution == null) {
					seqValidator.loadPartialConstructValidator();
					seqValidator.loadTerminalConstructValidator();
				} else
					return cycleSolution;
			}
			return null;
		} else
			return orderedNodes;
	}
	
}
