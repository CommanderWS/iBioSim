package analysis.dynamicsim.hierarchical.simulator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Constraint;
import org.sbml.jsbml.Event;
import org.sbml.jsbml.EventAssignment;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.RateRule;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

import analysis.dynamicsim.hierarchical.util.Setup;

public abstract class HierarchicalSetup extends HierarchicalArrays
{

	public HierarchicalSetup(String SBMLFileName, String rootDirectory, String outputDirectory, int runs, double timeLimit, double maxTimeStep,
			double minTimeStep, JProgressBar progress, double printInterval, double stoichAmpValue, JFrame running, String[] interestingSpecies,
			String quantityType, String abstraction) throws IOException, XMLStreamException
	{
		super(SBMLFileName, rootDirectory, outputDirectory, runs, timeLimit, maxTimeStep, minTimeStep, progress, printInterval, stoichAmpValue,
				running, interestingSpecies, quantityType, abstraction);

	}

	/**
	 * opens output file and seeds rng for new run
	 * 
	 * @param randomSeed
	 * @param currentRun
	 * @throws IOException
	 */
	protected void setupForOutput(int currentRun)
	{
		setCurrentRun(currentRun);

		try
		{

			setTSDWriter(new FileWriter(getOutputDirectory() + "run-" + currentRun + ".tsd"));
			setBufferedTSDWriter(new BufferedWriter(getTSDWriter()));
			getBufferedTSDWriter().write('(');

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	protected void setupCompartments(ModelState modelstate)
	{

		for (Compartment compartment : getModels().get(modelstate.getModel()).getListOfCompartments())
		{
			if (compartment.isSetId() && modelstate.isDeletedBySID(compartment.getId()))
			{
				continue;
			}
			else if (compartment.isSetMetaId() && modelstate.isDeletedByMetaID(compartment.getMetaId()))
			{
				continue;
			}

			Setup.setupSingleCompartment(modelstate, compartment, compartment.getId(), getReplacements());

			setupArrays(modelstate, compartment.getId(), compartment, SetupType.COMPARTMENT);

			setupArrayValue(modelstate, compartment, SetupType.COMPARTMENT);
		}
	}

	/**
	 * puts constraint-related information into data structures
	 */
	protected void setupConstraints(ModelState modelstate)
	{

		int count = 0;

		if (modelstate.getNumConstraints() > 0)
		{
			modelstate.setNoConstraintsFlag(false);
		}

		for (Constraint constraint : getModels().get(modelstate.getModel()).getListOfConstraints())
		{
			if (constraint.isSetMetaId() && modelstate.isDeletedByMetaID(constraint.getMetaId()))
			{
				continue;
			}
			String id = "constraint_" + count++;
			setupArrays(modelstate, id, constraint, SetupType.CONSTRAINT);
			setupArrayObject(modelstate, id, null, constraint, null, SetupType.CONSTRAINT);
			if (!modelstate.isArrayedObject(id))
			{
				Setup.setupSingleConstraint(modelstate, constraint.getMath(), getModels(), getIbiosimFunctionDefinitions());
			}
		}

	}

	/**
	 * puts event-related information into data structures
	 */
	protected void setupEvents(ModelState modelstate)
	{
		long size = getModels().get(modelstate.getModel()).getEventCount();

		for (int i = 0; i < size; i++)
		{

			Event event = getModels().get(modelstate.getModel()).getEvent(i);
			String id = event.getId();

			if (event.isSetId() && modelstate.isDeletedBySID(id))
			{
				continue;
			}
			else if (event.isSetMetaId() && modelstate.isDeletedByMetaID(event.getMetaId()))
			{
				continue;
			}

			setupArrays(modelstate, id, event, SetupType.EVENT);

			setupArrayObject(modelstate, id, null, event, null, SetupType.EVENT);

			if (!modelstate.isArrayedObject(id))
			{
				Setup.setupSingleEvent(modelstate, id, event.getTrigger().getMath(), event.getUseValuesFromTriggerTime(), event.getTrigger()
						.getInitialValue(), event.getTrigger().getPersistent(), getModels(), getIbiosimFunctionDefinitions());

				if (event.isSetPriority())
				{
					Setup.setupSinglePriority(modelstate, id, event.getPriority().getMetaId(), event.getPriority().getMath(), getModels(),
							getIbiosimFunctionDefinitions());
				}
				if (event.isSetDelay())
				{
					Setup.setupSingleDelay(modelstate, id, event.getDelay().getMetaId(), event.getDelay().getMath(), getModels(),
							getIbiosimFunctionDefinitions());
				}

				setupEventAssignments(modelstate, event, id);
			}

		}
	}

	protected void setupInitialAssignments(ModelState modelstate)
	{

		for (InitialAssignment initAssignment : getModels().get(modelstate.getModel()).getListOfInitialAssignments())
		{
			if (initAssignment.isSetMetaId() && modelstate.isDeletedByMetaID(initAssignment.getMetaId()))
			{
				continue;
			}
			String id = "initial_" + initAssignment.getVariable();
			setupArrays(modelstate, id, initAssignment, SetupType.INITIAL_ASSIGNMENT);
			setupArrayObject(modelstate, id, null, initAssignment, null, SetupType.INITIAL_ASSIGNMENT);
			if (!modelstate.isArrayedObject(id))
			{
				modelstate.getInitAssignment().put(initAssignment.getVariable(), initAssignment.getMath());
			}
		}
		Setup.calculateInitAssignments(modelstate, modelstate.getInitAssignment(), modelstate.getAssignmentRulesList(), getModels(),
				getIbiosimFunctionDefinitions(), getReplacements(), getCurrentTime());

	}

	protected void setupNonConstantSpeciesReferences(ModelState modelstate)
	{

		// loop through all reactions and calculate their propensities
		Reaction reaction;

		for (int i = 0; i < modelstate.getNumReactions(); i++)
		{
			reaction = getModels().get(modelstate.getModel()).getReaction(i);

			for (SpeciesReference reactant : reaction.getListOfReactants())
			{
				if (reactant.isSetId() && modelstate.isDeletedBySID(reactant.getId()))
				{
					continue;
				}
				else if (reactant.isSetMetaId() && modelstate.isDeletedByMetaID(reactant.getMetaId()))
				{
					continue;
				}

				if (reactant.getId().length() > 0)
				{
					if (reactant.getConstant() == false)
					{
						modelstate.getVariablesToPrint().add(reactant.getId());
					}
					else
					{
						modelstate.addVariableToIsConstant(reactant.getId());
					}
					if (modelstate.getVariableToValueMap().containsKey(reactant.getId()) == false)
					{
						modelstate.setVariableToValue(getReplacements(), reactant.getId(), reactant.getStoichiometry());
					}
				}
			}

			for (SpeciesReference product : reaction.getListOfProducts())
			{
				if (product.isSetId() && modelstate.isDeletedBySID(product.getId()))
				{
					continue;
				}
				else if (product.isSetMetaId() && modelstate.isDeletedByMetaID(product.getMetaId()))
				{
					continue;
				}
				if (product.getId().length() > 0)
				{
					if (product.getConstant() == false)
					{
						modelstate.getVariablesToPrint().add(product.getId());
					}
					else
					{
						modelstate.addVariableToIsConstant(product.getId());
					}
					if (modelstate.getVariableToValueMap().containsKey(product.getId()) == false)
					{
						modelstate.setVariableToValue(getReplacements(), product.getId(), product.getStoichiometry());
					}
				}
			}
		}
	}

	protected void setupEventAssignments(ModelState modelstate, Event event, String eventId)
	{
		for (EventAssignment assignment : event.getListOfEventAssignments())
		{
			if (assignment.isSetMetaId() && modelstate.isDeletedByMetaID(assignment.getMetaId()))
			{
				continue;
			}

			String assignmentId = event.getId() + "_" + assignment.getVariable();
			setupArrayObject(modelstate, assignmentId, assignmentId, assignment, null, SetupType.EVENT_ASSIGNMENT);

			if (!modelstate.isArrayedObject(assignmentId))
			{
				Setup.setupEventAssignment(modelstate, assignment.getVariable(), event.getId(), assignment.getMath(), assignment, getModels(),
						getIbiosimFunctionDefinitions());
			}
		}
	}

	/**
	 * puts parameter-related information into data structures
	 */
	protected void setupParameters(ModelState modelstate)
	{

		// add local parameters
		Reaction reaction;
		Parameter parameter;
		long size;

		size = modelstate.getNumReactions();
		for (int i = 0; i < size; i++)
		{
			reaction = getModels().get(modelstate.getModel()).getReaction(i);
			if (!reaction.isSetKineticLaw())
			{
				continue;
			}
			KineticLaw kineticLaw = reaction.getKineticLaw();

			if (kineticLaw.isSetMetaId() && modelstate.isDeletedByMetaID(kineticLaw.getMetaId()))
			{
				continue;
			}
			Setup.setupLocalParameters(modelstate, kineticLaw, reaction);
		}

		size = getModels().get(modelstate.getModel()).getListOfParameters().size();

		for (int i = 0; i < size; i++)
		{
			parameter = getModels().get(modelstate.getModel()).getParameter(i);

			if (parameter.isSetId() && modelstate.isDeletedBySID(parameter.getId()))
			{
				continue;
			}
			else if (parameter.isSetMetaId() && modelstate.isDeletedByMetaID(parameter.getMetaId()))
			{
				continue;
			}

			Setup.setupSingleParameter(modelstate, parameter, parameter.getId());
		}

		for (int i = 0; i < size; i++)
		{
			parameter = getModels().get(modelstate.getModel()).getParameter(i);

			setupArrays(modelstate, parameter.getId(), parameter, SetupType.PARAMETER);

			setupArrayValue(modelstate, parameter, SetupType.PARAMETER);
		}

	}

	protected void setupPropensities(ModelState modelstate)
	{
		for (String reaction : modelstate.getSetOfReactions())
		{
			Setup.setupSingleReactionPropensity(modelstate, reaction, modelstate.getReactionToFormulaMap().get(reaction), modelstate
					.getReactionToHasEnoughMolecules().get(reaction), getModels(), getIbiosimFunctionDefinitions(), getReplacements(),
					getCurrentTime());
		}

	}

	/**
	 * calculates the initial propensities for each reaction in the getModel()
	 * 
	 * @param numReactions
	 *            the number of reactions in the getModel()
	 */
	protected void setupReactions(ModelState modelstate)
	{
		Reaction reaction;
		for (int i = 0; i < modelstate.getNumReactions(); i++)
		{
			reaction = getModels().get(modelstate.getModel()).getReaction(i);

			if (reaction.isSetId() && modelstate.isDeletedBySID(reaction.getId()))
			{
				continue;
			}
			else if (reaction.isSetMetaId() && modelstate.isDeletedByMetaID(reaction.getMetaId()))
			{
				continue;
			}

			if (!reaction.isSetKineticLaw())
			{
				continue;
			}

			String reactionID = reaction.getId();
			String species = reactionID;
			if (reactionID.contains("Degradation") && getReplacements().containsKey(species))
			{
				if (modelstate.getIsHierarchical().contains(species) && !modelstate.getID().equals("topmodel"))
				{
					continue;
				}
			}

			ASTNode reactionFormula = reaction.getKineticLaw().getMath();
			setupArrays(modelstate, reaction.getId(), reaction, SetupType.REACTION);
			Setup.setupSingleReaction(modelstate, reaction, reactionID, reactionFormula, reaction.getReversible(), reaction.getListOfReactants(),
					reaction.getListOfProducts(), reaction.getListOfModifiers(), getModels(), getIbiosimFunctionDefinitions(), getReplacements(),
					getCurrentTime());
		}
	}

	protected void setupRules(ModelState modelstate)
	{
		long size = getModels().get(modelstate.getModel()).getListOfRules().size();

		if (size > 0)
		{
			modelstate.setNoRuleFlag(false);
		}

		for (Rule rule : getModels().get(modelstate.getModel()).getListOfRules())
		{
			if (rule.isSetMetaId() && modelstate.isDeletedByMetaID(rule.getMetaId()))
			{
				continue;
			}

			if (rule.isAssignment())
			{
				AssignmentRule assignRule = (AssignmentRule) rule;
				String id = "assignment_" + assignRule.getVariable();
				setupArrays(modelstate, id, assignRule, SetupType.ASSIGNMENT_RULE);
				setupArrayObject(modelstate, id, null, assignRule, null, SetupType.ASSIGNMENT_RULE);
				if (!modelstate.isArrayedObject(id))
				{
					Setup.setupSingleAssignmentRule(modelstate, assignRule.getVariable(), assignRule.getMath(), getModels(),
							getIbiosimFunctionDefinitions());
				}
			}
			else if (rule.isRate())
			{
				RateRule rateRule = (RateRule) rule;
				String id = "rate_" + rateRule.getVariable();
				setupArrays(modelstate, id, rateRule, SetupType.RATE_RULE);
				setupArrayObject(modelstate, id, null, rateRule, null, SetupType.RATE_RULE);
				if (!modelstate.isArrayedObject(id))
				{
					Setup.setupSingleRateRule(modelstate, rateRule.getVariable(), rateRule.getMath(), getModels(), getIbiosimFunctionDefinitions());
				}
			}

		}
	}

	/**
	 * puts species-related information into data structures
	 * 
	 * @throws IOException
	 */
	protected void setupSpecies(ModelState modelstate) throws IOException
	{
		Species species;
		long size = getModels().get(modelstate.getModel()).getListOfSpecies().size();
		for (int i = 0; i < size; i++)
		{

			species = getModels().get(modelstate.getModel()).getSpecies(i);

			if (species.isSetId() && modelstate.isDeletedBySID(species.getId()))
			{
				continue;
			}
			else if (species.isSetMetaId() && modelstate.isDeletedByMetaID(species.getMetaId()))
			{
				continue;
			}

			Setup.setupSingleSpecies(modelstate, species, species.getId(), getModels(), getReplacements());

			setupArrays(modelstate, species.getId(), species, SetupType.SPECIES);

			setupArrayValue(modelstate, species, SetupType.SPECIES);

		}

	}
}
