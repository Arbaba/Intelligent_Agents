import java.awt.Color;

import java.util.ArrayList;
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
		private static final int NUMINITRABBITS = 5;
		private static final int NUMINITGRASS = 10;
		private static final double GRASSGROWTHRATE = 5;
		private static final int BRITHTHRESHOLD = 40;
	
		private Schedule schedule;
		private int gridSize = GRIDSIZE;
		private int numInitRabbits = NUMINITRABBITS;
		private int numInitGrass = NUMINITGRASS;
		private double grassGrowthRate = GRASSGROWTHRATE;
		private int birthThreshold = BRITHTHRESHOLD;
		private ArrayList<RabbitsGrassSimulationAgent> agentList;
		private DisplaySurface displaySurf;
		private RabbitsGrassSimulationSpace rgSpace;
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
			// TODO Auto-generated method stub
			buildModel();
			buildSchedule();
			buildDisplay();
			displaySurf.display();
		}

		public String[] getInitParam() {
			// TODO Auto-generated method stub
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

			class CarryDropStep extends BasicAction {
				public void execute() {
				  SimUtilities.shuffle(agentList);
				  for(int i =0; i < agentList.size(); i++){
					RabbitsGrassSimulationAgent agent = agentList.get(i);

					//agent.step();
					displaySurf.updateDisplay();
				  }
				}
			  }
		  
			  schedule.scheduleActionBeginning(0, new CarryDropStep());
			}		

		public void buildDisplay(){
			System.out.println("Building Display");

			ColorMap map = new ColorMap();

			for(int i = 1; i<16; i++){
			  map.mapColor(i, new Color((int)(i * 8 + 127), 0, 0));
			}
			map.mapColor(0, Color.CYAN);
		
			Value2DDisplay displayRabbitGrass =
				new Value2DDisplay(rgSpace.getSpace(), map);
			Object2DDisplay displayAgents = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
			displayAgents.setObjectList(agentList);
		
			displaySurf.addDisplayable(displayRabbitGrass, "Money");
			displaySurf.addDisplayable(displayAgents, "Agents");
		}

		public void setup() {
			// TODO Auto-generated method stub
			rgSpace = null;
			agentList = new ArrayList<RabbitsGrassSimulationAgent>();
			schedule = new Schedule(1);
			if (displaySurf != null){

				displaySurf.dispose();
			  }
			  displaySurf = null;
		  
			  displaySurf = new DisplaySurface(this, "Rabit Grass Model Window 1");
		  
			  registerDisplaySurface("Rabit Grass Model Window 1", displaySurf);
			}

		public int randomIndex(){
			return  (int)(Math.random()*(rgSpace.getDim()));
		}
		
		private void addNewAgent(){
			RabbitsGrassSimulationAgent rabbitAgent = new RabbitsGrassSimulationAgent(randomIndex(), randomIndex());
			rgSpace.addAgent(rabbitAgent);
			agentList.add(rabbitAgent);
		}
		public String getName() {
			// TODO Auto-generated method stub
			return "Rabbit Grass Simulation";
		}

		public Schedule getSchedule() {
			// TODO Auto-generated method stub
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

		
		public double getGrassGrowthRate(){
			return grassGrowthRate;
		}

		public void setGrassGrowthRate(double grassGrowthRate){
			this.grassGrowthRate = grassGrowthRate;
		}
		
		public int getBirthThreshold(){
			return birthThreshold;
		}

		public void setBirthThreshold(int birthThreshold){
			this.birthThreshold =  birthThreshold;
		}


}
