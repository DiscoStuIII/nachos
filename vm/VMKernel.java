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
	swappt = new Hashtable<istKey, Integer>();	
	freeSwap = new LinkedList<Integer>();
	swapCount = 0;
	swapmemory = new Hashtable<Integer, Integer>();
	
	fifocount = 1;
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

    //Get page of the inverted page table
    public TranslationEntry getEntry(int processId, int virtualpn, int pageSize){
	Lib.debug(dbgVM, "get entry with pid " + processId + " and vpn " + virtualpn);
	istKey key = new istKey(processId, virtualpn);
	int paddr;
	//if not in the inverted get it from swap	
	if(!invertedpt.containsKey(key)){
		paddr = getSwap(key, pageSize);
		putEntry(key, new TranslationEntry(virtualpn, paddr, true, false, false, false));
	}

	return invertedpt.get(key);
    } 	

    //Put page in the inverted page table
    public void putEntry(istKey key, TranslationEntry page) {
	//Lib.debug(dbgVM, "put entry with pid " + key.processID + " vpn " + key.virtualPN);
	invertedpt.put(key, page);
    }	 

    //Put page of memory in the swap file
    public void getMemory(istKey key, int pageSize){	
	//	Lib.debug(dbgVM, "get memory with pid " + key.processID + " vpn " + key.virtualPN);
	//When we need more pages for the swap we create it :3
	if (freeSwap.size() == 0) {
		freeSwap.add(swapCount);
		swapCount++;
	}
	int swappage = freeSwap.removeFirst(); 	
	//Get physical page

				/* CHECK THIS, OSO ?  */
	
	if(freePages() == 0) {
		//Lib.debug(dbgVM, "insufficient pages");
		//terminate();
		istKey auxkey = invertedpt.keys().nextElement();
	
	/* DONT GET THE KEY D:
		istKey auxkey = new istKey(key.processID, fifocount);
		fifocount++;
		if(fifocount == Machine.processor().getNumPhysPages()) {
			fifocount = 1;
		}*/
		System.out.println("aux key " + auxkey.processID + " " +auxkey.virtualPN);
		
		
		TranslationEntry taux =	invertedpt.get(auxkey);
		if(!invertedpt.containsKey(auxkey)) {
			System.out.println("what");
		}
		returnPage(taux.ppn);
		invertedpt.remove(auxkey);
	}

					//
	int paddr = getPage();
	System.out.println(paddr);
	swappt.put(key,swappage);	
	swapmemory.put(swappage, paddr);
	byte[] memory = Machine.processor().getMemory();
	byte[] toWrite = new byte[pageSize];
	System.arraycopy(memory, paddr, toWrite, 0, pageSize);	
	swapFile.seek(swappage*pageSize);
	int write = swapFile.write(toWrite, 0 ,pageSize);
	
	if(write != pageSize) {
		Lib.debug(dbgVM, "Fatal error, couldn't write bytes to swap.");
		terminate();
	}
    }		


    //Get physical addres of a page in swap
    public int getSwap(istKey key,int pageSize){
	//Lib.debug(dbgVM, "get swap with pid " + key.processID + " vpn " + key.virtualPN);
	//if is not in swap, get it from memory	
	if(!swappt.containsKey(key)) {
		getMemory(key, pageSize);
	}
	
	int keymemory = swappt.get(key); 	
	return swapmemory.get(keymemory);
    }


    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    //Added
    //Inverted Page Table, here like the linked list of the physical pages(phase 2) to be global for the process	
    protected  Hashtable<istKey, TranslationEntry> invertedpt;
    //Swap Physical table spt	
    protected  Hashtable<istKey, Integer> swappt;
    protected  Hashtable<Integer, Integer> swapmemory; //Directions of memory in swap
    protected  OpenFile swapFile;	
    protected  LinkedList<Integer> freeSwap;		
    protected int swapCount;	

    //FIFO algorithm
    protected int fifocount;	

}
