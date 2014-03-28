package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 *
	 * @param	conditionLock	the lock associated with this condition
	 *				variable. The current thread must hold this
	 *				lock whenever it uses <tt>sleep()</tt>,
	 *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		waitingQueue = ThreadedKernel.scheduler.newThreadQueue(false);
		//sleeping = 0;
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The
	 * current thread must hold the associated lock. The thread will
	 * automatically reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();

		conditionLock.release();        
		
		waitingQueue.waitForAccess(KThread.currentThread());
		//sleeping++;
		KThread.sleep();
		
		conditionLock.acquire();

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		boolean intStatus = Machine.interrupt().disable();

		KThread thread = waitingQueue.nextThread();
		if (thread != null) {
			//sleeping--;
			thread.ready();
		}

		Machine.interrupt().restore(intStatus);
		//Machine.interrupt().enable();
}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = waitingQueue.nextThread();
		while (thread != null) {
			thread.ready();

			thread = waitingQueue.nextThread();
		}

		Machine.interrupt().restore(intStatus);
		//Machine.interrupt().enable();
		

		//while (sleeping > 0) {
		//	wake();
		//}
	}

	private Lock conditionLock;
	private ThreadQueue waitingQueue;
	//private int sleeping;



	// --- CONDITION2 TEST ---
	private static class Test implements Runnable {
	   
		Lock lock;
		Condition2 cv;
		int thread_num;
   
		Test(Lock lock, Condition2 cv, int i) {
			this.lock = lock;
			this.cv= cv;
			thread_num = i;
		}
	   
		public void run() {
			for (int i=1; i<=5; i++) {
				System.out.println("*** thread " + thread_num + " looped "
								   + i + " times");
			}
			System.out.println("Thread " + thread_num + " wakeAll");
				lock.acquire();
				cv.wakeAll();
				lock.release();
		   
		}
	}
 
	private static class Test3 implements Runnable {
	   
		Lock lock;
		Condition2 cv;
		int thread_num;

		Test3(Lock lock, Condition2 cv, int i) {
			this.lock = lock;
			this.cv= cv;
			thread_num = i;
		}
	   
		public void run() {
			   
				KThread.yield();
			for (int i=1; i<=5; i++) {
				System.out.println("*** thread " + thread_num + " looped "
								   + i + " times");
			}
			System.out.println("Thread " + thread_num + " wakeAll");
				lock.acquire();
				cv.wake();
				lock.release();
		   
		}
	}
   
	private static class Test2 implements Runnable {
	   
		Lock lock;
		Condition2 cv;
		int thread_num;
   
		Test2(Lock lock, Condition2 cv, int i) {
			this.lock = lock;
			this.cv= cv;
			thread_num = i;
		}
	   
		public void run() {
			System.out.println("Thread " + thread_num + " sleeping.");
				lock.acquire();
				cv.sleep();
				lock.release();
				System.out.println("Thread " + thread_num + " awake.");
		}      
	}
   
	public static void selfTest() {
	   
		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);
	   
		System.out.println("Condition2 Self Test:\n" +
						"\nTest single thread sleep and wake");
		new KThread(new Test3(lock, cv, 1)).fork();
		System.out.println("Thread 0 sleeping");
		lock.acquire();
		cv.sleep();
		lock.release();
		System.out.println("Thread 0 awake");
	   
		System.out.println("\nTest WakeAll when no sleeping threads");
		lock.acquire();
		cv.wakeAll();
		lock.release();
	   
		System.out.println("\nTest Wake when no sleeping threads");
		lock.acquire();
		cv.wake();
		lock.release();
	   
		System.out.println("\nTest multiple sleeps waiting on wakeAll");
		KThread tmp1 = new KThread(new Test2(lock, cv, 1));
		KThread tmp2 = new KThread(new Test2(lock, cv, 2));
		KThread tmp3 = new KThread(new Test2(lock, cv, 3));
		KThread tmp4 = new KThread(new Test(lock, cv, 4));
		tmp1.fork();
		tmp2.fork();
		tmp3.fork();
		tmp4.fork();
		System.out.println("Thread 0 sleeping");
		lock.acquire();
		cv.sleep();
		lock.release();
		System.out.println("Thread 0 awake");
		tmp1.join();
		tmp2.join();
		tmp3.join();
		tmp4.join();
	   
		System.out.println("");
			   
	}
	// --- CONDITION2 TEST ---
}
