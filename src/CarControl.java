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
	Semaphore inUse;
	int up,down,useUp, waiting;
	Pos alleyUpEntrance1, alleyUpEntrance2, alleyUpExit, alleyDownEntrance, alleyDownExit1, alleyDownExit2;
	Pos curpos;
	Car[] cars;
	public Alley(Car[] c){
		inUse = new Semaphore(1);
		cars = c;
		waiting=0;
		up = down = 4;
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
		if((    enterAlley1(cars[no]) && (no==1||no==2) || 
				enterAlley2(cars[no]) && (no==3||no==4))){
			if(up == 4 ) {
				waiting++;
				inUse.P();
			}
			up--;
		}
		else if(enterAlley3(cars[no])) {

			if(down == 4 && no > 4){
				inUse.P();
			}
			down--;
		}	
	}

	public void leave(int no) throws InterruptedException {
		if(leaveAlley2(cars[no])) {
			if(no>4 && down < 4)
				down++;

			if(down == 4){
				if(waiting>1){
					inUse.V();
				}
				inUse.V();
				waiting=0;
			}
		}else if(leaveAlley3(cars[no])){
			if(no<=4)
				up++;
			if(up == 4){
				inUse.V();
			}
		}
	}
}

class AlleyMonitor extends Alley{
	int up,down;
	Car[] cars;
	public AlleyMonitor(Car[] c){
		super(c);
		cars=c;
		up = down = 0;
	}
	@Override
	synchronized public void enter(int no) throws InterruptedException {
		Car c = cars[no];
		if((    enterAlley1(c) && (no==1||no==2) || 
				enterAlley2(c) && (no==3||no==4))){
			if(down > 0 ) 
				wait();
			up++;
			c.inAlley=true;
		}
		else if(enterAlley3(c)) {
			if(up > 0 && no > 4)
				wait();
			down++;
			c.inAlley=true;
		}	
	}

	
	@Override
	synchronized public void leave(int no) throws InterruptedException {
		Car c = cars[no];
		if(leaveAlley2(c)) {
			if(no>4 && down > 0){
				down--;
				c.inAlley=false;
			}
			if(down == 0)
				notifyAll();
			
		}else if(leaveAlley3(c)){
			if(no<=4){
				up--;
				c.inAlley =false;
			}
			if(up == 0)
				notifyAll();
		}
	}
	synchronized public void notifyThem() {
		notifyAll();
	}

}

class Barrier {
	boolean barrier;
	Semaphore queue;
	Car[] cars;
	int atBarrier;
	int active;
	public Barrier(Car[] c){
		atBarrier = 0;
		barrier = false;
		cars=c;
		queue=new Semaphore(0);
	}
	public void sync(int no) throws InterruptedException {
		active=8;
		if (cars[0].mygate.isopen||cars[0].curpos.equals(cars[0].barpos))
			active++;

		Car c=cars[no];
		if(barrier){
			if(c.curpos.equals(c.barpos)){
				atBarrier++;
				if(atBarrier==active)
					for(int i=0;i<active;i++){
						atBarrier=0;
						queue.V();
						}
				queue.P();
			}
		}
	}  // Wait for others to arrive (if barrier active)
	
	
	public void on() throws InterruptedException { 
		barrier=true;
	}    // Activate barrier

	public void off() {
		if(barrier){
			barrier=false;
			for(int i=0;i<atBarrier;i++)
				queue.V();
				
			
			atBarrier=0;
		}
	}   // Deactivate barrier 
		
	}

class BarrierMonitor extends Barrier {
	boolean barrier;
	Car[] cars;
	int atBarrier;
	int active;
	
	public BarrierMonitor(Car[] c) {
		super(c);
		atBarrier = 0;
		barrier = false;
		cars=c;
	}
	  @Override
	  synchronized public void sync(int no) throws InterruptedException {
		active=8;
		if (cars[0].mygate.isopen||cars[0].curpos.equals(cars[0].barpos))
			active++;

		Car c=cars[no];
		if(barrier){
			if(c.curpos.equals(c.barpos)){
				atBarrier++;
				if(atBarrier>=active){
					for(int i=0;i<=active;i++){
						atBarrier=0;
		                notifyAll();
		                
						}
				}else
					wait();
			}
		}
	}  
	  @Override
	  synchronized public void on(){ 
		barrier=true;
	}    // Activate barrier

	@Override
	synchronized public void off() {
		if(barrier){
			barrier=false;
			for(int i=0;i<atBarrier;i++)
				notifyAll();
			atBarrier=0;
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
	int active=0;
	AlleyMonitor alley;
	BarrierMonitor barrier;
	boolean enabled;
	boolean inAlley;
	public Car(int no, CarDisplayI cd, Gate g, Semaphore[][] t, AlleyMonitor a, BarrierMonitor b) {
		this.no = no;
		this.cd = cd;
		mygate = g;
		alley = a;
		barrier = b;
		startpos = cd.getStartPos(no);
		barpos = cd.getBarrierPos(no); // For later use
		track = t;
		col = chooseColor();
		enabled = true;
		inAlley = false;
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
	
	public synchronized void disable(int flag) throws InterruptedException{
		if(flag==1){
			cd.clear(curpos);
			track[curpos.col][curpos.row].V();
		}else if(flag==2){
			cd.clear(curpos,newpos);
			track[newpos.col][newpos.row].V();
			track[curpos.col][curpos.row].V();
		}else{
			cd.clear(curpos);
			track[curpos.col][curpos.row].V();
		}
		
	
		if(inAlley){
			if(no<=4) {
				alley.up++; 
				if(alley.up==4)
					alley.notifyThem();
			}
			else {
				alley.down++;
				if(alley.down==4)
					alley.notifyThem();
			}
		}

		wait();
				
	}

	public synchronized void enable() throws InterruptedException{
	
		speed = chooseSpeed();
		curpos = startpos;
		cd.mark(curpos, col, no);
		newpos = nextPos(curpos);
		track[curpos.col][curpos.row].P();
		notifyAll();
		


	}
	public void run() {
		try {
			
			Pos prevPos;
			speed = chooseSpeed();
			curpos = startpos;
			cd.mark(curpos, col, no);
			int flag=0;
			while (true) {
				try {
						sleep(speed());
						flag=1;
						if (atGate(curpos)) {
							mygate.pass();
							speed = chooseSpeed();
						}
						barrier.sync(no);
						alley.leave(no);
						newpos = nextPos(curpos);
						alley.enter(no);
						track[newpos.col][newpos.row].P();
						
						flag=2;
						// Move to new position
						cd.clear(curpos);
						cd.mark(curpos, newpos, col, no);
						sleep(speed());
						flag=0;
						cd.clear(curpos, newpos);
						cd.mark(newpos, col, no);
						prevPos = curpos;
						curpos = newpos;
						track[prevPos.col][prevPos.row].V();
				}catch (InterruptedException e) {
					try {
						disable(flag);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}catch (Exception e) {
	
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
	AlleyMonitor alley;
	BarrierMonitor barrier;
	public CarControl(CarDisplayI cd) {
		this.cd = cd;
		car = new Car[9];
		gate = new Gate[9];
		alley = new AlleyMonitor(car);
		barrier= new BarrierMonitor(car);
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
			car[no] = new Car(no, cd, gate[no], track, alley, barrier);
			car[no].start();
		}
	}

	public void startCar(int no) {
		gate[no].open();
	}

	public void stopCar(int no) {
		gate[no].close();
	}

	public void barrierOn() {
		barrier.on();
	}

	public void barrierOff() {
		barrier.off();
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
		
	
		Car c = car[no];
		if(c.enabled){
			c.enabled = false;
			cd.println("Destroy Car");	
			c.interrupt();
		}
		
	}

	public void restoreCar(int no) {
		Car c = car[no];
		if(!c.enabled){
			c.enabled = true;
			cd.println("Restore Car");
			try {
				car[no].enable();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/* Speed settings for testing purposes */

	public void setSpeed(int no, int speed) {
		car[no].setSpeed(speed);
	}

	public void setVariation(int no, int var) {
		car[no].setVariation(var);
	}

}
