package edu.upc.fib.masd.jav;

import edu.upc.fib.masd.jav.utils.Field;
import edu.upc.fib.masd.jav.utils.FieldState;
import sml.Identifier;
import sml.Kernel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Environment {
    // We keep references to Agents.
    private ArrayList<GeneralAgent> agents;
    // Create executor services to run Soar in since it blocks.
    private ArrayList<ExecutorService> executors;
    // To read user input
    private BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

    public Environment(ArrayList<GeneralAgent> agents) {
        this.agents = agents;
        this.executors = initExecutors();

        runSystemCycle();
    }

    private void runSystemCycle() {
        try {
            // Necessary delay (ms)
            delay(1000);

            // Loop
            while (!Thread.interrupted()) {
                System.out.println("========================");
                System.out.println("Press enter to continue (or X to exit):");
                String line = input.readLine();

                if (line.equalsIgnoreCase("x")) {
                    shutdown();
                }

                runAllAgentsOneStep();
                updateEnvironmentState();

                // Necessary delay (ms)
                delay(1000);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private void runAllAgentsOneStep() {
        for (GeneralAgent agent : agents) {
            agent.runStep();
        }
    }

    private void updateEnvironmentState() {
        for (int i = 0; i < this.agents.size(); ++i) {
            // Update agents
            this.agents.get(i).decreaseSatiety();
        }
    }


    private ArrayList<ExecutorService> initExecutors() {
        ArrayList<ExecutorService> exec = new ArrayList<ExecutorService>();
        for (int i = 0; i < this.agents.size(); ++i) {
            exec.add(Executors.newSingleThreadExecutor());
        }
        return exec;
    }

    private void shutdown() {
        // Shutdown the Soar interface and the executor service.
        for (int i = 0; i < this.agents.size(); ++i) {
            this.agents.get(i).shutdown();
            this.executors.get(i).shutdown();
        }
        System.exit(0);
    }

    private void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<GeneralAgent> createAgents(Kernel kernel, int numBarons, int numCollectors, int numBuilders) {
        ArrayList<GeneralAgent> allAgents = new ArrayList<GeneralAgent>();

        int food = 5;
        int foodSatiety = 15;
        int wood = 10;
        int numFields = 2;
        FieldState fieldState = FieldState.DRY;
        int fieldYield = 2;

        // Barons
        for (int i = 0; i < numBarons; ++i) {
            BaronAgent baron = new BaronAgent(kernel, "Baron_" + i, "SOAR_Codes/PRESET_baron_agent.soar", food, foodSatiety);
            baron.getAgent().RunSelf(0);
            allAgents.add(baron);

            // Collectors
            for (int j = 0; j < numCollectors; ++j) {
                CollectorAgent collector = new CollectorAgent(kernel, "Collector_" + j, "SOAR_Codes/PRESET_collector_agent.soar", baron, food, foodSatiety, wood);
                Identifier fieldsRoot = collector.inputLink.CreateIdWME("fields");
                for (int k = 0; k < numFields; ++k) {
                    Field field = new Field(collector, fieldsRoot, "Field_" + k, FieldState.DRY, fieldYield);
                    collector.addField(field);
                }
                collector.getAgent().RunSelf(0);
                baron.addCollector(collector);
                allAgents.add(collector);
            }

            // Builders
            for (int j = 0; j < numBuilders; ++j) {
                BuilderAgent builder = new BuilderAgent(kernel, "Builder_" + j, "SOAR_Codes/PRESET_builder_agent.soar", baron, food, foodSatiety);
                builder.getAgent().RunSelf(0);
                baron.addBuilder(builder);
                allAgents.add(builder);
            }
        }
        return allAgents;
    }

    public static void main(String[] args) {
        // Create the kernel
        final int kernelPort = 27390;
        Kernel k = Kernel.CreateKernelInNewThread(kernelPort);
        if (k.HadError()) {
            System.err.println("Error creating kernel: " + k.GetLastErrorDescription());
            System.exit(1);
        }

        // Create all the agents and load productions
        int numBarons = 1;
        int numCollectors = 2;
        int numBuilders = 0;
        ArrayList<GeneralAgent> agentsArray = createAgents(k, numBarons, numCollectors, numBuilders);

        // Spawn debugger just for testing
        agentsArray.get(0).getAgent().SpawnDebugger(kernelPort, "libs/soar/SoarJavaDebugger.jar");

        // Create the Soar environment and add the agents
        new Environment(agentsArray);
    }
}

