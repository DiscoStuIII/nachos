package nachos.threads;

import nachos.machine.*;

//import java.util.TreeSet;
//import java.util.HashSet;
import java.util.Iterator;

//estructura ordenada por prioridades
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.TreeSet;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other	
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}
	
	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer priority from waiting threads
	 *					to the owning thread.
	 * @return	a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
			   
		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
			   
		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
			   
		Lib.assertTrue(priority >= priorityMinimum &&
			   priority <= priorityMaximum);
		
		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
				   
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
				   
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;    

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param	thread	the thread whose scheduling state to return.
	 * @return	the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue 
						extends ThreadQueue
						implements Comparator<KThread> {

		//estructura ordenada por prioridades
		//PENDIENTE: comparacion de tiempo si prioridades son iguales
		
		public int compare (KThread t1, KThread t2) {
			ThreadState ts1 = getThreadState(t1);
			ThreadState ts2 = getThreadState(t2);
			int res = 0;

			if (ts1.getEffectivePriority() < ts2.getEffectivePriority()) {
				res = -1;
			} else if (ts1.getEffectivePriority() > ts2.getEffectivePriority()) {
				res = 1;
			} else if (ts1.queue != null && ts2.queue == null) {
				res = 1;
			} else if (ts1.queue == null && ts2.queue != null) {
				res = -1;
			} else {
				res = t1.compareTo(t2);
			}

			return res;
		}


		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			this.queue = new ConcurrentSkipListSet<KThread>(this);
			//this.queue = new TreeSet<KThread>();
			this.resourceHolder = null;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// -implement me

			//obtiene elemento de maxima prioridad
			KThread thread = this.queue.pollLast();
			//esta permitidad la donacion
			//alguien tiene el recurso (procesador)
			if (this.transferPriority && resourceHolder != null) {
				//va a ceder lugar, restaurar
				getThreadState(this.resourceHolder).resetEffectivePriority();
			}
			//cola asociada
			if (thread != null) {
				getThreadState(thread).queue = null;
			}

			resourceHolder = thread;
			return thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {
			// -implement me

			//estructura ordenada
			KThread thread = this.queue.last();
			ThreadState state = getThreadState(thread);

			return state;
		}
		
		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// -implement me (if you want)
			
			//imprime los mismos datos que round robin
			for (Iterator i=queue.iterator(); i.hasNext(); ) {
					System.out.print((KThread) i.next() + " ");
			}
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		//estructura ordenada
		protected ConcurrentSkipListSet<KThread> queue;
		//protected TreeSet<KThread> queue;
		//recibio la donacion
		protected KThread resourceHolder;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see	nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			
			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// -implement me
			//requiere que se haya definido su valor
			return effectivePriority;
		}
		public void setEffectivePriority(ThreadState donator) {

			//si la prioridad es igual o mayor no hay donacion
			//PENDIENTE: manejar prioridades iguales
			if (this.effectivePriority < donator.priority) {
				//cambiar y notificar
				this.effectivePriority = donator.priority;
				this.effectivePriorityChanged = true;
				//reinsertar para que se ordene
				try {
					PriorityQueue q = (PriorityQueue)this.queue;
					q.queue.remove(this.thread);
					q.queue.add(this.thread);
				} catch (Exception e) {}
			}
		}
		

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			
			this.priority = priority;

			// -implement me

			//la prioridad efectiva no se ha cambiado
			//la prioridad efectiva es menor
			if (!this.effectivePriorityChanged || this.effectivePriority < priority)
				this.effectivePriority = priority;
			
			//reinsertar para que se ordene
			try {
				PriorityQueue q = (PriorityQueue)this.queue;
				q.queue.remove(this.thread);
				q.queue.add(this.thread);
			} catch (Exception e) {}
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// -implement me

			//System.out.println("WAIT FOR ACCESS");
			//waitQueue.print();

			//estan permitidas las donaciones
			//hay alguien que tiene los recursos
			if (waitQueue.transferPriority && waitQueue.resourceHolder != null) {
				ThreadState h = getThreadState(waitQueue.resourceHolder);
				//colocamos la prioridad efectiva segun quien esta cediendo en este momento
				//hay que asegurarse que al menos quien esta cediendo pueda volverse a ejecutar
				h.setEffectivePriority(this);
			}

			//la cola asociada es la de espera
			this.queue = waitQueue;
			//colocar en cola
			waitQueue.queue.add(this.thread);
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// -implement me

			//estan permitidas las donaciones
			//hay alguien que tiene los recursos
			if (waitQueue.transferPriority && waitQueue.resourceHolder != null) {
				//consiguio el recurso, ya no necesita la donacion
				getThreadState(waitQueue.resourceHolder).resetEffectivePriority();
			}

			//es el poseedor del recurso
			waitQueue.resourceHolder = this.thread;
		}

		//restaurar prioridad
		protected void resetEffectivePriority () {
			//cambiar y notificar
			effectivePriority = priority;
			effectivePriorityChanged = false;

			//reinsestar para que se ordene
			try {
				PriorityQueue q = (PriorityQueue)this.queue;
				q.queue.remove(this.thread);
				q.queue.add(this.thread);
			} catch (Exception e) {}
		}

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;

		protected int effectivePriority;
		protected boolean effectivePriorityChanged;

		protected ThreadQueue queue;
	}
}







