package nachos.threads;

import nachos.machine.*;
//import nachos.threads.PriorityScheduler.PriorityQueue;
//import nachos.threads.PriorityScheduler.ThreadState;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
	}
	
	/**
	 * Allocate a new lottery thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer tickets from waiting threads
	 *					to the owning thread.
	 * @return	a new lottery thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new LotteryQueue(transferPriority);
	}

	public static void selfTest() {
		System.out.println("Lottery Scheduler Self Test:\n");
		Test5.run();
		System.out.println("");
	}

	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	protected class LotteryQueue
					extends ThreadQueue {

		LotteryQueue (boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		//indicar a que thread se esta esperando
		//se usa este metodo para que todas las comprobaciones esten juntas en un lugar
		private ThreadState setWaiting (ThreadState nuevo) {
			//indicar cero donaciones recibidas
			if ((holder != null) && transferPriority) {
				nuevo.effectivePriority = 0;
			}

			//cambiar el 'holder'
			//quien tiene el lock, y que los demas lo esperan a el
			holder = nuevo;

			//sumar boletos de quienes esperan a este thread
			//esta suma sera la prioridad que se usara
			if ((holder != null) && transferPriority) {
				int suma = 0;
				for (int i=0; i<threadQueue.size(); i++) {
					suma += threadQueue.get(i).priority;
				}
				holder.effectivePriority = suma;
			}

			return nuevo;
		}

		//otro thread esta esperando, se aumentan los boletos
		public void waitForAccess (KThread thread) {
			//System.out.println("waitForAccess ls");
			if (holder != null) holder.effectivePriority += getThreadState(thread).priority;

			if (threadQueue != null) threadQueue.add(getThreadState(thread));
		}

		//este thread adquirio el lock
		public void acquire (KThread thread) {
			//DEBUG
			//System.out.println("acquire");
			setWaiting((LotteryScheduler.ThreadState)getThreadState(thread));
			//setWaiting(getThreadState(thread));
		}

		//elegir nuevo thread en base a los boletos
		public KThread nextThread () {
			//System.out.println("Entered nextThread()");
			if (threadQueue.size() == 0) {
				//System.out.println("Returned null due to size zero " + holder.thread.toString());
				return null;
			} else {
				int boleto = (int)(Math.random() * holder.effectivePriority + 1);

				//resta hasta que sea cero
				//el thread con el que se llega a cero es el elegido
				for (int i=0; i<threadQueue.size(); i++) {
					boleto -= threadQueue.get(i).priority;
					if (boleto <= 0) {
						System.out.println("\t\tGot nextThread() " + threadQueue.get(i).thread.toString());
						return threadQueue.get(i).thread;
					}
				}

				return null;
			}
		}

		public void print() {}

		//esta permitida la transferencia
		public boolean transferPriority;
		//holder del lock, los demas lo esperan
		private ThreadState holder = null;
		//threads esperando
		private LinkedList<ThreadState> threadQueue = new LinkedList<ThreadState>();
	}

	protected class ThreadState
					extends PriorityScheduler.ThreadState {

		public ThreadState (KThread thread) {
			//thread
			//prioridad por defecto
			//arreglo de donaciones
			super(thread);

			effectivePriority = 0;
		}

		public int getPriority() {
			return priority;
		}

		public int getEffectivePriority() {
			return effectivePriority;
		}

		protected void setEffectivePriority (int nuevo) {
			effectivePriority = nuevo;
		}

		public void setPriority (int priority) {
			this.priority = priority;
		}

		//el thread se pone a la espera del recurso de waitingOnQueue
		public void waitForAccess (PriorityQueue waitQueue) {
			System.out.println("waitForAccess ts");
			waitingOnQueue = waitQueue;
		}

		//se adquirio el recurso
		public void acquire (PriorityQueue waitQueue) {
			waitingOnQueue = waitQueue;
		}
	}
}


/**
 * A more advanced priority inversion test.
 */
class Test5 {
    static boolean high_run = false;

    public static void run() {
        Lock l = new Lock();
        PriorityScheduler sched = (PriorityScheduler) ThreadedKernel.scheduler;

        System.out.println("Testing basic priority inversion:");

        KThread low = new KThread(new Low(l));
        KThread med1 = new KThread(new Med(1));
        KThread med2 = new KThread(new Med(2));
        KThread high = new KThread(new High(l));

        //System.out.println("Threads created");

        boolean intStatus = Machine.interrupt().disable();
        sched.setPriority(high, 4);
        sched.setPriority(med1, 3);
        sched.setPriority(med2, 3);
        sched.setPriority(low, 1);
        Machine.interrupt().restore(intStatus);

        //System.out.println("Priorities set");

        low.fork();
        //System.out.println("low.fork()");
        KThread.yield();
        //System.out.println("KThread.yield()");
        med1.fork();
        high.fork();
        med2.fork();
        KThread.yield();

        System.out.println("finished forking");

        /* Make sure its all finished before quitting */
        low.join();
        med2.join();
        med1.join();
        high.join();

        System.out.println("finished joining");
    }

    private static class High implements Runnable {
        private Lock lock;

        public High(Lock l) {
            lock = l;
        }

        public void run() {
            System.out.println("High priority thread sleeping");
            lock.acquire();
            Test3.high_run = true;
            System.out.println("High priority thread woken");
            lock.release();
        }
    }

    private static class Med implements Runnable {
        int num;
        public Med(int n) {
            num = n;
        }
        public void run() {
            for (int i = 1; i < 3; i++) {
            	System.out.println("Mid " + num + " about to yield");
                KThread.yield();
            }

            if (Test3.high_run)
                System.out.println("High thread finished before thread " + num + ".");
            else
                System.out.println("Error, medium priority thread " + num + " finished before high priority one!");
        }
    }

    private static class Low implements Runnable {
        private Lock lock;

        public Low(Lock l) {
            lock = l;
        }

        public void run() {
            System.out.println("Low priority thread running");
            lock.acquire();
            System.out.println("Low about to yield");
            KThread.yield();
            System.out.println("Low priority thread finishing");
            lock.release();
        }
    }
}