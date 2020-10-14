package template;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	public class State{
		City currentCity;
		List<Task> toPick;
		List<Task> picked;

		public State(City currentCity, List<Task> toPick, List<Task> picked){
			this.currentCity = currentCity;
			this.toPick = new ArrayList<Task>(toPick);
			this.picked = new ArrayList<Task>(picked);
		}
		public State(City currentCity, List<Task> toPick){
			this.currentCity = currentCity;
			this.toPick = new ArrayList<Task>(toPick);
			this.picked = new ArrayList<Task>();
		}
		//removes task from picked
		public State removePickedTask(Task t){
			List<Task> updatedToPick = new ArrayList<Task>(toPick);
			updatedToPick.remove(t);
			return new State(currentCity, updatedToPick, picked);
		}
		//moves task from toPick and adds it to picked
		public State pickTask(Task t){
			List<Task> updatedToPick = new ArrayList<Task>(toPick);
			List<Task> updatedPicked = new ArrayList<Task>(picked);
			updatedPicked.add(t);
			updatedToPick.remove(t);
			return new State(currentCity, updatedToPick, updatedPicked);
		} 

		public State moveTo(City city){
			return new State(city, toPick, picked);
		}
		public boolean isGoal(){
			return (toPick.size()==0 && picked.size()==0);

		}

		@Override
		public boolean equals(Object that){
			if (!(that instanceof State)) 
			return false;
			State thatState = (State) that;
			return this.currentCity.equals(thatState.currentCity)
					&& toPick.equals(thatState.toPick)
					&& picked.equals(thatState.picked);
		}

		
		@Override
		public String toString() {
			return String.format("State {city: %s, picked: %s, toPick: %s}", currentCity, picked, toPick);
		}

	}
	
	public class Node{
		State state;
		//List<Neighbor> neighbors;
		public Node(State state){
			this.state = state;
		}
		public boolean isGoal(){
			return state.isGoal();
		}
		@Override
		public boolean equals(Object that){
			if (!(that instanceof Node)) 
			return false;
			Node thatNode = (Node) that;
			return this.state.equals(thatNode.state);
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return String.format("Node { %s}", state);
		}
	
	}
	public class Neighbor{
		double cost;
		Node node;
		Plan plan;
		public Neighbor(Node node, Plan plan, double cost){
			this.node = node;
			this.plan = plan;
			this.cost = cost;
		}

		public void decrease(double cost2) {
			this.cost -= cost2;
		}

		return String.format("Neighbor {node: %s, plan: %s, cost %f}", node, plan, cost);
	}
	
	
	
	public List<Neighbor> computeNeighbors(State state, ArrayList<Action> prevActions, City initialCity){
		List<Neighbor> neighbors =  new ArrayList<Neighbor>();
		if(state.isGoal()){
			System.out.println("no neighbors");
			return neighbors;
		}
		//delivery
		for(Task task: state.picked){
			if(task.deliveryCity == state.currentCity){
				System.out.println("DELIVER");
				State newState = state.removePickedTask(task);
				List<Action> actions = new ArrayList<Action>(prevActions);
				actions.add(new Delivery(task));
				Node newNode = new Node(newState);
				int cost = 0;
				neighbors.add(new Neighbor(newNode, new Plan(initialCity, actions), cost));
			}
		}
			
		//pickup
		for(Task task: state.toPick){
			if(state.currentCity == task.pickupCity){
				System.out.println("PICK");
				State newState = state.pickTask(task);
				List<Action> actions = new ArrayList<Action>(prevActions);
				actions.add(new Pickup(task));
				Node newNode = new Node(newState);
				int cost = 0;
				neighbors.add(new Neighbor(newNode, new Plan(initialCity, actions), cost));
			}
			
		}
		//move
		for(City city: state.currentCity.neighbors()){
			System.out.println("MOVE");
			State newState = state.moveTo(city);
			Node newNode = new Node(state);
			double cost = state.currentCity.distanceTo(city);
			List<Action> actions = new ArrayList<Action>(prevActions);
			actions.add(new Move(city));
			neighbors.add(new Neighbor(newNode, new Plan(initialCity, actions), cost));
		}
		return neighbors;
	}


	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = aStarPlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = bfsPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks){
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
	class NeighborComparator implements Comparator<Neighbor> 
	{ 
		// Used for sorting in ascending order of 
		// roll number 
		public int compare(Neighbor a, Neighbor b) 
		{ 
			return (int) (a.cost - b.cost); 
		} 
	}
	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks){
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		List<Task> toPick = new ArrayList<Task>();

		for(Task t: tasks){
			toPick.add(t);
		}
		Node initNode = new Node(new State(current, toPick));

		LinkedList<Neighbor> Q = new LinkedList<Neighbor>();
		Q.add(new Neighbor(initNode, Plan.EMPTY, 0));
		LinkedList<Node> C = new LinkedList<Node>();
		Neighbor bestNode = null;
		double minCost = Double.MAX_VALUE;
		int counter = 0;
		while(Q.size() != 0){
			System.out.println(counter);
			counter++;
			Neighbor neighbor = Q.pop();
			Node node = neighbor.node;
			double cost = neighbor.cost;
			if(node.isGoal()){
				System.out.println("Goal reached");
				return neighbor.plan;
			}
			System.out.println("to pick: " + neighbor.node.state.toPick.size());

			if(!C.contains(node)){
				C.add(node);//put in list of visited nodes
				ArrayList<Action> prevActions = new ArrayList<Action>();
				for(Action action : neighbor.plan){
					prevActions.add(action);
				}
				List<Neighbor> neighbors = computeNeighbors(node.state, prevActions, initNode.state.currentCity);
				for(Neighbor n : neighbors){
					Q.add(n);
				}
				for(Neighbor n: neighbors){
					n.decrease(cost);
				}
				Collections.sort(Q, new NeighborComparator());
			}else {
				System.out.println("discard");

			}
		}
		System.out.println("Done");
		return plan;

	}
	 Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
