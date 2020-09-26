import java.awt.Color;

import javax.print.attribute.standard.DialogOwner;

import java.util.List;
import java.util.ArrayList;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	private int x;
	private int y;
	private static int IDNumber = 0;
	private int ID;
	private int energy = 20;
	private RabbitsGrassSimulationSpace rgSpace;

	
	private enum Direction{
		LEFT,
		RIGHT,
		UP,
		DOWN
	}

	public RabbitsGrassSimulationAgent(int x, int y, int initEnergey ){
		this.x = x;
		this.y = y;
		IDNumber++;
		ID = IDNumber;
		this.energy = initEnergey;
	}
	public void draw(SimGraphics graphics) {
		graphics.drawFastRoundRect(Color.gray);
	}
	
	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setX(int newX){
		x = newX;
	}

	public void setY(int newY){
		y = newY;
	}

	public int getEnergy(){
		return this.energy;
	}
	public void setRabbitGrassSpace(RabbitsGrassSimulationSpace space){
		this.rgSpace = space;
	}
	
	public void setXY(int x, int y){
		this.x = x;
		this.y = y;
	}
	public String getID(){
		return "A-" + ID;
	}

	public void payReproduction(int energyCost){
		this.energy -= energyCost;
	}

	public List<Direction> validDirections(){ 
		
		List<Direction> directions = new ArrayList<Direction>();

		if(canMoveTo(x - 1, y)){
			directions.add(Direction.LEFT);
		} 

		if(canMoveTo(x + 1, y)){
			directions.add(Direction.RIGHT);
		}

		if(canMoveTo(x, y + 1)){
			directions.add(Direction.DOWN);
		}

		if(canMoveTo(x, y - 1)){
			directions.add(Direction.UP);
		}
		return directions;
	}
	public void step(){
		energy--;
		move();
		energy += rgSpace.eatGrassAt(x, y);
		

	}

	private boolean canMoveTo(int x, int y){
		return x >= 0 && x <  rgSpace.getDimX() && y >= 0 && y < rgSpace.getDimY()  && !rgSpace.isCellOccupied(x,y);
	}

	public void move(){
		List<Direction> validDirections = validDirections();
		if(validDirections.size() > 0){
			int idx  = (int) (Math.random() * validDirections.size());
			Direction dir = validDirections.get(idx);
			switch (dir) {
				case LEFT:
					setXY(getX() - 1 , y);
					break;
				case RIGHT:
					setXY(getX() + 1, y);
					break;
				case UP:
					setXY(x, getY() - 1);
					break;
				case DOWN:
					setXY(x, getY() + 1);
					break;
				default:
					break;
			}
		}
	}

	public void report(){
	System.out.println(getID() +
						" at " +
						x + ", " + y);
						
	}
}
