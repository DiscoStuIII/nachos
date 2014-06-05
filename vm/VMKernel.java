package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.Hashtable;
import java.util.LinkedList;

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
	swappt = new Hashtable<istKey, TranslationEntry>();	
	freeSwap = new LinkedList<Integer>();
	swapCount = 0;
	swapmemory = new Hashtable<Integer, Integer>();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
	super.initialize(args);
	swapFile = Machine.stubFileSystem().open("swapFile.txt", true); 
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
	swapFile.close();
	Machine.stubFileSystem().remove("swapFile.txt"); 
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
    //Put page in the inverted page table
    public void putEntry(int processId, int virtualpn, TranslationEntry t){
	istKey key = new istKey(processId, virtualpn);		
	invertedpt.put(key, t);
    } 	


    //Get page in the inverted page table
    public TranslationEntry getEntry(int processId, int virtualpn, int pageSize){
	istKey key = new istKey(processId, virtualpn);	
	System.out.println("process id " + processId + "virtualp " + virtualpn);	
	return invertedpt.get(key);
    } 	
	 
    //Get page in swap 
    public TranslationEntry getSwap(int processId, int virtualpn ,int pageSize){
	istKey key = new istKey(processId, virtualpn);
	System.out.println("process id " + processId + "virtualp " + virtualpn);		
	return swappt.get(key);
    }	


    //Load page to swap
    public void loadEntry(int processId, int virtualpn, int pageSize){	
			
			/* Replace algorithm  */
	//For now just replace kind of random
	if(freePages() == 0) {
		istKey auxkey = invertedpt.keys().nextElement();	
	
	//Return a page from the inverted because we need more pages
		TranslationEntry taux =	invertedpt.get(auxkey);		
		returnPage(taux.ppn);
		invertedpt.remove(auxkey);
	}
			/*End replacement algorithm*/	

	
	int phypage = getPage();
	
	//When we need more pages for the swap we create it :3
	if (freeSwap.size() == 0) {
		freeSwap.add(swapCount);
		swapCount++;
	}
	
	int swappage = freeSwap.removeFirst(); 	
	istKey key = new istKey(processId, virtualpn);	


	TranslationEntry newPage = new TranslationEntry(virtualpn, phypage, true, false, false, false);

	swappt.put(key,newPage);
	System.out.println("process id " + processId + "virtualp " + virtualpn);
	swapmemory.put(swappage, phypage);
	//Save in swap information of the memory
	byte[] memory = Machine.processor().getMemory();
	byte[] toWrite = new byte[pageSize];
	System.arraycopy(memory, phypage*pageSize, toWrite, 0, pageSize);	
	swapFile.seek(swappage*pageSize);
	int write = swapFile.write(toWrite, 0 ,pageSize);
	
	if(write != pageSize) {
		Lib.debug(dbgVM, "Fatal error, couldn't write bytes to swap.");
		terminate();
	}
    }		





    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    //Added
    //Inverted Page Table, here like the linked list of the physical pages(phase 2) to be global for the process	
    protected  Hashtable<istKey, TranslationEntry> invertedpt;
    //Swap Physical table spt	
    protected  Hashtable<istKey, TranslationEntry> swappt;
    //Directions of memory in swap	
    protected  Hashtable<Integer, Integer> swapmemory; 
    protected  OpenFile swapFile;
    //Pages for the swapFile	
    protected  LinkedList<Integer> freeSwap;		
    protected int swapCount;	
}
