package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

import java.util.LinkedList;

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
	protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
		this.transferPriority = transferPriority;
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

		if (holder != null) {
			getThreadState(holder).release(this);	//el poseedor del lock va a liberar
		}
		
		ThreadState ts = this.pickNextThread(); //elegir threadstate
		if (ts == null) return null;
		else return ts.thread;					//devolver thread del threadstate
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
		if (waitQueue.isEmpty()) return null;

		int max = 0;
		int weight;

		ThreadState nextTS = null;
		ThreadState tempTS;

		//recorrer waitQueue
		for (Iterator i = waitQueue.iterator(); i.hasNext(); ) {
			tempTS = (ThreadState)i.next();
			weight = tempTS.calcular(this.transferPriority);

			if (weight > max) {
				max = weight;
				nextTS = tempTS;
			}
		} //end for

		if (max == 0) {		//todas son iguales
			ThreadState relatedToTempTS;

			for (Iterator i = waitQueue.iterator(); i.hasNext(); ) {
				relatedToTempTS = (ThreadState)i.next();

				if (this.transferPriority) relatedToTempTS.setCounter(relatedToTempTS.getEffectivePriority());		//POR QUE
				else relatedToTempTS.setCounter(relatedToTempTS.getPriority());		//POR QUE
			} //end for

			max = 0;
			nextTS = null;

			for (Iterator i = waitQueue.iterator(); i.hasNext(); ) {
				tempTS = (ThreadState)i.next();

				weight = tempTS.calcular(this.transferPriority);

				if (weight > max) {
					max = weight;
					nextTS = tempTS;	//mayor prioridad hasta el momento
				}
			} //end for

			if (max == 0) return null;	//todos son el minimo
			else return nextTS;			//devolver el maximo
		} else {
			return nextTS;		//devolver el maximo
		}
	}

	//NUNCA SE USA
	public boolean decreaseCounter (KThread thread) {
		return getThreadState(thread).decreaseCounter();
	}

	//IMPLEMENTAR	
	public void print() {
		Lib.assertTrue(Machine.interrupt().disabled());
		// implement me (if you want)
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
	public LinkedList<ThreadState> waitQueue = new LinkedList<ThreadState>();
	public KThread holder = null;		//MOVER
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
		int effectivePriority = this.priority;

		//todas las colas de espera
		for (Iterator i = relatedQueue.iterator(); i.hasNext(); ) {
			PriorityQueue tempQ = (PriorityQueue)i.next();

			if (tempQ.holder == this.thread) {	//el thread actual tiene el lock de la cola
				//todos los que esperan el lock
				for (Iterator j = tempQ.waitQueue.iterator(); j.hasNext(); ) {
					ThreadState tempTS = (ThreadState)j.next();

					if (tempTS.getPriority() > effectivePriority) {
						effectivePriority = tempTS.getPriority();	//aumenta la prioridad
					}
				} //end for
			} //end if
		} //end for

		return effectivePriority;
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
		
		// implement me
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
		if (this.counter == -1) {		//POR QUE
			this.counter = this.getEffectivePriority();
		} else {
			this.counter = this.priority;
		}

		waitQueue.waitQueue.add(this);	//poner en espera este thread
		this.relatedQueue.add((PriorityQueue)waitQueue);	//cola de espera de algun lock
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
		//Lib.assertTrue(Machine.interrupt().disabled());
		//Lib.assertTrue(waitQueue.waitQueue.isEmpty());
		
		this.relatedQueue.add((PriorityQueue)waitQueue);	//cola de espera de algun lock

		if (waitQueue.transferPriority) {
			waitQueue.holder = this.thread;		//el thread actual tiene el lock
		}
	}

	public int calcular(boolean transferPriority) {
		if (this.counter == 0) return 0;
		else if (transferPriority) return this.counter + this.getEffectivePriority();
		else return this.counter + this.priority;

		//si el contador es distinto de cero, el peso sera el contador mas la prioridad
	}

	public void release (PriorityQueue waitQueue) {
		this.relatedQueue.remove(waitQueue);	//cola de espera de algun lock

		waitQueue.holder = null;
	}

	public void setCounter (int newCounter) {
		if (newCounter < 0) return;
		else this.counter = newCounter;
	}

	public int getCounter () {
		return this.counter;
	}

	public boolean decreaseCounter () {
		if (this.counter == 0) {
			return false;
		} else {
			this.counter -= 1;
			return (this.counter != 0);
		}
	}

	protected int counter = -1;
	protected LinkedList<PriorityQueue> relatedQueue = new LinkedList<PriorityQueue>();

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	}
}
