package template;

import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import javax.management.RuntimeErrorException;
import javax.print.StreamPrintService;

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

	private class StepAction{

	}
	private class MOVE extends StepAction{
		private City destination;
		public MOVE(City d){
			this.destination = d;
		}
		public City getDestination(){
			return this.destination;
		}
		@Override
		public int hashCode(){
			int hash = 5581;
			return hash * 33 + this.destination.hashCode();
		}
		
		@Override
		public boolean equals(Object that) {
			if (!(that instanceof MOVE)) 
				return false;
			MOVE MOVE = (MOVE) that;
			return this.destination.equals(MOVE.destination);
		}
	}
	private class PICKUP extends StepAction{
		@Override
		public int hashCode(){
			int hash = 6363;
			return hash * 33;
		}
		
		@Override
		public boolean equals(Object that) {
			return that instanceof PICKUP;
		}	
	}
	private  class State{
		private City from;
		private City to;
		public State(City from, City to){
			this.from = from;
			this.to = to;
		}
		
		@Override
		public int hashCode() {
			int hash = 5581;

			// TODO Auto-generated method stub
			hash = 33 * hash + this.from.hashCode();
			if(to != null){
				return  hash*33 + this.to.hashCode();
			}else {
				return hash;
			}
		}

		@Override
		public boolean equals(Object that) {
			if (!(that instanceof State)) 
				return false;
			State s1 = (State) that;
			if(this.to == null && s1.to == null) {
				return this.from.equals(s1.from);
			}else {
				return this.from.equals(s1.from) && this.to.equals(s1.to);
			}
		}
	}

	
	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;

	private List<State> states;
	private List<StepAction> actions;
	private  HashMap<State, HashMap<StepAction, Double>> qtable ;
	private  HashMap<State,Double> vtable ;
	private  HashMap<State, HashMap<StepAction, Double>> rtable ;
	private  HashMap<State,StepAction> besttable ;

	Topology topology;
	TaskDistribution td;
	double gamma = 0.3;

	private double rewardLookup(State s, StepAction action){
		return rtable.get(s).get(action);
	}

	private double transitionLookup(State nextState){
		return td.probability(nextState.from, nextState.to);
	}

	private double vLookup(State nextState, StepAction action){
		return vtable.get(nextState);
	}
	private void buildRtable(){
		for(State s : states){
			for(StepAction action: actions){
	
				 if (action instanceof MOVE){
					 MOVE MOVE  = (MOVE) action;
					rtable.get(s).put(action,  5 *  s.from.distanceTo(MOVE.destination));
				}else if(action instanceof PICKUP) {
					rtable.get(s).put(action, (td.reward(s.from, s.to) - 5 *  s.from.distanceTo(s.to)));

				}
			}
		}
	}
	private double updateBestAction(State s){
		Double max = Double.NEGATIVE_INFINITY;
		double prev = vtable.get(s);
		for(Map.Entry<StepAction, Double> entry: qtable.get(s).entrySet()){
			if(entry.getValue() > max ){
				max = entry.getValue();
				besttable.put(s, entry.getKey());
				vtable.put(s, entry.getValue());
			}
		}
		return Math.pow(prev - max,2 );

	}


	private void valueIteration(){
		double epsilon = 0.0001;
		double delta = 10;
		while(delta > epsilon){
			delta = 0.0;
			for(State s: states){
				for(StepAction action : actions){
					Double value = rewardLookup(s, action) ;
					for(State sPrime: states){
						value += transitionLookup(sPrime) * vLookup(sPrime, action); 
					}
					qtable.get(s).put(action, rewardLookup(s, action) + gamma * value);
					//System.out.println( "--" + rewardLookup(s, action) + gamma * value);
				}
				delta += updateBestAction(s);
				StepAction bestaction = besttable.get(s);
				vtable.put(s, qtable.get(s).get(bestaction));
			}
			System.out.println("DELTA: " + delta);
		}

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
		this.td = td;
		this.states = new ArrayList<Reactive.State>();
		this.vtable = new HashMap<Reactive.State,Double>();
		this.rtable = new HashMap<Reactive.State,HashMap<StepAction,Double>>();
		this.qtable = new HashMap<Reactive.State,HashMap<StepAction,Double>>();
		this.besttable = new HashMap<Reactive.State,Reactive.StepAction>();
		this.actions = new ArrayList<Reactive.StepAction>();
		this.actions.add(new PICKUP());

		for(City from: topology.cities()){
			this.actions.add(new MOVE(from));
			for(City to: topology.cities()){
				State state = new State(from, to);
				states.add(state);
				System.out.println(state.from.name + " " + state.to.name);
			}
			//states.add(new State(from, null));
		}
		for(State state: states){
			vtable.put(state, Double.valueOf(0.0) );
			qtable.put(state, new HashMap<Reactive.StepAction,Double>());
			rtable.put(state, new HashMap<StepAction, Double>());
			for(StepAction action: actions){
				rtable.get(state).put(action, 0.0);
				qtable.get(state).put(action, 0.0);
				besttable.put(state,action);

			}

		}
		
		System.out.println(states.size() == besttable.size());
		for(Map.Entry<State, StepAction> s: besttable.entrySet()){
			System.out.println("--- " + s.getKey().from.name + " " + ((s.getKey().to == null) ? "": s.getKey().to.name)+ " "+ s.getKey().hashCode());
		}
		buildRtable();
		valueIteration();
		/*int numberCities = topology.cities().size();
		bestAction = new StepAction[numberCities][numberCities + 1];
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
					if(qtable[i][j][StepAction.MOVE.ordinal()] > qtable[i][j][StepAction.PICKUP.ordinal()]){
						bestAction[i][j] = StepAction.MOVE;
						delta += Math.pow(vtable[i][j] - qtable[i][j][StepAction.MOVE.ordinal()], 2) ;
						vtable[i][j] = qtable[i][j][StepAction.MOVE.ordinal()];
					}else {
						bestAction[i][j] = StepAction.PICKUP;
						delta += Math.pow(vtable[i][j] - qtable[i][j][StepAction.PICKUP.ordinal()],2) ;
						vtable[i][j] = qtable[i][j][StepAction.PICKUP.ordinal()];
					}
					delta = Math.abs(delta);
					System.out.println(String.format("Agent ID: %d Iteration %d: delta: %f",agent.id(), iteration, delta));
					iteration++;
				}

			}
			
			
		}*/

	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			State s = new State(vehicle.getCurrentCity(), availableTask.deliveryCity);
			System.out.println(s.hashCode());

			StepAction optimalAction = besttable.get(s);

			if(optimalAction instanceof MOVE){
				action = new Move(((MOVE)optimalAction).getDestination());
			}else {
				action = new Pickup(availableTask);
			}
			/*if(optimalAction == null)throw new RuntimeException(vehicle.getCurrentCity().name + " " + ((availableTask.deliveryCity == null) ? "": availableTask.deliveryCity.name));
			switch (optimalAction) {
				case MOVE:
					action = new Move(vehicle.getCurrentCity().randomNeighbor(random));
					break;
				case PICKUP:
					action = new Pickup(availableTask);
				default:
					action = new Pickup(availableTask);

					break;
			}*/
		}
		
		if (numActions >= 1) {
			System.out.println("Agent Reactive: "+ myAgent.id() + " The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}
