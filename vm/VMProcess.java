package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
	super();

	vmk = (VMKernel)Kernel.kernel;
        TLBnumber = 0;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
	super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	//super.restoreState();
	
	//Invalidate all the TLB entries on contex switch;
	int numEntries = Machine.processor().getTLBSize();
	for(int i = 0; i < numEntries; i++) {
		TranslationEntry page = Machine.processor().readTLBEntry(i);
		page.valid = false;
		Machine.processor().writeTLBEntry(i, page);
	}
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    //@Override
    protected boolean loadSections() {		
	//return super.loadSections();	
	//nothing loaded(lazy loader), so invalid the pages(cuz UserProcess set it valid)
	
	int numPhysPages = Machine.processor().getNumPhysPages();
	for (int i = 0; i < numPhysPages; i++){
		pageTable[i].valid = false;
	}
	return true;
	
	
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	super.unloadSections();
    }    

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();
	
	switch (cause) {

	case Processor.exceptionTLBMiss: 
		Lib.debug(dbgVM, "TLB Miss");
		int virtualAddress = processor.readRegister(Processor.regBadVAddr);
			
		int virtualPage = virtualAddress/pageSize;
		
		//Check the page is in range
		
		if(virtualPage > pageTable.length || virtualPage < 0){
			Lib.debug(dbgVM, "Page out of range");
			handleExit(-1);
		}
		
		System.out.println("processId " + processId + " virtualPage " + virtualPage + " virtualAddress " + virtualAddress);
		//Get the page of the inverted page table
		TranslationEntry page; 
		page = vmk.getEntry(processId, virtualPage, pageSize);

		//if not in inverted load it from swap	
		if(page == null || page.valid == false) {
			Lib.debug(dbgVM, "Page fault");
			vmk.loadSwap(processId, virtualPage, pageSize);		
			page = vmk.getEntry(processId, virtualPage, pageSize);
		//if not in swap, load it	
			if(page == null){
			Lib.debug(dbgVM, "Load page");
			vmk.loadEntry(processId, virtualPage, pageSize);
		//Now loaded get it
			page = vmk.getEntry(processId, virtualPage, pageSize);
			}
		}
		System.out.println("physic page assigned " + page.ppn);
		//pageTable[virtualPage] = page;

		//Check if it is coff
		//Practically same as loadSections from userprocess
		for(int s=0;(s<coff.getNumSections());s++){
			CoffSection section = coff.getSection(s);
			for(int i = 0; i<section.getLength(); i++){
				if(section.getFirstVPN()+i == virtualPage){
					 Lib.debug(dbgProcess, "\tinitializing " + section.getName()
			      + " section (" + section.getLength() + " pages)");
					pageTable[virtualPage].readOnly = section.isReadOnly();
			section.loadPage(i, pageTable[virtualPage].ppn);
				}
			}
		}
		
		//DEBUG FOR NOW
		try{
		Thread.sleep(2000);
		}catch(Exception e){}
		//

		//write the page in the TLB process
		//the TLB write pages are in FIFO 
		if (TLBnumber == processor.getTLBSize()){
			TLBnumber = 0;
		}
		processor.writeTLBEntry(TLBnumber, page);
		TLBnumber++;
		break;	

	default:
	    	super.handleException(cause);
	    	break;
	}
    }
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';

    //Added
    //VMKernel reference to hava access to the inverted page table
    private VMKernel vmk;
    //to do a FIFO TLB and not just random	
    private int TLBnumber;			
}
