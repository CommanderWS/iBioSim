package sbmleditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import main.Gui;

import org.sbml.libsbml.AlgebraicRule;
import org.sbml.libsbml.AssignmentRule;
import org.sbml.libsbml.Compartment;
import org.sbml.libsbml.InitialAssignment;
import org.sbml.libsbml.ListOf;
import org.sbml.libsbml.Model;
import org.sbml.libsbml.Parameter;
import org.sbml.libsbml.RateRule;
import org.sbml.libsbml.Reaction;
import org.sbml.libsbml.Rule;
import org.sbml.libsbml.SBMLDocument;
import org.sbml.libsbml.Species;
import org.sbml.libsbml.SpeciesReference;
import org.sbml.libsbml.Unit;
import org.sbml.libsbml.UnitDefinition;
import org.sbml.libsbml.libsbml;

import util.MutableBoolean;
import util.Utility;

/**
 * This is a class for creating SBML rules
 * 
 * @author Chris Myers
 * 
 */
public class Rules extends JPanel implements ActionListener, MouseListener {

	private static final long serialVersionUID = 1L;

	private JButton addRule, removeRule, editRule;

	private JList rules; // JList of initial assignments
		
	private SBMLDocument document;
		
	private MutableBoolean dirty;
		
	private Gui biosim;
	
	private JComboBox ruleType,ruleVar;
	
	/* Create rule panel */
	public Rules(Gui biosim,SBMLDocument document,MutableBoolean dirty) {
		super(new BorderLayout());
		this.document = document;
		this.biosim = biosim;
		this.dirty = dirty;
		
		/* Create rule panel */
		Model model =  document.getModel();
		addRule = new JButton("Add Rule");
		removeRule = new JButton("Remove Rule");
		editRule = new JButton("Edit Rule");
		rules = new JList();
		ListOf listOfRules = model.getListOfRules();
		String[] rul = new String[(int) model.getNumRules()];
		for (int i = 0; i < model.getNumRules(); i++) {
			Rule rule = (Rule) listOfRules.get(i);
			if (rule.isAlgebraic()) {
				rul[i] = "0 = " + SBMLutilities.myFormulaToString(rule.getMath());
			}
			else if (rule.isAssignment()) {
				rul[i] = rule.getVariable() + " = " + SBMLutilities.myFormulaToString(rule.getMath());
			}
			else {
				rul[i] = "d( " + rule.getVariable() + " )/dt = "
						+ SBMLutilities.myFormulaToString(rule.getMath());
			}
		}
		String[] oldRul = rul;
		try {
			rul = sortRules(rul);
		}
		catch (Exception e) {
			//cycle = true;
			JOptionPane.showMessageDialog(Gui.frame, "Cycle detected in assignments.",
					"Cycle Detected", JOptionPane.ERROR_MESSAGE);
			rul = oldRul;
		}
		/*
		if (!cycle && SBMLutilities.checkCycles(document)) {
			JOptionPane.showMessageDialog(Gui.frame,
					"Cycle detected within initial assignments, assignment rules, and rate laws.",
					"Cycle Detected", JOptionPane.ERROR_MESSAGE);
		}
        */
		JPanel addRem = new JPanel();
		addRem.add(addRule);
		addRem.add(removeRule);
		addRem.add(editRule);
		addRule.addActionListener(this);
		removeRule.addActionListener(this);
		editRule.addActionListener(this);
		JLabel panelLabel = new JLabel("List of Rules:");
		JScrollPane scroll = new JScrollPane();
		scroll.setMinimumSize(new Dimension(260, 220));
		scroll.setPreferredSize(new Dimension(276, 152));
		scroll.setViewportView(rules);
		Utility.sort(rul);
		rules.setListData(rul);
		rules.setSelectedIndex(0);
		rules.addMouseListener(this);
		this.add(panelLabel, "North");
		this.add(scroll, "Center");
		this.add(addRem, "South");
	}

	/**
	 * Refresh initial assingment panel
	 */
	public void refreshRulesPanel(SBMLDocument document) {
		Model model = document.getModel();
		if (model.getNumRules() > 0) {
			String [] rul = new String[(int)model.getNumRules()];
			for (int i = 0; i < model.getNumRules(); i++) {
				Rule rule = (Rule) model.getListOfRules().get(i);
				if (rule.isAlgebraic()) {
					rul[i] = "0 = " + SBMLutilities.myFormulaToString(rule.getMath());
				}
				else if (rule.isAssignment()) {
					rul[i] = rule.getVariable() + " = " + SBMLutilities.myFormulaToString(rule.getMath());
				}
				else {
					rul[i] = "d( " + rule.getVariable() + " )/dt = "
							+ SBMLutilities.myFormulaToString(rule.getMath());
				}
			}
			try {
				rul = sortRules(rul);
				if (SBMLutilities.checkCycles(document)) {
					JOptionPane.showMessageDialog(Gui.frame,
							"Cycle detected within initial assignments, assignment rules, and rate laws.",
									"Cycle Detected", JOptionPane.ERROR_MESSAGE);
				}
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(Gui.frame, "Cycle detected in assignments.",
						"Cycle Detected", JOptionPane.ERROR_MESSAGE);
			}
			rules.setListData(rul);
			rules.setSelectedIndex(0);
		}
	}
		
	/**
	 * Creates a frame used to edit rules or create new ones.
	 */
	private void ruleEditor(String option) {
		if (option.equals("OK") && rules.getSelectedIndex() == -1) {
			JOptionPane.showMessageDialog(Gui.frame, "No rule selected.", "Must Select a Rule",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		JPanel rulePanel = new JPanel();
		JPanel rulPanel = new JPanel();
		JLabel typeLabel = new JLabel("Type:");
		JLabel varLabel = new JLabel("Variable:");
		JLabel ruleLabel = new JLabel("Rule:");
		String[] list = { "Algebraic", "Assignment", "Rate" };
		ruleType = new JComboBox(list);
		ruleVar = new JComboBox();
		JTextField ruleMath = new JTextField(30);
		ruleVar.setEnabled(false);
		ruleType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (((String) ruleType.getSelectedItem()).equals("Assignment")) {
					assignRuleVar("");
					ruleVar.setEnabled(true);
				}
				else if (((String) ruleType.getSelectedItem()).equals("Rate")) {
					rateRuleVar("");
					ruleVar.setEnabled(true);
				}
				else {
					ruleVar.removeAllItems();
					ruleVar.setEnabled(false);
				}
			}
		});
		int Rindex = -1;
		if (option.equals("OK")) {
			ruleType.setEnabled(false);
			String selected = ((String) rules.getSelectedValue());
			// algebraic rule
			if ((selected.split(" ")[0]).equals("0")) {
				ruleType.setSelectedItem("Algebraic");
				ruleVar.setEnabled(false);
				ruleMath.setText(selected.substring(4));
				ListOf r = document.getModel().getListOfRules();
				for (int i = 0; i < document.getModel().getNumRules(); i++) {
					if ((((Rule) r.get(i)).isAlgebraic())
							&& (SBMLutilities.myFormulaToString(((Rule) r.get(i)).getMath()).equals(ruleMath
									.getText()))) {
						Rindex = i;
					}
				}
			}
			else if ((selected.split(" ")[0]).equals("d(")) {
				ruleType.setSelectedItem("Rate");
				rateRuleVar(selected.split(" ")[1]);
				ruleVar.setEnabled(true);
				ruleVar.setSelectedItem(selected.split(" ")[1]);
				ruleMath.setText(selected.substring(selected.indexOf('=') + 2));
				ListOf r = document.getModel().getListOfRules();
				for (int i = 0; i < document.getModel().getNumRules(); i++) {
					if ((((Rule) r.get(i)).isRate())
							&& ((Rule) r.get(i)).getVariable().equals(ruleVar.getSelectedItem())) {
						Rindex = i;
					}
				}
			}
			else {
				ruleType.setSelectedItem("Assignment");
				assignRuleVar(selected.split(" ")[0]);
				ruleVar.setEnabled(true);
				ruleVar.setSelectedItem(selected.split(" ")[0]);
				ruleMath.setText(selected.substring(selected.indexOf('=') + 2));
				ListOf r = document.getModel().getListOfRules();
				for (int i = 0; i < document.getModel().getNumRules(); i++) {
					if ((((Rule) r.get(i)).isAssignment())
							&& ((Rule) r.get(i)).getVariable().equals(ruleVar.getSelectedItem())) {
						Rindex = i;
					}
				}
			}
		}
		else {
			if (!assignRuleVar("") && !rateRuleVar("")) {
				String[] list1 = { "Algebraic" };
				ruleType = new JComboBox(list1);
			}
			else if (!assignRuleVar("")) {
				String[] list1 = { "Algebraic", "Rate" };
				ruleType = new JComboBox(list1);
			}
			else if (!rateRuleVar("")) {
				String[] list1 = { "Algebraic", "Assignment" };
				ruleType = new JComboBox(list1);
			}
		}
		rulPanel.add(typeLabel);
		rulPanel.add(ruleType);
		rulPanel.add(varLabel);
		rulPanel.add(ruleVar);
		rulPanel.add(ruleLabel);
		rulPanel.add(ruleMath);
		rulePanel.add(rulPanel);
		Object[] options = { option, "Cancel" };
		int value = JOptionPane.showOptionDialog(Gui.frame, rulePanel, "Rule Editor",
				JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		boolean error = true;
		while (error && value == JOptionPane.YES_OPTION) {
			error = false;
			String addVar = "";
			addVar = (String) ruleVar.getSelectedItem();
			if (ruleMath.getText().trim().equals("")) {
				JOptionPane.showMessageDialog(Gui.frame, "Rule must have formula.",
						"Enter Rule Formula", JOptionPane.ERROR_MESSAGE);
				error = true;
			}
			else if (SBMLutilities.myParseFormula(ruleMath.getText().trim()) == null) {
				JOptionPane.showMessageDialog(Gui.frame, "Rule formula is not valid.",
						"Enter Valid Formula", JOptionPane.ERROR_MESSAGE);
				error = true;
			}
			else {
				ArrayList<String> invalidVars = SBMLutilities.getInvalidVariables(document,ruleMath.getText().trim(), "", false);
				if (invalidVars.size() > 0) {
					String invalid = "";
					for (int i = 0; i < invalidVars.size(); i++) {
						if (i == invalidVars.size() - 1) {
							invalid += invalidVars.get(i);
						}
						else {
							invalid += invalidVars.get(i) + "\n";
						}
					}
					String message;
					message = "Rule contains unknown variables.\n\n" + "Unknown variables:\n"
							+ invalid;
					JTextArea messageArea = new JTextArea(message);
					messageArea.setLineWrap(true);
					messageArea.setWrapStyleWord(true);
					messageArea.setEditable(false);
					JScrollPane scrolls = new JScrollPane();
					scrolls.setMinimumSize(new Dimension(300, 300));
					scrolls.setPreferredSize(new Dimension(300, 300));
					scrolls.setViewportView(messageArea);
					JOptionPane.showMessageDialog(Gui.frame, scrolls, "Unknown Variables",
							JOptionPane.ERROR_MESSAGE);
					error = true;
				}
				if (!error) {
					error = SBMLutilities.checkNumFunctionArguments(document,SBMLutilities.myParseFormula(ruleMath.getText().trim()));
				}
				if (!error) {
					if (SBMLutilities.myParseFormula(ruleMath.getText().trim()).isBoolean()) {
						JOptionPane.showMessageDialog(Gui.frame,
								"Rule must evaluate to a number.", "Number Expected",
								JOptionPane.ERROR_MESSAGE);
						error = true;
					}
				}
			}
			if (!error) {
				if (option.equals("OK")) {
					String [] rul = new String[rules.getModel().getSize()];
					for (int i=0;i<rules.getModel().getSize();i++) {
						rul[i] = rules.getModel().getElementAt(i).toString();
					}
					int index = rules.getSelectedIndex();
					rules.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
					rul = Utility.getList(rul, rules);
					rules.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					Rule r = (Rule) (document.getModel().getListOfRules()).get(Rindex);
					String addStr;
					String oldVar = "";
					String oldMath = SBMLutilities.myFormulaToString(r.getMath());
					if (ruleType.getSelectedItem().equals("Algebraic")) {
						r.setMath(SBMLutilities.myParseFormula(ruleMath.getText().trim()));
						addStr = "0 = " + SBMLutilities.myFormulaToString(r.getMath());
						SBMLutilities.checkOverDetermined(document);
					}
					else if (ruleType.getSelectedItem().equals("Rate")) {
						oldVar = r.getVariable();
						r.setVariable(addVar);
						r.setMath(SBMLutilities.myParseFormula(ruleMath.getText().trim()));
						error = checkRateRuleUnits(r);
						addStr = "d( " + addVar + " )/dt = " + SBMLutilities.myFormulaToString(r.getMath());
					}
					else {
						oldVar = r.getVariable();
						r.setVariable(addVar);
						r.setMath(SBMLutilities.myParseFormula(ruleMath.getText().trim()));
						error = checkAssignmentRuleUnits(r);
						addStr = addVar + " = " + SBMLutilities.myFormulaToString(r.getMath());
					}
					String oldVal = rul[index];
					rul[index] = addStr;
					if (!error) {
						try {
							rul = sortRules(rul);
						}
						catch (Exception e) {
							JOptionPane.showMessageDialog(Gui.frame,
									"Cycle detected in assignments.", "Cycle Detected",
									JOptionPane.ERROR_MESSAGE);
							error = true;
						}
					}
					if (!error && SBMLutilities.checkCycles(document)) {
						JOptionPane
								.showMessageDialog(
										Gui.frame,
										"Cycle detected within initial assignments, assignment rules, and rate laws.",
										"Cycle Detected", JOptionPane.ERROR_MESSAGE);
						error = true;
					}
					if (error) {
						if (!oldVar.equals("")) {
							r.setVariable(oldVar);
						}
						r.setMath(SBMLutilities.myParseFormula(oldMath));
						rul[index] = oldVal;
					}
					updateRules(rul);
					rules.setListData(rul);
					rules.setSelectedIndex(index);
				}
				else {
					String [] rul = new String[rules.getModel().getSize()];
					for (int i=0;i<rules.getModel().getSize();i++) {
						rul[i] = rules.getModel().getElementAt(i).toString();
					}					JList add = new JList();
					int index = rules.getSelectedIndex();
					String addStr;
					if (ruleType.getSelectedItem().equals("Algebraic")) {
						addStr = "0 = "
								+ SBMLutilities.myFormulaToString(SBMLutilities.myParseFormula(ruleMath.getText().trim()));
					}
					else if (ruleType.getSelectedItem().equals("Rate")) {
						addStr = "d( " + addVar + " )/dt = "
								+ SBMLutilities.myFormulaToString(SBMLutilities.myParseFormula(ruleMath.getText().trim()));
					}
					else {
						addStr = addVar + " = "
								+ SBMLutilities.myFormulaToString(SBMLutilities.myParseFormula(ruleMath.getText().trim()));
					}
					Object[] adding = { addStr };
					add.setListData(adding);
					add.setSelectedIndex(0);
					rules.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
					adding = Utility.add(rul, rules, add, false, null, null, null, null, null,
							null, Gui.frame);
					String[] oldRul = rul;
					rul = new String[adding.length];
					for (int i = 0; i < adding.length; i++) {
						rul[i] = (String) adding[i];
					}
					try {
						rul = sortRules(rul);
					}
					catch (Exception e) {
						JOptionPane.showMessageDialog(Gui.frame,
								"Cycle detected in assignments.", "Cycle Detected",
								JOptionPane.ERROR_MESSAGE);
						error = true;
						rul = oldRul;
					}
					if (!error) {
						if (ruleType.getSelectedItem().equals("Algebraic")) {
							AlgebraicRule r = document.getModel().createAlgebraicRule();
							r.setMath(SBMLutilities.myParseFormula(ruleMath.getText().trim()));
							SBMLutilities.checkOverDetermined(document);
						}
						else if (ruleType.getSelectedItem().equals("Rate")) {
							RateRule r = document.getModel().createRateRule();
							r.setVariable(addVar);
							r.setMath(SBMLutilities.myParseFormula(ruleMath.getText().trim()));
							error = checkRateRuleUnits(r);
						}
						else {
							AssignmentRule r = document.getModel().createAssignmentRule();
							r.setVariable(addVar);
							r.setMath(SBMLutilities.myParseFormula(ruleMath.getText().trim()));
							error = checkAssignmentRuleUnits(r);
						}
					}
					if (!error && SBMLutilities.checkCycles(document)) {
						JOptionPane
								.showMessageDialog(
										Gui.frame,
										"Cycle detected within initial assignments, assignment rules, and rate laws.",
										"Cycle Detected", JOptionPane.ERROR_MESSAGE);
						error = true;
						rul = oldRul;
					}
					if (error) {
						rul = oldRul;
						removeTheRule(addStr);
					}
					updateRules(rul);
					rules.setListData(rul);
					rules.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					if (document.getModel().getNumRules() == 1) {
						rules.setSelectedIndex(0);
					}
					else {
						rules.setSelectedIndex(index);
					}
				}
				dirty.setValue(true);
			}
			if (error) {
				value = JOptionPane.showOptionDialog(Gui.frame, rulePanel, "Rule Editor",
						JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
						options[0]);
			}
		}
		if (value == JOptionPane.NO_OPTION) {
			return;
		}
	}

	/**
	 * Remove a rule
	 */
	private void removeRule() {
		int index = rules.getSelectedIndex();
		if (index != -1) {
			String selected = ((String) rules.getSelectedValue());
			removeTheRule(selected);
			rules.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			Utility.remove(rules);
			rules.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			if (index < rules.getModel().getSize()) {
				rules.setSelectedIndex(index);
			} else {
				rules.setSelectedIndex(index-1);
			}
			dirty.setValue(true);
		}
	}

	/**
	 * Remove the rule
	 */
	private void removeTheRule(String selected) {
		// algebraic rule
		if ((selected.split(" ")[0]).equals("0")) {
			String tempMath = selected.substring(4);
			ListOf r = document.getModel().getListOfRules();
			for (int i = 0; i < document.getModel().getNumRules(); i++) {
				if ((((Rule) r.get(i)).isAlgebraic())
						&& SBMLutilities.myFormulaToString(((Rule) r.get(i)).getMath()).equals(tempMath)) {
					r.remove(i);
				}
			}
		}
		// rate rule
		else if ((selected.split(" ")[0]).equals("d(")) {
			String tempVar = selected.split(" ")[1];
			String tempMath = selected.substring(selected.indexOf('=') + 2);
			ListOf r = document.getModel().getListOfRules();
			for (int i = 0; i < document.getModel().getNumRules(); i++) {
				if ((((Rule) r.get(i)).isRate())
						&& SBMLutilities.myFormulaToString(((Rule) r.get(i)).getMath()).equals(tempMath)
						&& ((Rule) r.get(i)).getVariable().equals(tempVar)) {
					r.remove(i);
				}
			}
		}
		// assignment rule
		else {
			String tempVar = selected.split(" ")[0];
			String tempMath = selected.substring(selected.indexOf('=') + 2);
			ListOf r = document.getModel().getListOfRules();
			for (int i = 0; i < document.getModel().getNumRules(); i++) {
				if ((((Rule) r.get(i)).isAssignment())
						&& SBMLutilities.myFormulaToString(((Rule) r.get(i)).getMath()).equals(tempMath)
						&& ((Rule) r.get(i)).getVariable().equals(tempVar)) {
					r.remove(i);
				}
			}
		}
	}

	/**
	 * Sort rules in order to be evaluated
	 */
	private String[] sortRules(String[] rules) {
		String[] result = new String[rules.length];
		int j = 0;
		boolean[] used = new boolean[rules.length];
		for (int i = 0; i < rules.length; i++) {
			used[i] = false;
		}
		for (int i = 0; i < rules.length; i++) {
			if (rules[i].split(" ")[0].equals("0")) {
				result[j] = rules[i];
				used[i] = true;
				j++;
			}
		}
		boolean progress;
		do {
			progress = false;
			for (int i = 0; i < rules.length; i++) {
				if (used[i] || (rules[i].split(" ")[0].equals("0"))
						|| (rules[i].split(" ")[0].equals("d(")))
					continue;
				String[] rule = rules[i].split(" ");
				boolean insert = true;
				for (int k = 1; k < rule.length; k++) {
					for (int l = 0; l < rules.length; l++) {
						if (used[l] || (rules[l].split(" ")[0].equals("0"))
								|| (rules[l].split(" ")[0].equals("d(")))
							continue;
						String[] rule2 = rules[l].split(" ");
						if (rule[k].equals(rule2[0])) {
							insert = false;
							break;
						}
					}
					if (!insert)
						break;
				}
				if (insert) {
					result[j] = rules[i];
					j++;
					progress = true;
					used[i] = true;
				}
			}
		}
		while ((progress) && (j < rules.length));
		for (int i = 0; i < rules.length; i++) {
			if (rules[i].split(" ")[0].equals("d(")) {
				result[j] = rules[i];
				j++;
			}
		}
		if (j != rules.length) {
			throw new RuntimeException();
		}
		return result;
	}
	
	/**
	 * Create comboBox for assignments rules
	 */
	private boolean assignRuleVar(String selected) {
		boolean assignOK = false;
		ruleVar.removeAllItems();
		Model model = document.getModel();
		ListOf ids = model.getListOfCompartments();
		for (int i = 0; i < model.getNumCompartments(); i++) {
			String id = ((Compartment) ids.get(i)).getId();
			if (!((Compartment) ids.get(i)).getConstant()) {
				if (keepVarAssignRule(document,selected, id)) {
					ruleVar.addItem(((Compartment) ids.get(i)).getId());
					assignOK = true;
				}
			}
		}
		ids = model.getListOfParameters();
		for (int i = 0; i < model.getNumParameters(); i++) {
			String id = ((Parameter) ids.get(i)).getId();
			if (!((Parameter) ids.get(i)).getConstant()) {
				if (keepVarAssignRule(document,selected, id)) {
					ruleVar.addItem(((Parameter) ids.get(i)).getId());
					assignOK = true;
				}
			}
		}
		ids = model.getListOfSpecies();
		for (int i = 0; i < model.getNumSpecies(); i++) {
			String id = ((Species) ids.get(i)).getId();
			if (!((Species) ids.get(i)).getConstant()) {
				if (keepVarAssignRule(document,selected, id))
					if (((Species) ids.get(i)).getBoundaryCondition() || !SBMLutilities.usedInReaction(document,id)) {
						ruleVar.addItem(((Species) ids.get(i)).getId());
						assignOK = true;
					}
			}
		}
		ids = model.getListOfReactions();
		for (int i = 0; i < model.getNumReactions(); i++) {
			Reaction reaction = (Reaction) ids.get(i);
			ListOf ids2 = reaction.getListOfReactants();
			for (int j = 0; j < reaction.getNumReactants(); j++) {
				SpeciesReference reactant = (SpeciesReference) ids2.get(j);
				if ((reactant.isSetId()) && (!reactant.getId().equals(""))
						&& !(reactant.getConstant())) {
					String id = reactant.getId();
					if (keepVarAssignRule(document,selected, id)) {
						ruleVar.addItem(id);
						assignOK = true;
					}
				}
			}
			ids2 = reaction.getListOfProducts();
			for (int j = 0; j < reaction.getNumProducts(); j++) {
				SpeciesReference product = (SpeciesReference) ids2.get(j);
				if ((product.isSetId()) && (!product.getId().equals(""))
						&& !(product.getConstant())) {
					String id = product.getId();
					if (keepVarAssignRule(document,selected, id)) {
						ruleVar.addItem(id);
						assignOK = true;
					}
				}
			}
		}
		return assignOK;
	}

	/**
	 * Create comboBox for rate rules
	 */
	private boolean rateRuleVar(String selected) {
		boolean rateOK = false;
		ruleVar.removeAllItems();
		Model model = document.getModel();
		ListOf ids = model.getListOfCompartments();
		for (int i = 0; i < model.getNumCompartments(); i++) {
			String id = ((Compartment) ids.get(i)).getId();
			if (!((Compartment) ids.get(i)).getConstant()) {
				if (keepVarRateRule(document,selected, id)) {
					ruleVar.addItem(((Compartment) ids.get(i)).getId());
					rateOK = true;
				}
			}
		}
		ids = model.getListOfParameters();
		for (int i = 0; i < model.getNumParameters(); i++) {
			String id = ((Parameter) ids.get(i)).getId();
			if (!((Parameter) ids.get(i)).getConstant()) {
				if (keepVarRateRule(document,selected, id)) {
					ruleVar.addItem(((Parameter) ids.get(i)).getId());
					rateOK = true;
				}
			}
		}
		ids = model.getListOfSpecies();
		for (int i = 0; i < model.getNumSpecies(); i++) {
			String id = ((Species) ids.get(i)).getId();
			if (!((Species) ids.get(i)).getConstant()) {
				if (keepVarRateRule(document,selected, id))
					if (((Species) ids.get(i)).getBoundaryCondition() || !SBMLutilities.usedInReaction(document,id)) {
						ruleVar.addItem(((Species) ids.get(i)).getId());
						rateOK = true;
					}
			}
		}
		ids = model.getListOfReactions();
		for (int i = 0; i < model.getNumReactions(); i++) {
			Reaction reaction = (Reaction) ids.get(i);
			ListOf ids2 = reaction.getListOfReactants();
			for (int j = 0; j < reaction.getNumReactants(); j++) {
				SpeciesReference reactant = (SpeciesReference) ids2.get(j);
				if ((reactant.isSetId()) && (!reactant.getId().equals(""))
						&& !(reactant.getConstant())) {
					String id = reactant.getId();
					if (keepVarRateRule(document,selected, id)) {
						ruleVar.addItem(id);
						rateOK = true;
					}
				}
			}
			ids2 = reaction.getListOfProducts();
			for (int j = 0; j < reaction.getNumProducts(); j++) {
				SpeciesReference product = (SpeciesReference) ids2.get(j);
				if ((product.isSetId()) && (!product.getId().equals(""))
						&& !(product.getConstant())) {
					String id = product.getId();
					if (keepVarRateRule(document,selected, id)) {
						ruleVar.addItem(id);
						rateOK = true;
					}
				}
			}
		}
		return rateOK;
	}

	/**
	 * Check the units of a rate rule
	 */
	public boolean checkRateRuleUnits(Rule rule) {
		document.getModel().populateListFormulaUnitsData();
		if (rule.containsUndeclaredUnits()) {
			if (biosim.checkUndeclared) {
				JOptionPane
						.showMessageDialog(
								Gui.frame,
								"Rate rule contains literals numbers or parameters with undeclared units.\n"
										+ "Therefore, it is not possible to completely verify the consistency of the units.",
								"Contains Undeclared Units", JOptionPane.WARNING_MESSAGE);
			}
			return false;
		}
		else if (biosim.checkUnits) {
			UnitDefinition unitDef = rule.getDerivedUnitDefinition();
			UnitDefinition unitDefVar;
			Species species = document.getModel().getSpecies(rule.getVariable());
			Compartment compartment = document.getModel().getCompartment(rule.getVariable());
			Parameter parameter = document.getModel().getParameter(rule.getVariable());
			if (species != null) {
				unitDefVar = species.getDerivedUnitDefinition();
			}
			else if (compartment != null) {
				unitDefVar = compartment.getDerivedUnitDefinition();
			}
			else {
				unitDefVar = parameter.getDerivedUnitDefinition();
			}
			if (document.getModel().getUnitDefinition("time") != null) {
				UnitDefinition timeUnitDef = document.getModel().getUnitDefinition("time");
				for (int i = 0; i < timeUnitDef.getNumUnits(); i++) {
					Unit timeUnit = timeUnitDef.getUnit(i);
					Unit recTimeUnit = unitDefVar.createUnit();
					recTimeUnit.setKind(timeUnit.getKind());
					if (document.getLevel() < 3) {
						recTimeUnit.setExponent(timeUnit.getExponent() * (-1));
					}
					else {
						recTimeUnit.setExponent(timeUnit.getExponentAsDouble() * (-1));
					}
					recTimeUnit.setScale(timeUnit.getScale());
					recTimeUnit.setMultiplier(timeUnit.getMultiplier());
				}
			}
			else {
				Unit unit = unitDefVar.createUnit();
				unit.setKind(libsbml.UnitKind_forName("second"));
				unit.setExponent(-1);
				unit.setScale(0);
				unit.setMultiplier(1.0);
			}
			if (!UnitDefinition.areEquivalent(unitDef, unitDefVar)) {
				JOptionPane.showMessageDialog(Gui.frame,
						"Units on the left and right-hand side of the rate rule do not agree.",
						"Units Do Not Match", JOptionPane.ERROR_MESSAGE);
				return true;
			}
		}
		return false;
	}

	/**
	 * Check the units of an assignment rule
	 */
	public boolean checkAssignmentRuleUnits(Rule rule) {
		document.getModel().populateListFormulaUnitsData();
		if (rule.containsUndeclaredUnits()) {
			if (biosim.checkUndeclared) {
				JOptionPane
						.showMessageDialog(
								Gui.frame,
								"Assignment rule contains literals numbers or parameters with undeclared units.\n"
										+ "Therefore, it is not possible to completely verify the consistency of the units.",
								"Contains Undeclared Units", JOptionPane.WARNING_MESSAGE);
			}
			return false;
		}
		else if (biosim.checkUnits) {
			UnitDefinition unitDef = rule.getDerivedUnitDefinition();
			UnitDefinition unitDefVar;
			Species species = document.getModel().getSpecies(rule.getVariable());
			Compartment compartment = document.getModel().getCompartment(rule.getVariable());
			Parameter parameter = document.getModel().getParameter(rule.getVariable());
			if (species != null) {
				unitDefVar = species.getDerivedUnitDefinition();
			}
			else if (compartment != null) {
				unitDefVar = compartment.getDerivedUnitDefinition();
			}
			else {
				unitDefVar = parameter.getDerivedUnitDefinition();
			}
			if (!UnitDefinition.areEquivalent(unitDef, unitDefVar)) {
				JOptionPane
						.showMessageDialog(
								Gui.frame,
								"Units on the left and right-hand side of the assignment rule do not agree.",
								"Units Do Not Match", JOptionPane.ERROR_MESSAGE);
				return true;
			}
		}
		return false;
	}

	/**
	 * Update rules
	 */
	private void updateRules(String [] rul) {
		ListOf r = document.getModel().getListOfRules();
		while (document.getModel().getNumRules() > 0) {
			r.remove(0);
		}
		for (int i = 0; i < rul.length; i++) {
			if (rul[i].split(" ")[0].equals("0")) {
				AlgebraicRule rule = document.getModel().createAlgebraicRule();
				rule.setMath(SBMLutilities.myParseFormula(rul[i].substring(rul[i].indexOf("=") + 1)));
			}
			else if (rul[i].split(" ")[0].equals("d(")) {
				RateRule rule = document.getModel().createRateRule();
				rule.setVariable(rul[i].split(" ")[1]);
				rule.setMath(SBMLutilities.myParseFormula(rul[i].substring(rul[i].indexOf("=") + 1)));
			}
			else {
				AssignmentRule rule = document.getModel().createAssignmentRule();
				rule.setVariable(rul[i].split(" ")[0]);
				rule.setMath(SBMLutilities.myParseFormula(rul[i].substring(rul[i].indexOf("=") + 1)));
			}
		}
	}

	/**
	 * Determines if a variable is already in an initial assignment, assignment rule, or rate rule
	 */
	public static boolean keepVarAssignRule(SBMLDocument document, String selected, String id) {
		if (!selected.equals(id)) {
			ListOf ia = document.getModel().getListOfInitialAssignments();
			for (int i = 0; i < document.getModel().getNumInitialAssignments(); i++) {
				InitialAssignment init = (InitialAssignment)ia.get(i);
				if (init.getSymbol().equals(id)) return false;
			}
			ListOf e = document.getModel().getListOfEvents();
			for (int i = 0; i < document.getModel().getNumEvents(); i++) {
				org.sbml.libsbml.Event event = (org.sbml.libsbml.Event) e.get(i);
				for (int j = 0; j < event.getNumEventAssignments(); j++) {
					if (id.equals(event.getEventAssignment(j).getVariable())) {
						return false;
					}
				}
			}
			ListOf r = document.getModel().getListOfRules();
			for (int i = 0; i < document.getModel().getNumRules(); i++) {
				Rule rule = (Rule)r.get(i);
				if (rule.isAssignment() && rule.getVariable().equals(id)) return false;
				if (rule.isRate() && rule.getVariable().equals(id)) return false;
			}
		}
		return true;
	}

	/**
	 * Determines if a variable is already in a rate rule
	 */
	public static boolean keepVarRateRule(SBMLDocument document, String selected, String id) {
		if (!selected.equals(id)) {
			ListOf r = document.getModel().getListOfRules();
			for (int i = 0; i < document.getModel().getNumRules(); i++) {
				Rule rule = (Rule)r.get(i);
				if (rule.isRate() && rule.getVariable().equals(id)) return false;
			}
		}
		return true;
	}	

	public void actionPerformed(ActionEvent e) {
		// if the add event button is clicked
		if (e.getSource() == addRule) {
			ruleEditor("Add");
		}
		// if the edit event button is clicked
		else if (e.getSource() == editRule) {
			ruleEditor("OK");
		}
		// if the remove event button is clicked
		else if (e.getSource() == removeRule) {
			removeRule();
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			if (e.getSource() == rules) {
				ruleEditor("OK");
			}
		}
	}

	/**
	 * This method currently does nothing.
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mouseExited(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mousePressed(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mouseReleased(MouseEvent e) {
	}
}
