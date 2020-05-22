package edu.upc.fib.masd.jav;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import sml.Agent;
import sml.Identifier;
import sml.Kernel;
import sml.smlUpdateEventId;
import sml.Kernel.UpdateEventInterface;
import sml.WMElement;
import sml.smlRunStepSize;

public abstract class SoarAgent
{
	protected final Kernel kernel;

	protected final Agent agent;

	// Input-link WME so that we can quickly add additional messages as they come in.
	protected Identifier inputLink;
	
	// To ask Soar to stop executing.
	private final AtomicBoolean stopSoar = new AtomicBoolean(true);


	public SoarAgent(Kernel k, String agentName, String productionsFile) {
		kernel = k;
		agent = kernel.CreateAgent(agentName);
		
		if (agent == null)
		{
			System.err.println("Error creating agent: " + kernel.GetLastErrorDescription());
			System.exit(1);
		}

		// Load the productions.
		if (!agent.LoadProductions(productionsFile)) {
			System.err.println("Can't load " + productionsFile + " file.");
			System.exit(1);
		}

		// Create and cache input-link and output-link
		inputLink = agent.GetInputLink();
	}
	
	public void runStep() {
		System.out.println("Agent " + agent.GetAgentName() + " run step");
		this.agent.RunSelf(1, smlRunStepSize.sml_UNTIL_OUTPUT);
	}

	public void stop() {
		// Ask the agent to stop itself during its next update event.
		stopSoar.set(true);
	}

	public void shutdown() {
		stop();
		try {
			Thread.sleep(500);
		}
		catch (InterruptedException ignored){}
		kernel.Shutdown();
	}
	
	public Agent getAgent() {
		return agent;
	}
	
	public void readAndTreatOutput() {
		// Iterate through the commands on the output link.
		for (int index = 0; index < agent.GetOutputLink().GetNumberChildren(); ++index)
		{
			// Get command
			WMElement command = agent.GetOutputLink().GetChild(index);
			treatCommand(command);
		}
	}
	
	public abstract void treatCommand(WMElement command); 
	
}