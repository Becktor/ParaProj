//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2014

//Hans Henrik Løvengreen    Oct 6, 2014

import java.awt.Color;

class Gate {

	Semaphore g = new Semaphore(0);
	Semaphore e = new Semaphore(1);
	boolean isopen = false;

	public void pass() throws InterruptedException {
		g.P();
		g.V();
	}

	public void open() {
		try {
			e.P();
		} catch (InterruptedException e) {
		}
		if (!isopen) {
			g.V();
			isopen = true;
		}
		e.V();
	}

	public void close() {
		try {
			e.P();
		} catch (InterruptedException e) {
		}
		if (isopen) {
			try {
				g.P();
			} catch (InterruptedException e) {
			}
			isopen = false;
		}
		e.V();
	}

}

class Alley{
	Semaphore inUse1,inUse2;
	int up,down1,down2,useUp, useDown;
	Pos alleyUpEntrance1, alleyUpEntrance2, alleyUpExit, alleyDownEntrance, alleyDownExit1, alleyDownExit2;
	Pos curpos;
	Car[] cars;
	public Alley(Car[] c){
		inUse1 = new Semaphore(1);
		inUse2 = new Semaphore(1);
		cars = c;
		useDown=useUp=0;
		up = down1 = down2 = 4;
		alleyUpEntrance1= new Pos(8,1);
		alleyUpEntrance2= new Pos(9,3);
		alleyUpExit = new Pos (1,0);
		alleyDownEntrance= new Pos(0,0);
		alleyDownExit1 = new Pos (8,0);
		alleyDownExit2 = new Pos (9,2);
	}

	public boolean enterAlley1(Car c){
		if ((c.curpos.equals(alleyUpEntrance1) && (c.no==1||c.no==2)))
			return true;
		else
			return false;
	}
	public boolean enterAlley2(Car c){
		if ((c.curpos.equals(alleyUpEntrance2) && (c.no==3||c.no==4)))
			return true;
		else
			return false;
	}
	public boolean enterAlley3(Car c){
		if ((c.curpos.equals(alleyDownEntrance) && c.no>4))
			return true;
		else
			return false;
	}

	public boolean leaveAlley1(Car c){
		if ((c.curpos.equals(alleyDownExit1) && c.no>4))
			return true;

		else
			return false;
	}
	public boolean leaveAlley2(Car c){
		if (c.curpos.equals(alleyDownExit2) && c.no>4)
			return true;

		else
			return false;
	}
	public boolean leaveAlley3(Car c){
		if ((c.curpos.equals(alleyUpExit) && c.no<=4))
			return true;

		else
			return false;
	}

	public void enter(int no) throws InterruptedException {
		if(enterAlley1(cars[no])){

			if(up == 4  && (no==1||no==2) ) {
				inUse1.P();
			}
			up--;
		}else if(enterAlley2(cars[no])){

			if(up == 4  && (no==3 || no==4) ){
				inUse2.P();
			}
			up--;
		}else if(enterAlley3(cars[no])) {

			if(down1 == 4 && down2==4 && no > 4 ){
				inUse2.P();
				inUse1.P();
			}
			down1--;
			down2--;
		}
	}


	public void leave(int no) throws InterruptedException {
	if(leaveAlley1(cars[no])){
		if(no>4 && down1 < 4)
			down1++;
			if(up == 4 && down1 == 4){
				inUse1.V();
			}

	}else if(leaveAlley2(cars[no])) {
		if(no>4 && down2 < 4)
			down2++;
		if(up == 4 && down2 == 4){
			inUse2.V();
		}
	}else if(leaveAlley3(cars[no])){
		if(no<=4)
			up++;

		if(up == 4 && down1 == 4 && down2==4){
			inUse2.V();
			inUse1.V();
		}
		}
	}



}

class Car extends Thread {

	int basespeed = 100; // Rather: degree of slowness
	int variation = 50; // Percentage of base speed

	CarDisplayI cd; // GUI part

	int no; // Car number
	Pos startpos; // Startpositon (provided by GUI)
	Pos barpos; // Barrierpositon (provided by GUI)
	Color col; // Car color
	Gate mygate; // Gate at startposition

	int speed; // Current car speed
	Pos curpos; // Current position
	Pos newpos; // New position to go to
	Semaphore track[][]; // race track for cars
	Alley alley;

	public Car(int no, CarDisplayI cd, Gate g, Semaphore[][] t, Alley a) {
		this.no = no;
		this.cd = cd;
		mygate = g;
		alley = a;
		startpos = cd.getStartPos(no);
		barpos = cd.getBarrierPos(no); // For later use

		track = t;
		col = chooseColor();

		// do not change the special settings for car no. 0
		if (no == 0) {
			basespeed = 0;
			variation = 0;
			setPriority(Thread.MAX_PRIORITY);
		}
	}

	public synchronized void setSpeed(int speed) {
		if (no != 0 && speed >= 0) {
			basespeed = speed;
		} else
			cd.println("Illegal speed settings");
	}

	public synchronized void setVariation(int var) {
		if (no != 0 && 0 <= var && var <= 100) {
			variation = var;
		} else
			cd.println("Illegal variation settings");
	}

	synchronized int chooseSpeed() {
		double factor = (1.0D + (Math.random() - 0.5D) * 2 * variation / 100);
		return (int) Math.round(factor * basespeed);
	}

	private int speed() {
		// Slow down if requested
		final int slowfactor = 3;
		return speed * (cd.isSlow(curpos) ? slowfactor : 1);
	}

	Color chooseColor() {
		return Color.blue; // You can get any color, as longs as it's blue
	}

	Pos nextPos(Pos pos) {
		// Get my track from display
		return cd.nextPos(no, pos);
	}

	boolean atGate(Pos pos) {
		return pos.equals(startpos);
	}

	public static <PrintableToString> void println(PrintableToString... args) {
		for (PrintableToString pts : args)
			System.out.print(pts);
		System.out.println();
	}

	public static <PrintableToString> void print(PrintableToString... args) {
		for (PrintableToString pts : args)
			System.out.print(pts);
	}



	public void run() {
		try {
			Pos prevPos;
			speed = chooseSpeed();
			curpos = startpos;
			cd.mark(curpos, col, no);
			
			while (true) {
				sleep(speed());

				if (atGate(curpos)) {
					mygate.pass();
					speed = chooseSpeed();
				}
				alley.leave(no);
				newpos = nextPos(curpos);
				alley.enter(no);

				track[newpos.col][newpos.row].P();
				// Move to new position
				cd.clear(curpos);
				cd.mark(curpos, newpos, col, no);
				sleep(speed());
				cd.clear(curpos, newpos);
				cd.mark(newpos, col, no);
				prevPos = curpos;
				curpos = newpos;
				track[prevPos.col][prevPos.row].V();
				
			}

		} catch (Exception e) {
			cd.println("Exception in Car no. " + no);
			System.err.println("Exception in Car no. " + no + ":" + e);
			e.printStackTrace();
		}
	}

}

public class CarControl implements CarControlI {
	int i = 0;
	CarDisplayI cd; // Reference to GUI
	Car[] car; // Cars
	Gate[] gate; // Gates
	Semaphore[][] track; // Track
	Semaphore inUse; //Alley
	Alley alley;
	public CarControl(CarDisplayI cd) {
		this.cd = cd;
		car = new Car[9];
		gate = new Gate[9];
		alley = new Alley(car);
		track = new Semaphore[12][11];
		while (i < 12) {
			int  j = 0;
			while (j < 11) {
				track[i][j] = new Semaphore(1);
				j++;
			}
			i++;
		}
		for (int no = 0; no < 9; no++) {
			gate[no] = new Gate();
			car[no] = new Car(no, cd, gate[no], track, alley);
			car[no].start();
		}
		alley = new Alley(car);
	}

	public void startCar(int no) {
		gate[no].open();
	}

	public void stopCar(int no) {
		gate[no].close();
	}

	public void barrierOn() {
		cd.println("Barrier On not implemented in this version");
	}

	public void barrierOff() {
		cd.println("Barrier Off not implemented in this version");
	}

	public void barrierSet(int k) {
		cd.println("Barrier threshold setting not implemented in this version");
		// This sleep is for illustrating how blocking affects the GUI
		// Remove when feature is properly implemented.
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
	}

	public void removeCar(int no) {
		cd.println("Remove Car not implemented in this version");
	}

	public void restoreCar(int no) {
		cd.println("Restore Car not implemented in this version");
	}

	/* Speed settings for testing purposes */

	public void setSpeed(int no, int speed) {
		car[no].setSpeed(speed);
	}

	public void setVariation(int no, int var) {
		car[no].setVariation(var);
	}

}
