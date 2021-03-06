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
	swapmemory = new Hashtable<istKey, Integer>();

    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
	super.initialize(args);
	swapFile = Machine.stubFileSystem().open("swapFile.txt", true); 
	mutex = new Lock();
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

		public int hashCode(){
			return new String(processID + " " + virtualPN).hashCode();
		}
		public boolean equals(Object o){
			if(!(o instanceof istKey)){
				return false;
			}
			istKey key = (istKey)o;
			return (key.processID == processID) && (key.virtualPN == virtualPN);
		}
    }	
    // Replace algorithm 	
    public int algorithm(int pageSize){
	mutex.acquire();			
	if(freePages() == 0) {
		System.out.println("algorithm");
		Hashtable<istKey, Integer> haux = new Hashtable<istKey, Integer>();
		int size = invertedpt.size();	
		istKey[] aaux = invertedpt.keySet().toArray(new istKey[size]);
		for(int i = 0; i < size;i++) {
			haux.put(aaux[i], 10);
		}
		int count = 0;
		int num = 10;
		while(true){
			if(invertedpt.get(aaux[count]).used) {
				invertedpt.get(aaux[count]).used = false;
				haux.put(aaux[count], 10);
			} else {
				num = haux.get(aaux[count]);
				if(num == 1){
					break;
				} else {
					haux.put(aaux[count], num -1);
				}
			}
			count++;	
			if(count == size) {
				count = 0;
			}
		}


		istKey remkey = aaux[count];

	//Return a page from the inverted because we need more pages
		TranslationEntry taux =	invertedpt.get(remkey);		
	
	//if dirty the replace one, update swapfile		

		if(taux.dirty){
			byte[] memory = Machine.processor().getMemory();
			byte[] toWrite = new byte[pageSize];
			System.arraycopy(memory, taux.ppn*pageSize, toWrite, 0, pageSize);	
			int swapPos = swapmemory.get(remkey);
			swapFile.seek(swapPos*pageSize);
			int write = swapFile.write(toWrite, 0 ,pageSize);
			if(write != pageSize) {
				Lib.debug(dbgVM, "Fatal error, couldn't write bytes to swap.");
				terminate();
			}
		}

		returnPage(taux.ppn);
		invertedpt.remove(remkey);
	}	
	int ret = getPage();
  	mutex.release();
	return ret;
   }	

    //Put page in the inverted page table
    public void putEntry(int processId, int virtualpn, TranslationEntry t){	
	mutex.acquire();	
	istKey key = new istKey(processId, virtualpn);
	invertedpt.put(key, t);
	mutex.release();
    } 	


    //Get page in the inverted page table
    public TranslationEntry getEntry(int processId, int virtualpn, int pageSize){	
	mutex.acquire();
	istKey key = new istKey(processId, virtualpn);
	TranslationEntry ret = invertedpt.get(key);
	mutex.release();
	return ret;
    } 	
	 
    //Load page in swap to inverted
    public void loadSwap(int processId, int virtualpn ,int pageSize){	
	mutex.acquire();
	istKey key = new istKey(processId, virtualpn);	
	if(swapmemory.containsKey(key)){
		//Read page from swap
		int swapPos = swapmemory.get(key);
		byte[] buf = new byte[pageSize];
		swapFile.seek(swapPos*pageSize);
		int bytesRead = swapFile.read(buf, 0, pageSize);
		if (bytesRead < pageSize){
			terminate();
		}
		//Get new physic page
		mutex.release();
		int newphypage = algorithm(pageSize);
		mutex.acquire();
		TranslationEntry t = swappt.get(key);
		t.ppn = newphypage;
		//Save it in inverted
		invertedpt.put(key, t);
		//Update memory
		byte[] memory = Machine.processor().getMemory();
		System.arraycopy(buf, 0, memory, newphypage*pageSize, pageSize);

	}
	mutex.release();

    }	


    //Load page to swap and inverted
    public void loadEntry(int processId, int virtualpn, int pageSize){	
			
	int phypage = algorithm(pageSize);
	mutex.acquire();
	//When we need more pages for the swap we create it :3
	if (freeSwap.size() == 0) {
		freeSwap.add(swapCount);
		swapCount++;
	}
	
	int swappage = freeSwap.removeFirst(); 	

	TranslationEntry newPage = new TranslationEntry(virtualpn, phypage, true, false, false, false);

	istKey key = new istKey(processId, virtualpn);
	//Save in swap and inverted
	swappt.put(key,newPage);
	swapmemory.put(key, phypage);
	invertedpt.put(key, newPage);
	//Save in swap information of the memory
	byte[] memory = Machine.processor().getMemory();
	byte[] toWrite = new byte[pageSize];
	System.arraycopy(memory, phypage*pageSize, toWrite, 0, pageSize);	
	swapFile.seek(swappage*pageSize);
	int write = swapFile.write(toWrite, 0 ,pageSize);
	mutex.release();
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
    protected  Hashtable<istKey, Integer> swapmemory; 
    protected  OpenFile swapFile;
    //Pages for the swapFile	
    protected  LinkedList<Integer> freeSwap;		
    protected int swapCount;	
    public Lock mutex;
	
}
