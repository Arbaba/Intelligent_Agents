package template;

import java.util.HashMap;
import java.util.Random;

import javax.management.RuntimeErrorException;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class Reactive implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private HashMap<City, Integer> cityEncoding = new HashMap<City, Integer>();
	private StepAction bestAction[][];

	private enum StepAction{
		MOVE,
		PICKUP
	}

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		int numberCities = topology.cities().size();
		bestAction = new StepAction[numberCities][numberCities + 1];
		double gamma = 0.2;
		double qtable[][][] = new double[numberCities][numberCities + 1][2]; 


		double epsilon = 0.0000000001;
		double delta = 1;
		double rtable[][][] = new double[numberCities][numberCities + 1][2];
		double vtable[][]  = new double[numberCities][numberCities + 1];


		for(int i = 0; i < numberCities; i++){
			cityEncoding.put(topology.cities().get(i), i);
		}
		for(int i = 0; i < numberCities; i++){
			double noTaskCost = 0.0;
			for(int j=0; j < numberCities; j++){
				City from = topology.cities().get(i);
				City to =topology.cities().get(j);
				if(from == null || to == null){
					throw new RuntimeException("Null City");
				}
				for(int k=0; k< 2; k++){
					if(k == StepAction.MOVE.ordinal()){
						rtable[i][j][k] = -5 *  from.distanceTo(to);
						if(from.hasNeighbor(to)){
							noTaskCost +=  (td.reward(from, to) - 5 *  from.distanceTo(to))/ ((double)from.neighbors().size());
						}
					}else if (k == StepAction.PICKUP.ordinal()){
						rtable[i][j][k] = td.reward(from, to) - 5 *  from.distanceTo(to);
					}
				}	
			}
			//Reward of moving to a random city is the average of the cost for going to a random city
			rtable[i][numberCities][StepAction.MOVE.ordinal()] =  noTaskCost;
		}
		int iteration = 0;
		while(delta > epsilon){
			delta = 0;
			for(int i = 0; i < numberCities; i++){
				City from = topology.cities().get(i);
				for(int j=0; j < numberCities; j++){
					
					City to =topology.cities().get(j);
				
					double tSum = 0.0;
					//p((c2,c3) | Action, (c1,c2)) = p(c2,c3)
					for(City c: topology.cities()){
						if(from == null || to == null || c == null){
							throw new RuntimeException("Null City");
						}
						tSum += 2 * td.probability(to, c)* vtable[j][cityEncoding.get(c)];

					}
					//p((c2,#) | Action, (c1,c2)) = 1 - sum c3 <-neighbors(c2)[p(c2,c3)]
					double tSumNoTask =0.0;
					for(City c: to.neighbors()){
						if(from == null || to == null || c == null){
							throw new RuntimeException("Null City");
						}
						tSumNoTask += td.probability(to, c);
					}
					tSum += 2 * (1 - tSumNoTask) * vtable[i][i + 1];
					qtable[i][j][0] = rtable[i][j][0] + gamma * tSum;
					qtable[i][j][1] = rtable[i][j][1] + gamma * tSum;

					qtable[i][numberCities][StepAction.MOVE.ordinal()] = rtable[i][numberCities][StepAction.MOVE.ordinal()];
				}

			}
			for(int i = 0; i < numberCities; i++){
				for(int j=0; j < numberCities + 1; j++){
					if(qtable[i][j][StepAction.MOVE.ordinal()] > qtable[i][j][StepAction.PICKUP.ordinal()]){
							bestAction[i][j] = StepAction.MOVE;
							delta += Math.pow(vtable[i][j] - qtable[i][j][StepAction.MOVE.ordinal()], 2) ;
							vtable[i][j] = qtable[i][j][StepAction.MOVE.ordinal()];
					}else {
						bestAction[i][j] = StepAction.PICKUP;
						delta += Math.pow(vtable[i][j] - qtable[i][j][StepAction.PICKUP.ordinal()],2) ;
						vtable[i][j] = qtable[i][j][StepAction.PICKUP.ordinal()];
					}
				}
			}
			delta = Math.abs(delta);
			System.out.println(String.format("Agent ID: %d Iteration %d: delta: %f",agent.id(), iteration, delta));
			iteration++;
		}

	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			StepAction optimalAction = bestAction[cityEncoding.get(availableTask.pickupCity)][cityEncoding.get(availableTask.deliveryCity)];
			switch (optimalAction) {
				case MOVE:
					action = new Move(vehicle.getCurrentCity().randomNeighbor(random));
					break;
				case PICKUP:
					action = new Pickup(availableTask);
				default:
					action = new Pickup(availableTask);

					break;
			}
		}
		
		if (numActions >= 1) {
			System.out.println("Agent Reactive: "+ myAgent.id() + " The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}
