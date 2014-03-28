package nachos.threads;

import nachos.machine.*;

import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p><b>Note</b>: Nachos will not function correctly with more than one
	 * alarm.
	 */
	public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread
	 * that should be run.
	 */
	//PE2
	public void timerInterrupt() {
		//DEBUG
		//System.out.println("timerInterrupt");

		boolean intStatus = Machine.interrupt().disable();
		long currentTime = Machine.timer().getTime();

		ThreadTime waitingThread = waitQueue.peek();	//ver con peek
		while (waitingThread != null && currentTime >= waitingThread.time) {
			waitQueue.poll();	//remover con poll
			waitingThread.thread.ready();
			waitingThread = waitQueue.peek();
		}

		Machine.interrupt().restore(intStatus);

		KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks,
	 * waking it up in the timer interrupt handler. The thread must be
	 * woken up (placed in the scheduler ready set) during the first timer
	 * interrupt where
	 *
	 * <p><blockquote>
	 * (current time) >= (WaitUntil called time)+(x)
	 * </blockquote>
	 *
	 * @param	x	the minimum number of clock ticks to wait.
	 *
	 * @see	nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		//long wakeTime = Machine.timer().getTime() + x;
		//while (wakeTime > Machine.timer().getTime())
		//	KThread.yield();

		//DEBUG
		//System.out.println("waitUntil: " + x);

		if (x > 0) {
			long time = Machine.timer().getTime() + x;
			boolean intStatus = Machine.interrupt().disable();
			ThreadTime e = new ThreadTime(KThread.currentThread(), time);
			waitQueue.offer(e);	//guardar con offer
			KThread.sleep();	//elimina busy waiting
			Machine.interrupt().restore(intStatus);
		}
	}


	class ThreadTime
			implements Comparable<ThreadTime> {

		public KThread thread;
		public long time;		//despertar

		public ThreadTime (KThread kt, long t) {
			thread = kt;
			time = t;
		}

		//interfaz Comparable para poder usar PriorityQueue
		//compareTo
			//-1 cuando this sea menor
			//1 cuando this sea mayor
			//0 cuando sean iguales
		public int compareTo (ThreadTime other) {
			if (this.time < other.time) return -1;
			else if (this.time > other.time) return 1;
			else return 0;
		}
	}

	private PriorityQueue<ThreadTime> waitQueue = new PriorityQueue<ThreadTime>();

	// --- ALARM TEST ---
	private static class AlarmTest implements Runnable {
		AlarmTest(long waitTime){
				this.wait = waitTime;
		}
	   
		public void run(){
				System.out.print("Calling WaitUntil\t");
				System.out.println( "Wait time: " + wait);
				System.out.println("Current time " + Machine.timer().getTime() );
				ThreadedKernel.alarm.waitUntil(wait);
				System.out.print("Returned from waitUntil");
				System.out.println("Current time " + Machine.timer().getTime() );
		}
	   
		long wait;
	}
   
	public static void selfTest(){
		System.out.println("Alarms self test");
	   
		System.out.println("Test 1: Sleep time negative");
		KThread thread = new KThread( new AlarmTest(-10) );
		thread.setName("AlarmTest").fork();
		thread.join();
	   
		System.out.println("\nTest 2: Sleep time 0");
		thread = new KThread( new AlarmTest(0) );
		thread.setName("AlarmTest").fork();
		thread.join();
	   
		System.out.println("\nTest 3: Sleep time smaller than 500");
		thread = new KThread( new AlarmTest(200) );
		thread.setName("AlarmTest").fork();
		thread.join();
	   
		System.out.println("\nTest 4: Sleep time greater than 500");
		thread = new KThread( new AlarmTest(700) );
		thread.setName("AlarmTest").fork();
		thread.join();
	   
		System.out.println("");
	}
	// --- ALARM TEST ---
}
