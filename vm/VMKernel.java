package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.Hashtable;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
	super();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
	super.initialize(args);
    }

    /**
     * Test this kernel.
     */	
    public void selfTest() {
	super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
	super.run();
    }
    
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    //Added
    //key for the inverted page table = need process ID and virtual page number	
    public class iptKey {
	public int processID;
	public int virtualPN;

		public iptKey(int pid, int vpn) {
			processID = pid;
			virtualPN = vpn;
		}
    }	

    //Get page of the inverted page table
    public TranslationENtry getEntry(int processId, int virtualpn){
	iptKey key = new iptKey(processId, virtualpn);
	return invertedpt.get(key);
    } 	




    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    //Added
    //Inverted Page Table, here like the linked list of the physical pages(phase 2) to be global for the process	
    protected static Hashtable<iptKey, TranslationEntry> invertedpt = new Hashtable<iptKey, TranslationEntry>();	
}
