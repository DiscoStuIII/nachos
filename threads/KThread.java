package nachos.threads;

import nachos.machine.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
	/**
	 * Get the current thread.
	 *
	 * @return	the current thread.
	 */
	public static KThread currentThread() {
	//System.out.println("asserting...");
	Lib.assertTrue(currentThread != null);
	//System.out.println("asserted!");
	return currentThread;
	}
	
	/**
	 * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
	 * create an idle thread as well.
	 */
	public KThread() {
		if (currentThread != null) {
			tcb = new TCB();
		}	    
		else {
			readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
			readyQueue.acquire(this);	    

			currentThread = this;
			tcb = TCB.currentTCB();
			name = "main";
			restoreState();

			createIdleThread();
		}

		//PE1
		if (joinQueue == null)
			joinQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	}

	/**
	 * Allocate a new KThread.
	 *
	 * @param	target	the object whose <tt>run</tt> method is called.
	 */
	public KThread(Runnable target) {
		this();
		this.target = target;
	}

	/**
	 * Set the target of this thread.
	 *
	 * @param	target	the object whose <tt>run</tt> method is called.
	 * @return	this thread.
	 */
	public KThread setTarget(Runnable target) {
	Lib.assertTrue(status == statusNew);
	
	this.target = target;
	return this;
	}

	/**
	 * Set the name of this thread. This name is used for debugging purposes
	 * only.
	 *
	 * @param	name	the name to give to this thread.
	 * @return	this thread.
	 */
	public KThread setName(String name) {
	this.name = name;
	return this;
	}

	/**
	 * Get the name of this thread. This name is used for debugging purposes
	 * only.
	 *
	 * @return	the name given to this thread.
	 */     
	public String getName() {
	return name;
	}
	 /**
     * Get the id of this thread. This had to be put in for compare inside of
     * <tt>ThreadState</tt> to work on threads that have been queued at the
     * same time and with the same priority.
     */
    public int getId() {
        return id;
    }
	/**
	 * Get the full name of this thread. This includes its name along with its
	 * numerical ID. This name is used for debugging purposes only.
	 *
	 * @return	the full name given to this thread.
	 */
	public String toString() {
	return (name + " (#" + id + ")");
	}

	/**
	 * Deterministically and consistently compare this thread to another
	 * thread.
	 */
	public int compareTo(Object o) {
	KThread thread = (KThread) o;

	if (id < thread.id)
		return -1;
	else if (id > thread.id)
		return 1;
	else
		return 0;
	}

	/**
	 * Causes this thread to begin execution. The result is that two threads
	 * are running concurrently: the current thread (which returns from the
	 * call to the <tt>fork</tt> method) and the other thread (which executes
	 * its target's <tt>run</tt> method).
	 */
	public void fork() {
	Lib.assertTrue(status == statusNew);
	Lib.assertTrue(target != null);
	
	Lib.debug(dbgThread,
		  "Forking thread: " + toString() + " Runnable: " + target);

	boolean intStatus = Machine.interrupt().disable();

	tcb.start(new Runnable() {
		public void run() {
			runThread();
		}
		});

	ready();
	
	Machine.interrupt().restore(intStatus);
	}

	private void runThread() {
	begin();
	target.run();
	finish();
	}

	private void begin() {
	Lib.debug(dbgThread, "Beginning thread: " + toString());
	
	Lib.assertTrue(this == currentThread);

	restoreState();

	Machine.interrupt().enable();
	}

	/**
	 * Finish the current thread and schedule it to be destroyed when it is
	 * safe to do so. This method is automatically called when a thread's
	 * <tt>run</tt> method returns, but it may also be called directly.
	 *
	 * The current thread cannot be immediately destroyed because its stack and
	 * other execution state are still in use. Instead, this thread will be
	 * destroyed automatically by the next thread to run, when it is safe to
	 * delete this thread.
	 */
	public static void finish() {
		Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
		
		Machine.interrupt().disable();

		Machine.autoGrader().finishingCurrentThread();

		//System.out.println("asserting...");
		Lib.assertTrue(toBeDestroyed == null);
		//System.out.println("asserted!");
		toBeDestroyed = currentThread;

		currentThread.status = statusFinished;

		KThread thread = currentThread.joinQueue.nextThread();
		while (thread != null) {
			thread.ready();

			thread = currentThread.joinQueue.nextThread();
		}
		
		sleep();
	}

	/**
	 * Relinquish the CPU if any other thread is ready to run. If so, put the
	 * current thread on the ready queue, so that it will eventually be
	 * rescheuled.
	 *
	 * <p>
	 * Returns immediately if no other thread is ready to run. Otherwise
	 * returns when the current thread is chosen to run again by
	 * <tt>readyQueue.nextThread()</tt>.
	 *
	 * <p>
	 * Interrupts are disabled, so that the current thread can atomically add
	 * itself to the ready queue and switch to the next thread. On return,
	 * restores interrupts to the previous state, in case <tt>yield()</tt> was
	 * called with interrupts disabled.
	 */
	public static void yield() {
	Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
	
	Lib.assertTrue(currentThread.status == statusRunning);
	
	boolean intStatus = Machine.interrupt().disable();

	currentThread.ready();

	runNextThread();
	
	Machine.interrupt().restore(intStatus);
	}

	/**
	 * Relinquish the CPU, because the current thread has either finished or it
	 * is blocked. This thread must be the current thread.
	 *
	 * <p>
	 * If the current thread is blocked (on a synchronization primitive, i.e.
	 * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
	 * some thread will wake this thread up, putting it back on the ready queue
	 * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
	 * scheduled this thread to be destroyed by the next thread to run.
	 */
	public static void sleep() {
	Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());

	if (currentThread.status != statusFinished)
		currentThread.status = statusBlocked;

	runNextThread();
	}

	/**
	 * Moves this thread to the ready state and adds this to the scheduler's
	 * ready queue.
	 */
	public void ready() {
	Lib.debug(dbgThread, "Ready thread: " + toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(status != statusReady);
	
	status = statusReady;
	if (this != idleThread)
		readyQueue.waitForAccess(this);
	
	Machine.autoGrader().readyThread(this);
	}

	/**
	 * Waits for this thread to finish. If this thread is already finished,
	 * return immediately. This method must only be called once; the second
	 * call is not guaranteed to return. This thread must not be the current
	 * thread.
	 */
	//PE1
	public void join() {
		Lib.debug(dbgThread, "Joining to thread: " + toString());
		Lib.assertTrue(this != currentThread);

		if (status == statusFinished) {
			return;
		}

		if (this != currentThread) {
			boolean intStatus = Machine.interrupt().disable();
			joinQueue.waitForAccess(currentThread);
			currentThread.sleep();
			Machine.interrupt().restore(intStatus);
		}
	}

	/**
	 * Create the idle thread. Whenever there are no threads ready to be run,
	 * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
	 * idle thread must never block, and it will only be allowed to run when
	 * all other threads are blocked.
	 *
	 * <p>
	 * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
	 */
	private static void createIdleThread() {
	Lib.assertTrue(idleThread == null);
	
	idleThread = new KThread(new Runnable() {
		public void run() { while (true) yield(); }
	});
	idleThread.setName("idle");

	Machine.autoGrader().setIdleThread(idleThread);
	
	idleThread.fork();
	}
	
	/**
	 * Determine the next thread to run, then dispatch the CPU to the thread
	 * using <tt>run()</tt>.
	 */
	private static void runNextThread() {
	KThread nextThread = readyQueue.nextThread();
	if (nextThread == null)
		nextThread = idleThread;

	nextThread.run();
	}

	/**
	 * Dispatch the CPU to this thread. Save the state of the current thread,
	 * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
	 * load the state of the new thread. The new thread becomes the current
	 * thread.
	 *
	 * <p>
	 * If the new thread and the old thread are the same, this method must
	 * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
	 * <tt>restoreState()</tt>.
	 *
	 * <p>
	 * The state of the previously running thread must already have been
	 * changed from running to blocked or ready (depending on whether the
	 * thread is sleeping or yielding).
	 *
	 * @param	finishing	<tt>true</tt> if the current thread is
	 *				finished, and should be destroyed by the new
	 *				thread.
	 */
	private void run() {
	Lib.assertTrue(Machine.interrupt().disabled());

	Machine.yield();

	currentThread.saveState();

	Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
		  + " to: " + toString());

	currentThread = this;

	tcb.contextSwitch();

	currentThread.restoreState();
	}

	/**
	 * Prepare this thread to be run. Set <tt>status</tt> to
	 * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
	 */
	protected void restoreState() {
	Lib.debug(dbgThread, "Running thread: " + currentThread.toString());
	
	//System.out.println("\tasserting...");
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
	Lib.assertTrue(tcb == TCB.currentTCB());
	//System.out.println("\tasserted!	");

	Machine.autoGrader().runningThread(this);
	
	status = statusRunning;

	if (toBeDestroyed != null) {
		toBeDestroyed.tcb.destroy();
		toBeDestroyed.tcb = null;
		toBeDestroyed = null;
	}
	}

	/**
	 * Prepare this thread to give up the processor. Kernel threads do not
	 * need to do anything here.
	 */
	protected void saveState() {
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
	}

	private static class PingTest implements Runnable {
	PingTest(int which) {
		this.which = which;
	}
	
	public void run() {
		for (int i=0; i<5; i++) {
		System.out.println("*** thread " + which + " looped "
				   + i + " times");
		currentThread.yield();
		}
	}

	private int which;
	}

	/*
	//test join, clase externa
	public static void selfTest() {
		KThreadTest.runTest();
	}
	*/
	

	/*
	//test communicator, clase externa
	public static void selfTest() {
		CommunicatorTest.runTest();
	}
	*/

	// --- KTHREAD TEST ---
	public static void selfTest() {
	
		Lib.debug(dbgThread, "Enter KThread.selfTest");
	   

		System.out.println("KThread Self Test:\n");
		new KThread(new PingTest(1)).setName("forked thread").fork();
		new PingTest(0).run();
		System.out.println("");
	   
		KThread parent, parent1, parent2, child;
	   
		//Test 1: Join a  thread that has not started running
		System.out.println("Test 1: Join a thread that has not started running");
		child = new  KThread(new Child(2));
		parent = new KThread(new Parent(0, child, 1));
		parent.setName("Parent").fork();
		currentThread.yield(); //run parent
		child.setName("Child").fork();
		currentThread.yield(); //run child
		parent.join(); //wait for the test to finish
	   
		//Test 2: Join a thread that is running
		System.out.println("\nTest 2: Join a thread that is running");
		child = new  KThread(new Child(5));
		parent = new KThread(new Parent(3,child, 1));
		parent.setName("Parent").fork();
		child.setName("Child").fork();
		currentThread.yield(); //run both the child and the parent
		parent.join(); //wait for test to finish
	   
		//Test 3: Join a thread that has finished
		System.out.println("\nTest 3: Join a thread that has finished");
		child = new  KThread(new Child(0));
		parent = new KThread(new Parent(0,child,1));
		child.setName("Child").fork();
		currentThread.yield();
		parent.setName("Parent").fork();
		parent.join();
	   
		//Test 4: Have 3 threads  A, B, C, A join B, B join C
		System.out.println( "\nTest 4: 3 threads Parent 1, Parent 2, Child. C join P1, P1 join P2");
		child = new  KThread(new Child(5));
		parent1 = new KThread(new Parent(4,child,1));
		parent2 = new KThread(new Parent(3,parent1,2));
		parent2.setName("Parent2").fork();
		currentThread.yield();
		parent1.setName("Parent1").fork();
		currentThread.yield();
		child.setName("Child").fork();
		currentThread.yield();
		parent2.join();
	   
		System.out.println("");


	}

	private static class Parent implements Runnable {
		Parent( int count, KThread child, int id ) {
				this.count = count;
				this.child = child;
				this.id = id;
		}
		public void run() {
				System.out.println("Parent " + id + " started running");
				//let the program run for a while
				for( int i = 0; i < count; i++ ) {
						System.out.println("Parent " + id + " Running");
						//swap it out and let something else run
						currentThread.yield();
				}
				System.out.println("Parent " + id + " Calling join()");
				this.child.join();
				System.out.println("Parent " + id + " Returned from join()");
				System.out.println("Parent " + id + " finished running");
		}
		private KThread child;
		private int count;
		private int id;
	}
   
	private static class Child implements Runnable {
		Child( int count ) {
				this.count = count;
		}
		public void run() {
				System.out.println("Child Started Running");
				for( int i = 0; i < count; i++ ) {
						System.out.println("Child Running");
						//swap it out and let something else run
						currentThread.yield();
				}
				System.out.println("Child Finished Running");
		}
		int count;
	}
	// --- KTHREAD TEST ---

	/*
	//test communicator, tres listeners y speakers
	public static void selfTest() {
		System.out.println(">>> Enter Communicator.selfTest <<<");

		KThread parent = new KThread(new Runnable() {
			
			Integer val_trans0 = 123456789;
			Integer val_trans1 = 987654321;
			Integer val_trans2 = 222999666;
			
			Communicator c = new Communicator();
			Speaker s0 = new Speaker(val_trans0, c);
			Listener l0 = new Listener(c);

			KThread thread1 = new KThread(s0).setName(">>> Speaker Thread");
			KThread thread2 = new KThread(l0).setName(">>> Listener Thread");
			
			Speaker s1 = new Speaker(val_trans1, c);
			Listener l1 = new Listener(c);
			KThread thread3 = new KThread(s1).setName(">>> Speaker Thread");
			KThread thread4 = new KThread(l1).setName(">>> Listener Thread");
			
			Speaker s2 = new Speaker(val_trans2, c);
			Listener l2 = new Listener(c);
			KThread thread5 = new KThread(s2).setName(">>> Speaker Thread");
			KThread thread6 = new KThread(l2).setName(">>> Listener Thread");

			public void run() {
				thread1.fork();
				thread2.fork();
				thread1.join();
				thread2.join();
				
				thread3.fork();
				thread4.fork();
				thread4.join();
				thread3.join();
				
				thread6.fork();
				thread5.fork();
				thread6.join();
				thread5.join();
			}
		});
		System.out.println(">>> t1 about to fork");
		parent.fork();
		System.out.println(">>> t1 forked");
		parent.join();
		System.out.println(">>> t1 joined");

		System.out.println(">>> End Communicator.selfTest <<<");
	}// seftTest();
	*/
	/*
	public static void selfTest() {
		System.out.println(">>> Enter Communicator.selfTest <<<");

		KThread parent = new KThread(new Runnable() {

			Integer val_trans0 = 111;
			Integer val_trans1 = 222;
			Integer val_trans2 = 333;
			Integer val_trans3 = 444;
			Integer val_trans4 = 555;
			Integer val_trans5 = 666;

			Communicator c = new Communicator();

			Speaker s0 = new Speaker(val_trans0, c);
			Listener l0 = new Listener(c);

			Speaker s1 = new Speaker(val_trans1, c);
			Listener l1 = new Listener(c);

			Speaker s2 = new Speaker(val_trans2, c);
			Listener l2 = new Listener(c);

			Speaker s3 = new Speaker(val_trans3, c);
			Listener l3 = new Listener(c);

			Speaker s4 = new Speaker(val_trans4, c);
			Listener l4 = new Listener(c);

			Speaker s5 = new Speaker(val_trans5, c);
			Listener l5 = new Listener(c);

			KThread t01 = new KThread(s0).setName("Speaker_01 ");
			KThread t04 = new KThread(l0).setName("Listener_04");

			KThread t02 = new KThread(s1).setName("Speaker_02 ");
			KThread t05 = new KThread(l1).setName("Listener_05");
			
			KThread t03 = new KThread(s2).setName("Speaker_03 ");
			KThread t06 = new KThread(l2).setName("Listener_06");
			
			KThread t10 = new KThread(s3).setName("Speaker_10 ");
			KThread t07 = new KThread(l3).setName("Listener_07");
			
			KThread t11 = new KThread(s4).setName("Speaker_11 ");
			KThread t08 = new KThread(l4).setName("Listener_08");
	
			KThread t12 = new KThread(s5).setName("Speaker_12 ");
			KThread t09 = new KThread(l5).setName("Listener_09");

			public void run() {
				t01.fork();//Speaker	111
				t02.fork();//Speaker	222
				t03.fork();//Speaker	333
				t04.fork();//Listener	> 111
				t05.fork();//Listener	> 
				t06.fork();//Listener	0,
				t07.fork();//Listener	0,
				t08.fork();//Listener	0,
				t09.fork();//Listener	0,
				t10.fork();//Speaker	444
				t11.fork();//Speaker	555
				t12.fork();//Speaker	666
				
				t01.join();//Speaker	111
				t02.join();//Speaker	222
				t03.join();//Speaker	333
				t04.join();//Listener	
				t05.join();//Listener
				t06.join();//Listener
				t07.join();//Listener
				t08.join();//Listener
				t09.join();//Listener
				t10.join();//Speaker	444
				t11.join();//Speaker	555
				t12.join();//Speaker	666
			}
		});
		System.out.println(">>> parent about to fork");
		parent.fork();
		System.out.println(">>> parent forked");
		parent.join();
		System.out.println(">>> parent joined");

		System.out.println(">>> End Communicator.selfTest <<<");
	}// seftTest();
	*/

	/*
	//test communicator, un listener y speaker
	public static void selfTest() {
		System.out.println(">>> Enter Communicator.selfTest <<<");

		KThread parent = new KThread(new Runnable() {
		Communicator c = new Communicator();
		Speaker s = new Speaker(0xdeadbeef, c);
		Listener l = new Listener(c);

		KThread thread1 = new KThread(s).setName("Speaker Thread");
		KThread thread2 = new KThread(l).setName("Listener Thread");

		public void run() {
		thread1.fork();
		thread2.fork();

		thread1.join();
		thread2.join();

		System.out.println("Incorrect Message recieved?");
		Lib.assertTrue(0xdeadbeef == l.getMessage());
		}
		});
		System.out.println("t1 about to fork");
		parent.fork();
		System.out.println("t1 forked");
		parent.join();
		System.out.println("t1 joined");

		System.out.println(">>> End Communicator.selfTest <<<");
	}// seftTest();
	*/
	
	/*
	// --- COMMUNICATOR TEST ---
	private static class Listener implements Runnable {
		private int msg;
		private Communicator commu;
		private boolean hasRun;

		private Listener(Communicator commu) {
			this.commu = commu;
			this.hasRun = false;
		}

		public void run() {
			System.out.println("Listener Listening! <" + msg + ">");
			msg = commu.listen();
			System.out.println("Listener Return! <" + msg + ">");
			hasRun = true;
		}

		private int getMessage() {
			System.out.println("Listener has not finished running? <" + hasRun+ ">");
			Lib.assertTrue(hasRun);
			return msg;
		}
	}

	private static class Speaker implements Runnable {
		private int msg;
		private Communicator commu;

		private Speaker(int msg, Communicator commu) {
			this.msg = msg;
			this.commu = commu;
		}

		public void run() {
			System.out.println("Speaker Speaking! <" + msg + ">");
			commu.speak(msg);
			System.out.println("Speaker Return! <" + msg + ">");
		}
	}
	// --- COMMUNICATOR TEST ---
	*/

	private static final char dbgThread = 't';

	/**
	 * Additional state used by schedulers.
	 *
	 * @see	nachos.threads.PriorityScheduler.ThreadState
	 */
	public Object schedulingState = null;

	private static final int statusNew = 0;
	private static final int statusReady = 1;
	private static final int statusRunning = 2;
	private static final int statusBlocked = 3;
	private static final int statusFinished = 4;

	/**
	 * The status of this thread. A thread can either be new (not yet forked),
	 * ready (on the ready queue but not running), running, or blocked (not
	 * on the ready queue and not running).
	 */
	private int status = statusNew;
	private String name = "(unnamed thread)";
	private Runnable target;
	private TCB tcb;

	/**
	 * Unique identifer for this thread. Used to deterministically compare
	 * threads.
	 */
	private int id = numCreated++;
	/** Number of times the KThread constructor was called. */
	private static int numCreated = 0;

	private static ThreadQueue readyQueue = null;
	private static KThread currentThread = null;
	private static KThread toBeDestroyed = null;
	private static KThread idleThread = null;

	private ThreadQueue joinQueue = null;
}
