package fdtmc;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FDTMCTest {

	FDTMC fdtmc1;

	@Before
	public void setUp() throws Exception {
		fdtmc1 = new FDTMC();
	}

	@Test
	public void testEmptyFDTMC() {
		Assert.assertTrue(fdtmc1.getStates().isEmpty());
		Assert.assertNull(fdtmc1.getInitialState());
		Assert.assertEquals(0, fdtmc1.getVariableIndex());
	}

	/**
	 * This test ensures if a state is created accordingly. The returned state must be
	 * different than null, it must be inside the FDTMC's states set, it must have the
	 * index equals to 0 (it is the first state) and it variable name must be equals to
	 *  name defined.
	 */
	@Test
	public void testCreateState() {
		fdtmc1.setVariableName("x");
		State temp = fdtmc1.createState();
		Assert.assertNotNull(temp);
		Assert.assertTrue(fdtmc1.getStates().contains(temp));
		Assert.assertEquals(0, temp.getIndex());
		Assert.assertEquals("x", temp.getVariableName());
		Assert.assertEquals(temp, fdtmc1.getInitialState());
	}

	/**
	 * This test is similar to the test of creation a single state. However it ensures
	 * states created in sequence will have index value in sequence.
	 */
	@Test
	public void testCreateLotsOfStates() {		
		int numStates = 5;
		List<State> states = new ArrayList<>(numStates);
		fdtmc1.setVariableName("x");
		
		for (int i = 0; i < numStates; i++) {
			State state;
			state = fdtmc1.createState();
			
			Assert.assertNotNull(state);
			Assert.assertTrue(fdtmc1.getStates().contains(state));
			
			states.add(state);
		}
		
		for (int i = 0; i < numStates; i++) {
			Assert.assertEquals(i, states.get(i).getIndex());
		}

		Assert.assertEquals(states.get(0), fdtmc1.getInitialState());
	}


	/**
	 * This test ensures we can set a label to a state. It doesn't do too much,
	 * but it was useful to create the labeling function for states.
	 */
	@Test
	public void testCreateLabeledState() {
		fdtmc1.setVariableName("x");
		State s0, s1, s2;

		s0 = fdtmc1.createState("init");
		s1 = fdtmc1.createState("sucess");
		s2 = fdtmc1.createState("error");

		Assert.assertEquals("init", s0.getLabel());
		Assert.assertEquals("sucess", s1.getLabel());
		Assert.assertEquals("error", s2.getLabel());

		Assert.assertEquals(s0, fdtmc1.getInitialState());
	}


	/**
	 * This test ensures we can create transitions between FDTMC's states, passing the states,
	 * transition name and probability value as parameters.
	 */
	@Test
	public void testCreateTransition() {
		State s0, s1, s2;
		s0 = fdtmc1.createState("init");
		s1 = fdtmc1.createState("success");
		s2 = fdtmc1.createState("error");

		Assert.assertNotNull(fdtmc1.createTransition(s0, s1, "alpha", Double.toString(0.95)));
		Assert.assertNotNull(fdtmc1.createTransition(s0, s2, "alpha", Double.toString(0.05)));
	}


	/**
	 * This test is similar to the test above (testCreateTransition), however it test if the
	 * creation of transitions with parameters instead of real values works accordingly.
	 */
	@Test
	public void testCreateTransitionWithParameter() {
		State s0, s1, s2;
		s0 = fdtmc1.createState("init");
		s1 = fdtmc1.createState("success");
		s2 = fdtmc1.createState("error");

		Assert.assertNotNull(fdtmc1.createTransition(s0, s1, "alpha", "rAlpha"));
		Assert.assertNotNull(fdtmc1.createTransition(s0, s2, "alpha", "1-rAlpha"));
	}


	/**
	 * This test ensures a created state can be recovered by its label.
	 */
	@Test
	public void testGetStateByLabel() {
		State s0, s1, s2;
		s0 = fdtmc1.createState("init");
		s1 = fdtmc1.createState("success");
		s2 = fdtmc1.createState("error");

		State t0, t1, t2;
		t0 = fdtmc1.getStateByLabel("init");
		t1 = fdtmc1.getStateByLabel("success");
		t2 = fdtmc1.getStateByLabel("error");

		Assert.assertSame(t0, s0);
		Assert.assertSame(t1, s1);
		Assert.assertSame(t2, s2);
	}


	/**
	 * This test ensures it is possible to recover a transition (and all of its information like
	 * probability and source and target states) by using its name.
	 */
	@Test
	public void testGetTransitionByActionName() {
		State s0, s1, s2;
		s0 = fdtmc1.createState("init");
		s1 = fdtmc1.createState("sucess");
		s2 = fdtmc1.createState("error");

		Assert.assertNotNull(fdtmc1.createTransition(s0, s1, "alpha", "rAlpha"));
		Assert.assertNotNull(fdtmc1.createTransition(s0, s2, "alpha_error", "1-rAlpha"));

		Transition t1, t2;
		t1 = fdtmc1.getTransitionByActionName("alpha");
		t2 = fdtmc1.getTransitionByActionName("alpha_error");

		Assert.assertNotNull(t1);
		Assert.assertEquals("alpha", t1.getActionName());
		Assert.assertEquals("rAlpha", t1.getProbability());
		Assert.assertSame(s0, t1.getSource());
		Assert.assertSame(s1, t1.getTarget());

		Assert.assertNotNull(t2);
		Assert.assertEquals("alpha_error", t2.getActionName());
		Assert.assertEquals("1-rAlpha", t2.getProbability());
		Assert.assertSame(s0, t2.getSource());
		Assert.assertSame(s2, t2.getTarget());
	}



	/**
	 * This test must ensure that the FDTMC will be printed (or builded) considering the order
	 * the states and transitions were build.
	 */
	@Test
	public void testPrintOrderedFDTMC (){
		FDTMC fdtmc = new FDTMC();
		fdtmc.setVariableName("sSqlite");
		State init = fdtmc.createState("init"),
			  success = fdtmc.createState("success"),
			  error = fdtmc.createState("fail"),
			  source,
			  target;

		source = init;
		target = fdtmc.createState();
		Assert.assertNotNull(fdtmc.createTransition(source, target, "persist", "0.999"));
		Assert.assertNotNull(fdtmc.createTransition(source, error, "persist", "0.001"));

		source = target;
		target = success;
		Assert.assertNotNull(fdtmc.createTransition(source, target, "persist_return", "0.999"));
		Assert.assertNotNull(fdtmc.createTransition(source, target, "persist_return", "0.001"));

		Assert.assertNotNull(fdtmc.createTransition(success, success, "", "1.0"));
		Assert.assertNotNull(fdtmc.createTransition(error, error, "", "1.0"));


		String expectedAnswer = "sSqlite=0(init) --- persist / 0.999 ---> sSqlite=3" + '\n'
				+ "sSqlite=0(init) --- persist / 0.001 ---> sSqlite=2(fail)" + '\n'
				+ "sSqlite=1(success) ---  / 1.0 ---> sSqlite=1(success)" + '\n'
				+ "sSqlite=2(fail) ---  / 1.0 ---> sSqlite=2(fail)" + '\n'
				+ "sSqlite=3 --- persist_return / 0.999 ---> sSqlite=1(success)" + '\n'
				+ "sSqlite=3 --- persist_return / 0.001 ---> sSqlite=1(success)" + '\n';

		Assert.assertEquals(expectedAnswer, fdtmc.toString());
	}


	@Test
	public void testEquivalentFDTMCs() {
	    FDTMC fdtmc1 = new FDTMC();
        fdtmc1.setVariableName("s");
        State init1 = fdtmc1.createInitialState(),
              success1 = fdtmc1.createSuccessState(),
              error1 = fdtmc1.createErrorState(),
              source,
              target,
              interface_error;

        source = init1;
        target = fdtmc1.createState();
        fdtmc1.createTransition(source, target, "persist", "0.999");
        fdtmc1.createTransition(source, error1, "!persist", "0.001");

        source = target;
        target = fdtmc1.createState();
        interface_error = fdtmc1.createState();
        fdtmc1.createInterface("F", source, target, interface_error);

        fdtmc1.createTransition(interface_error, error1, "error_ground", "1");

        source = target;
        target = success1;
        fdtmc1.createTransition(source, target, "persist_return", "0.999");
        fdtmc1.createTransition(source, error1, "!persist_return", "0.001");

        FDTMC fdtmc2 = new FDTMC();
        fdtmc2.setVariableName("v");
        State init2 = fdtmc2.createInitialState(),
              success2 = fdtmc2.createSuccessState(),
              error2 = fdtmc2.createErrorState();

        source = init2;
        target = fdtmc2.createState();
        fdtmc2.createTransition(source, target, "msg", "0.999");
        fdtmc2.createTransition(source, error2, "!msg", "0.001");

        source = target;
        target = fdtmc2.createState();
        interface_error = fdtmc2.createState();
        fdtmc2.createInterface("G", source, target, interface_error);

        fdtmc2.createTransition(interface_error, error2, "error_ground", "1");

        source = target;
        target = success2;
        fdtmc2.createTransition(source, target, "msg_return", "0.999");
        fdtmc2.createTransition(source, error2, "!msg_return", "0.001");

        Assert.assertEquals("FDTMCs' states should be compared disregarding variable names",
                init1, init2);
        Assert.assertNotEquals("FDTMCs' states should be compared disregarding variable names",
                success1, error2);

        Assert.assertEquals("FDTMCs' states should be compared disregarding variable names",
                fdtmc1.getStates(), fdtmc2.getStates());

        Assert.assertEquals("FDTMCs should be compared disregarding actions' names, interfaces' names and variable names",
                fdtmc1, fdtmc2);
	}

}
