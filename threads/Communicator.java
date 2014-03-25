package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	//locks and conditions
	private Lock mutex;
	private Condition2 speakerReady;
	private Condition2 listenerReady;
	//message
	private int message;
	//count
	private int speakers;
	private int listeners;

	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		mutex = new Lock();
		speakerReady = new Condition2(mutex);
		listenerReady = new Condition2(mutex);

		speakers = 0;
		listeners = 0;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 *
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 *
	 * @param	word	the integer to transfer.
	 */
	public void speak(int word) {
		mutex.acquire();
		
		//sleep waiting for condition
		while (listeners == 0)
			listenerReady.sleep();

		//consume the listener that will get this message
		listeners--;

		//tell that a message is ready
		speakers++;

		message = word;
		
		//wake a waiting listener
		speakerReady.wake();


		mutex.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return
	 * the <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return	the integer transferred.
	 */    
	public int listen() {
		mutex.acquire();

		listeners++;

		//wake a waiting speaker
		listenerReady.wake();

		//sleep waiting for condition
		while (speakers == 0)
			speakerReady.sleep();

		//consume the speaker that sent this message
		speakers--;

		int msg = message;
		//DEBUG
		System.out.println("LISTENED TO -> " + msg);

		mutex.release();

		return msg;
	}
}
