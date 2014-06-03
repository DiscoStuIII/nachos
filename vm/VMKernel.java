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
	invertedpt = new Hashtable<istKey, TranslationEntry>();	
	swappt = new Hashtable<istKey, Integer>();
	swapFile = Machine.stubFileSystem().open("swapFile", true); 	
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
    //key for the inverted page table and swap physical table, need process ID and virtual page number	
    public class istKey {
	public int processID;
	public int virtualPN;

		public istKey(int pid, int vpn) {
			processID = pid;
			virtualPN = vpn;
		}
    }	

    //Get page of the inverted page table
    public TranslationEntry getEntry(int processId, int virtualpn){
	istKey key = new istKey(processId, virtualpn);
	return invertedpt.get(key);
    } 	

    //Put page in the swap file
    //Used only when the invertedpt needs space and the page is dirty
    public void putSwap(processId, virtualpn, physicpn, TranslationEntry){
	

    }		


    //Get page of the swap file
    public TranslationEntry getSwap(){
	
    }


    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    //Added
    //Inverted Page Table, here like the linked list of the physical pages(phase 2) to be global for the process	
    protected static Hashtable<istKey, TranslationEntry> invertedpt;
    //Swap Physical table spt	
    protected static swappt Hashtable<istKey, Integer> swappt;
    protected OpenFile swapFile;	
	
}
