import java.awt.Color;

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
  
	public RabbitsGrassSimulationAgent(int x, int y){
		this.x = x;
		this.y = y;
		IDNumber++;
		ID = IDNumber;
	}
	public void draw(SimGraphics graphics) {
		// TODO Auto-generated method stub
		graphics.drawFastRoundRect(Color.blue);
	}

	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}

	public void setX(int newX){
		x = newX;
	}

	public void setY(int newY){
		y = newY;
	}
	
	public void setXY(int x, int y){
		this.x = x;
		this.y = y;
	}
	public String getID(){
		return "A-" + ID;
	}

	public void step(){
		setX(getX()+1);
	}



	public void report(){
	System.out.println(getID() +
						" at " +
						x + ", " + y);
						
	}
}
