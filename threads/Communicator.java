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
		//System.out.println("LISTENED TO -> " + msg);

		mutex.release();

		return msg;
	}



	// --- COMMUNICATOR TEST ---
private static class TestListener implements Runnable {
	   
		private Communicator aLink;
	   
		public TestListener(Communicator aLink)
		{
				this.aLink=aLink;              
		}
	   
		public void run(){
				System.out.println("Message Received: " +aLink.listen());
		}      
	}
   
	private static class TestSpeaker implements Runnable {
	   
		private Communicator aLink;
		private int msg;
	   
		public TestSpeaker(Communicator aLink, int msg)
		{
				this.aLink=aLink;
				this.msg = msg;
		}
	   
		public void run(){      
				System.out.println("Speaker says: " + msg);
				aLink.speak(msg);
		}
	   
	}
   
	public static void selfTest() {
		System.out.println("Communicator Self Test:\n");
		Communicator comm = new Communicator();

		System.out.println("Case1: Speak then Listen");
		KThread tmp1 = new KThread(new TestSpeaker(comm, 10));
		tmp1.fork();
		KThread tmp2 = new KThread(new TestListener(comm));
		tmp2.fork();
		tmp1.join();
		tmp2.join();

		System.out.println("Case2: Listen then Speak");
		KThread tmp3 = new KThread(new TestListener(comm));
		KThread tmp4 = new KThread(new TestSpeaker(comm, 20));
		tmp3.fork();
		tmp4.fork();
		tmp3.join();
		tmp4.join();

		System.out.println("Case3: Queued Speakers and Listeners");
		KThread tmp5 = new KThread(new TestSpeaker(comm, 30));
		KThread tmp6 = new KThread(new TestSpeaker(comm, 40));
		KThread tmp9 = new KThread(new TestSpeaker(comm, 50));
		KThread tmp10 = new KThread(new TestSpeaker(comm, 60));
		KThread tmp7 = new KThread(new TestListener(comm));
		KThread tmp8 = new KThread(new TestListener(comm));
		KThread tmp11 = new KThread(new TestListener(comm));
		KThread tmp12 = new KThread(new TestListener(comm));
		tmp5.fork();
		tmp6.fork();
		tmp7.fork();
		tmp8.fork();
		tmp9.fork();
		tmp10.fork();
		tmp11.fork();
		tmp12.fork();
		tmp5.join();
		tmp6.join();
		tmp7.join();
		tmp8.join();
		tmp9.join();
		tmp10.join();
		tmp11.join();
		tmp12.join();
		System.out.println("");
	}
	// --- COMMUNICATOR TEST ---
}
