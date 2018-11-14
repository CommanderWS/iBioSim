package edu.utah.ece.async.ibiosim.synthesis.VerilogCompiler;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sbolstandard.core2.AccessType;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.DirectionType;
import org.sbolstandard.core2.FunctionalComponent;
import org.sbolstandard.core2.Interaction;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.Participation;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SystemsBiologyOntology;


/**
 * Test OR gate design that was compiled from a verilog expression to an SBOL data model
 * @author Tramy Nguyen
 *
 */
public class SBOL_Example3 extends AbstractVerilogParserTest{

	private static SBOLDocument sbolDoc;
	private static ModuleDefinition sbolDesign;

	@BeforeClass
	public static void setupTest() {
		
		String[] cmd = {"-v", reader.getFile("contAssign4.v"), "-sbol"};
		
		VerilogCompiler compiledVerilog = reader.runCompiler(cmd);
		String vName = "contAssign4";
		WrappedSBOL sbolWrapper = compiledVerilog.getSBOLWrapper(vName);
		Assert.assertNotNull(sbolWrapper);
		sbolDoc = sbolWrapper.getSBOLDocument();
		Assert.assertEquals(1, sbolDoc.getModuleDefinitions().size());
		sbolDesign = sbolDoc.getModuleDefinition(vName, "1.0");
	}

	@Test
	public void Test_cdSize() {
		Assert.assertEquals(6, sbolDoc.getComponentDefinitions().size());
	}
	
	@Test
	public void Test_fcSize() {
		Assert.assertEquals(6, sbolDesign.getFunctionalComponents().size());
	}
	
	@Test
	public void Test_CD_ports() {
		List<String> expected_id = Arrays.asList("CD0_a", "CD1_b", "CD2_y", 
				"CD3_wiredProtein");
		for(String id : expected_id) {
			ComponentDefinition cd = sbolDoc.getComponentDefinition(id, "1.0");
			Assert.assertNotNull(cd);
			Assert.assertEquals(1, cd.getTypes().size());
			Assert.assertEquals(ComponentDefinition.PROTEIN, cd.getTypes().iterator().next());
		}
	}
	
	@Test
	public void Test_CD_gate() {
		List<String> expected_id = Arrays.asList("CD4_notGate", "CD5_norGate");
		for(String id : expected_id) {
			ComponentDefinition cd = sbolDoc.getComponentDefinition(id, "1.0");
			Assert.assertNotNull(cd);
			Assert.assertEquals(1, cd.getTypes().size());
			Assert.assertEquals(ComponentDefinition.DNA, cd.getTypes().iterator().next());
		}
	}
	
	@Test
	public void Test_FC_Inputs() {
		List<String> expected_id = Arrays.asList("FC0_a", "FC1_b");
		for(String id : expected_id) {
			FunctionalComponent fc = sbolDesign.getFunctionalComponent(id);
			Assert.assertNotNull(fc);
			Assert.assertEquals(AccessType.PUBLIC, fc.getAccess());
			Assert.assertEquals(DirectionType.IN, fc.getDirection());
		}
	}
	
	@Test
	public void Test_FC_output() {
		List<String> expected_id = Arrays.asList("FC2_y");
		for(String id : expected_id) {
			FunctionalComponent fc = sbolDesign.getFunctionalComponent(id);
			Assert.assertNotNull(fc);
			Assert.assertEquals(AccessType.PUBLIC, fc.getAccess());
			Assert.assertEquals(DirectionType.OUT, fc.getDirection());
		}
	}
	
	@Test
	public void Test_FC_internalWires() {
		List<String> expected_id = Arrays.asList("FC3_wiredProtein");
		for(String id : expected_id) {
			FunctionalComponent fc = sbolDesign.getFunctionalComponent(id);
			Assert.assertNotNull(fc);
			Assert.assertEquals(AccessType.PUBLIC, fc.getAccess());
			Assert.assertEquals(DirectionType.NONE, fc.getDirection());
		}
	}
	
	@Test
	public void Test_FC_gate() {
		List<String> expected_id = Arrays.asList("FC4_notGate", "FC5_norGate");
		for(String id : expected_id) {
			FunctionalComponent fc = sbolDesign.getFunctionalComponent(id);
			Assert.assertNotNull(fc);
			Assert.assertEquals(AccessType.PUBLIC, fc.getAccess());
			Assert.assertEquals(DirectionType.NONE, fc.getDirection());
		}
	}
	
	@Test
	public void Test_InteractionSize() {
		Assert.assertEquals(5, sbolDesign.getInteractions().size());
	}
	
	@Test
	public void Test_Interaction1() {
		Interaction interaction = sbolDesign.getInteraction("I0");
		Assert.assertNotNull(interaction);
		Assert.assertEquals(1, interaction.getTypes().size());
		Assert.assertEquals(SystemsBiologyOntology.INHIBITION, interaction.getTypes().iterator().next());
		
		Participation p1 = interaction.getParticipation("P0");
		Assert.assertNotNull(p1);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC3_wiredProtein"), p1.getParticipant());
		Assert.assertEquals(1, p1.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.INHIBITOR, p1.getRoles().iterator().next());
		
		Participation p2 = interaction.getParticipation("P1");
		Assert.assertNotNull(p2);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC4_notGate"), p2.getParticipant());
		Assert.assertEquals(1, p2.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.INHIBITED, p2.getRoles().iterator().next());
	}
	
	@Test
	public void Test_Interaction2() {
		Interaction interaction = sbolDesign.getInteraction("I1");
		Assert.assertNotNull(interaction);
		Assert.assertEquals(1, interaction.getTypes().size());
		Assert.assertEquals(SystemsBiologyOntology.GENETIC_PRODUCTION, interaction.getTypes().iterator().next());
		
		Participation p1 = interaction.getParticipation("P2");
		Assert.assertNotNull(p1);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC4_notGate"), p1.getParticipant());
		Assert.assertEquals(1, p1.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.PROMOTER, p1.getRoles().iterator().next());
		
		Participation p2 = interaction.getParticipation("P3");
		Assert.assertNotNull(p2);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC2_y"), p2.getParticipant());
		Assert.assertEquals(1, p2.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.PRODUCT, p2.getRoles().iterator().next());
	}
	
	@Test
	public void Test_Interaction3() {
		Interaction interaction = sbolDesign.getInteraction("I2");
		Assert.assertNotNull(interaction);
		Assert.assertEquals(1, interaction.getTypes().size());
		Assert.assertEquals(SystemsBiologyOntology.INHIBITION, interaction.getTypes().iterator().next());
		
		Participation p1 = interaction.getParticipation("P4");
		Assert.assertNotNull(p1);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC1_b"), p1.getParticipant());
		Assert.assertEquals(1, p1.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.INHIBITOR, p1.getRoles().iterator().next());
		
		Participation p2 = interaction.getParticipation("P5");
		Assert.assertNotNull(p2);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC5_norGate"), p2.getParticipant());
		Assert.assertEquals(1, p2.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.INHIBITED, p2.getRoles().iterator().next());
	}
	
	@Test
	public void Test_Interaction4() {
		Interaction interaction = sbolDesign.getInteraction("I3");
		Assert.assertNotNull(interaction);
		Assert.assertEquals(1, interaction.getTypes().size());
		Assert.assertEquals(SystemsBiologyOntology.INHIBITION, interaction.getTypes().iterator().next());
		
		Participation p1 = interaction.getParticipation("P6");
		Assert.assertNotNull(p1);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC0_a"), p1.getParticipant());
		Assert.assertEquals(1, p1.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.INHIBITOR, p1.getRoles().iterator().next());
		
		Participation p2 = interaction.getParticipation("P7");
		Assert.assertNotNull(p2);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC5_norGate"), p2.getParticipant());
		Assert.assertEquals(1, p2.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.INHIBITED, p2.getRoles().iterator().next());
	}
	
	@Test
	public void Test_Interaction5() {
		Interaction interaction = sbolDesign.getInteraction("I4");
		Assert.assertNotNull(interaction);
		Assert.assertEquals(1, interaction.getTypes().size());
		Assert.assertEquals(SystemsBiologyOntology.GENETIC_PRODUCTION, interaction.getTypes().iterator().next());
		
		Participation p1 = interaction.getParticipation("P8");
		Assert.assertNotNull(p1);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC5_norGate"), p1.getParticipant());
		Assert.assertEquals(1, p1.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.PROMOTER, p1.getRoles().iterator().next());
		
		Participation p2 = interaction.getParticipation("P9");
		Assert.assertNotNull(p2);
		Assert.assertEquals(sbolDesign.getFunctionalComponent("FC3_wiredProtein"), p2.getParticipant());
		Assert.assertEquals(1, p2.getRoles().size());
		Assert.assertEquals(SystemsBiologyOntology.PRODUCT, p2.getRoles().iterator().next());
	}
}