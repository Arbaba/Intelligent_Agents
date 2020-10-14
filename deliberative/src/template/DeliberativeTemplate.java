package template;

import java.util.List;
import java.util.ArrayList;

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
		//removes task from picked
		public State removePickedTask(Task t){
			List<Task> updatedToPick = new ArrayList<Task>(toPick);
			updatedToPick.remove(t);
			return new State(currentCity, toPick, picked);
		}
		//moves task from toPick and adds it to picked
		public State pickTask(Task t){
			List<Task> updatedToPick = new ArrayList<Task>(toPick);
			List<Task> updatedPicked = new ArrayList<Task>(picked);
			updatedPicked.add(t);
			updatedToPick.remove(t);
			return new State(currentCity, toPick, picked);
		} 

		public State moveTo(City city){
			return new State(city, toPick, picked);
		}
		public boolean isGoal(){
			return (toPick.size()==0 && picked.size()==0);

		}

	}
	
	public class Node{
		State state;
		List<Neighbor> neighbors;
		public Node(State state){
			this.state = state;
		}
		public boolean isGoal(){
			return state.isGoal();
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
	}
	
	
	
	public List<Neighbor> computeNeighbors(State state, ArrayList<Action> prevActions, City initialCity){
		List<Neighbor> neighbors =  new ArrayList<Neighbor>();
		if(state.isGoal()){
			return neighbors;
		}
		//delivery
		for(Task task: state.picked){
			if(task.deliveryCity == state.currentCity){
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

	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks){
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
