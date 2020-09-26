import java.awt.Color;

import java.util.ArrayList;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.util.SimUtilities;
/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {
		// Default values
		private static final int GRIDSIZE = 20;
		private static final int NUMINITRABBITS = 100;
		private static final int NUMINITGRASS = 50;
		private static final int GRASSGROWTHRATE = 30;
		public 	static final int BIRTHTHRESHOLD = 30;
		private static final int NUMINITLIFE = 20;
		private static final int REPRODUCTIONCOST = 10;

		private Schedule schedule;
		private int gridSize = GRIDSIZE;
		private int numInitRabbits = NUMINITRABBITS;
		private int numInitGrass = NUMINITGRASS;
		private int grassGrowthRate = GRASSGROWTHRATE;
		private int birthThreshold = BIRTHTHRESHOLD;
		private ArrayList<RabbitsGrassSimulationAgent> agentList;
		private DisplaySurface displaySurf;
		private RabbitsGrassSimulationSpace rgSpace;
		private OpenSequenceGraph graphs;
		public static void main(String[] args) {
			
			System.out.println("Rabbit skeleton");

			SimInit init = new SimInit();
			RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
			// Do "not" modify the following lines of parsing arguments
			if (args.length == 0) // by default, you don't use parameter file nor batch mode 
				init.loadModel(model, "", false);
			else
				init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
			
		}

		public void begin() {
			buildModel();
			buildSchedule();
			buildDisplay();
			displaySurf.display();
			graphs.display();
		}

		public String[] getInitParam() {
			// Parameters to be set by users via the Repast UI slider bar
			// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
			String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold"};
			return params;
		}

		public void buildModel(){
			System.out.println("Building Model");
			rgSpace = new RabbitsGrassSimulationSpace(getGridSize(), getGridSize());
			rgSpace.spreadGrass(getNumInitGrass());
			for(int i = 0; i < numInitRabbits; i++){
				addNewAgent();
			}
			for(int i = 0; i < agentList.size(); i++){
				RabbitsGrassSimulationAgent agent = agentList.get(i);
				agent.report();
			}		
		}
		
		public void buildSchedule(){
			System.out.println("Building Schedule");

			class SimulationStep extends BasicAction {
				public void execute() {
					SimUtilities.shuffle(agentList);

					rgSpace.spreadGrass(grassGrowthRate);
					ArrayList<RabbitsGrassSimulationAgent> readyToReproduce = new ArrayList<RabbitsGrassSimulationAgent>();
					for(int i =0; i < agentList.size(); i++){
						RabbitsGrassSimulationAgent agent = agentList.get(i);

						if(countLivingAgents() + readyToReproduce.size() < rgSpace.getDimX() *rgSpace.getDimY() && agent.getEnergy()>= birthThreshold){
							readyToReproduce.add(agent);
						}
						agent.step();

					}
						
					for (RabbitsGrassSimulationAgent agent : readyToReproduce) {
						addNewAgent();
						agent.payReproduction(REPRODUCTIONCOST);
					}
					reapDeadAgents();

					displaySurf.updateDisplay();
				}
			  }
		  
			  schedule.scheduleActionBeginning(10, new SimulationStep());

			  class RabbitCountLiving extends BasicAction {
				public void execute(){
				  countLivingAgents();
				}
			  }
		  
			  schedule.scheduleActionAtInterval(1, new RabbitCountLiving());
		  
			  class GrassCountInSpace extends BasicAction {
				public void execute(){
				  graphs.step();
				}
			  }
		  
			  schedule.scheduleActionAtInterval(1, new GrassCountInSpace());
			}		

		public void buildDisplay(){
			System.out.println("Building Display");

			ColorMap map = new ColorMap();

			for(int i = 1; i< 200; i++){
				//Denser Grass will be lighter
				map.mapColor(i, new Color(0, 55 + i, 0));
			}
			final Color BROWN = new Color(139,69,19);
			map.mapColor(0, BROWN);
		
			Value2DDisplay displayGrass =
				new Value2DDisplay(rgSpace.getGrassSpace(), map);
			Object2DDisplay displayAgents = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
			displayAgents.setObjectList(agentList);
		
			displaySurf.addDisplayable(displayGrass, "Grass");
			displaySurf.addDisplayable(displayAgents, "Agents");
			
			graphs.addSequence("Grass In Space", new grassInSpace());
			graphs.addSequence("Rabbits In Space", new rabbitInSpace());
		}

		class grassInSpace implements DataSource, Sequence {

			public Object execute() {
			  return new Double(getSValue());
			}
		
			public double getSValue() {
			  return (double)rgSpace.getTotalGrass();
			}
		}
		class rabbitInSpace implements DataSource, Sequence {

			public Object execute() {
			  return new Double(getSValue());
			}
		
			public double getSValue() {
			  return (double) countLivingAgents();
			}
		}
		public int countLivingAgents(){
			int count = 0;
			for (RabbitsGrassSimulationAgent agent : agentList) {
				if(agent.getEnergy() > 0){
					count += 1;
				}
			}
			return count;
		}
		public void setup() {
			rgSpace = null;
			agentList = new ArrayList<RabbitsGrassSimulationAgent>();
			schedule = new Schedule(1);
			if (displaySurf != null){

				displaySurf.dispose();
			}
			displaySurf = null;
		
			displaySurf = new DisplaySurface(this, "Rabit Grass Model Window 1");
		
			registerDisplaySurface("Rabit Grass Model Window 1", displaySurf);
			
			graphs = new OpenSequenceGraph("Amount Of Grass In Space",this);

			// Register Displays
			registerDisplaySurface("Carry Drop Model Window 1", displaySurf);
			this.registerMediaProducer("Plot", graphs);
		}

		private void addNewAgent(){
			RabbitsGrassSimulationAgent rabbitAgent = new RabbitsGrassSimulationAgent( (int)(Math.random()*(rgSpace.getDimX())),  (int)(Math.random()*(rgSpace.getDimY())),NUMINITLIFE);
			rgSpace.addAgent(rabbitAgent);
			agentList.add(rabbitAgent);

		}

		private int reapDeadAgents(){
			int count = 0;
			for(int i = (agentList.size() - 1); i >= 0 ; i--){
			  RabbitsGrassSimulationAgent rabbit = agentList.get(i);
			  if(rabbit.getEnergy() < 1){
				rgSpace.removeAgentAt(rabbit.getX(), rabbit.getY());
				agentList.remove(i);
				count++;
			  }
			}
			return count;
		  }
		public String getName() {
			return "Rabbit Grass Simulation";
		}

		public Schedule getSchedule() {
			return schedule;
		}

		public int getGridSize(){
			return gridSize;
		} 

		public void setGridSize(int gridSize){
			this.gridSize = gridSize;
		} 

		public int getNumInitRabbits(){
			return numInitRabbits;
		}

		public void setNumInitRabbits(int numInitRabbits){
			this.numInitRabbits = numInitRabbits;
		}

		public int getNumInitGrass(){
			return numInitGrass;
		}

		public void setNumInitGrass(int numInitGrass){
			this.numInitGrass = numInitGrass;
		}

		
		public int getGrassGrowthRate(){
			return grassGrowthRate;
		}

		public void setGrassGrowthRate(int grassGrowthRate){
			this.grassGrowthRate = grassGrowthRate;
		}
		
		public int getBirthThreshold(){
			return birthThreshold;
		}

		public void setBirthThreshold(int birthThreshold){
			this.birthThreshold =  birthThreshold;
		}


}
