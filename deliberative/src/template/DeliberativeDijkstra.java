package template;

import java.util.List;
import java.io.File;
import java.io.FileWriter;
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
public class DeliberativeDijkstra implements DeliberativeBehavior {

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
			List<Task> updatedPicked = new ArrayList<Task>(picked);
			updatedPicked.remove(t);
			return new State(currentCity, toPick, updatedPicked);
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
					&& toPick.size() == thatState.toPick.size()
					&& picked.size() == thatState.picked.size()
					&& toPick.containsAll(thatState.toPick)
					&& picked.containsAll(thatState.picked);
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
			if(this.cost < 0){
				throw new RuntimeException();
			}
		}
		@Override
		public String toString() {
			return String.format("Neighbor {node: %s, plan: %s, cost %f}", node, plan, cost);
		}
	}
	
	
	
	public List<Neighbor> computeNeighbors(State state, ArrayList<Action> prevActions, City initialCity, double cost){
		List<Neighbor> neighbors =  new ArrayList<Neighbor>();
		
		if(state.isGoal()){
			//System.out.println("no neighbors");
			return neighbors;
		}
		//delivery
		for(Task task: state.picked){
			if(task.deliveryCity == state.currentCity){
				State newState = state.removePickedTask(task);
				List<Action> actions = new ArrayList<Action>(prevActions);
				Action action =  new Delivery(task);
				actions.add(action);
				logger.write(action);

				Node newNode = new Node(newState);
				int c = 0;
				neighbors.add(new Neighbor(newNode, new Plan(initialCity, actions), c));
			}
		}
			
		//pickup
		for(Task task: state.toPick){
			if(state.currentCity == task.pickupCity){
				State newState = state.pickTask(task);
				List<Action> actions = new ArrayList<Action>(prevActions);
				Action action = new Pickup(task);
				actions.add(action);
				logger.write(action);
				Node newNode = new Node(newState);
				int c = 0;
				neighbors.add(new Neighbor(newNode, new Plan(initialCity, actions), c));
			}
			
		}
		//move
		for(City city: state.currentCity.neighbors()){
			State newState = state.moveTo(city);
			Node newNode = new Node(newState);
			double c = state.currentCity.distanceTo(city);
			List<Action> actions = new ArrayList<Action>(prevActions);
			Action action = new Move(city);
			actions.add(action);
			logger.write(action);
			neighbors.add(new Neighbor(newNode, new Plan(initialCity, actions), c ));
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
	Logger logger ;

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
		logger = new Logger();
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
	public  static class Logger{
		
		FileWriter writer;
		int writerCount;
		public Logger(){
			writerCount = 0;
			try {
				
				writer = new FileWriter("log.txt");
			} catch (Exception e) {
				//TODO: handle exception
			}
		}
		
		public  void logCounter(int counter){
			if(counter % 1000 == 0 || true ) {
				write("Counter: " +  counter);
			}
		}
		public  void logNode(Node n){
			write(n.toString());
		}

		public void write(String str){
			writerCount++;
			//writeToFile(str);
			//writeToConsole(str);
		}

		public void writeQueue(LinkedList<Neighbor> queue, Node node){
			StringBuilder b = new StringBuilder(String.format("Queue (current node %s): \n", node.state.currentCity));
			for(Neighbor n: queue){
				Action a = null;
				for(Action aprime: n.plan){
					a = aprime;
				}
				//b.append(String.format("   city: %s, cost: %f, lastAction: %s\n", n.node.state.currentCity, n.cost, a));
			}
			write(b.toString());
		}
		public void write(Object o){
			writerCount++;
			//writeToFile(o.toString());
			//writeToConsole(o.toString());
		}
		public void writeToFile(String str){
			try {
				
				writer.write(str + "\n");
					writer.flush();
				
			} catch (Exception e) {
				//TODO: handle exception
			}
		}
		public void writeToConsole(String str){
			System.out.println(str);
		}
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
			logger.logCounter(counter);
			counter++;
			System.out.println(counter);
			Neighbor neighbor = Q.pop();
			Node node = neighbor.node;
			double cost = neighbor.cost;
			if(node.isGoal()){
				//System.out.println("Goal reached");
				logger.write("Goal reached");
				logger.write(neighbor.plan);
				return neighbor.plan;
			}

			//System.out.println("Current City" + neighbor.node.state.currentCity);
			if(!C.contains(node)){
				for(Neighbor n: Q){
					n.decrease(neighbor.cost);
				}
				logger.writeQueue(Q, node);

				C.add(node);//put in list of visited nodes
				logger.logNode(node);
				ArrayList<Action> prevActions = new ArrayList<Action>();
				for(Action action : neighbor.plan){
					prevActions.add(action);
				}
				List<Neighbor> neighbors = computeNeighbors(node.state, prevActions, initNode.state.currentCity, cost);

				for(Neighbor n : neighbors){
					Q.add(n);
				}

				if(counter == 1000){
					for(Neighbor n : Q){
						System.out.println(n.cost);
					}
				}

				Collections.sort(Q, new NeighborComparator());
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
