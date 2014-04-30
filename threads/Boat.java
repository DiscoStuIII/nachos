package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
		    
    static Lock boatMutex; //Using booat
    static Lock inBoatMutex; //Using booat
    static Semaphore childSemaphore; 
    static int inBoat;
    static boolean finish; //True when all finish
    static int numOChildren;
    static int numMChildren;				
    static int numOAdult;   
    static int numMAdult;    	
    static int boatLocation; //0 = Oahu, 1 = Molokai
    static boolean synchro;
	

    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	numOChildren = children;
	numMChildren = 0;
	numOAdult = adults;
	numMAdult = 0;
	boatMutex = new Lock();
	inBoatMutex = new Lock();
	childSemaphore = new Semaphore(2);
	inBoat = 0;
  	boatLocation = 0;
        finish = false;
	synchro = false;
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	Runnable r1 = new Runnable() {
	    public void run() {
                ChildItinerary();
            }
        };
	Runnable r2 = new Runnable() {
	    public void run() {
                AdultItinerary();
            }
        };

	for(int i = 0; i<children; i++) {
		KThread t = new KThread(r1);
		t.fork();
        }

	for(int i = 0; i<adults; i++) {
		KThread t = new KThread(r2);
		t.fork();
        }
	if (children<2 && adults == 0) {
		finish = true; //Que todos terminen porque no hay suficientes 	
				//ni;os para hacer que funcione
	}
    }

    static void AdultItinerary() {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
	int inBoat = 0;

	boatMutex.acquire();
	while( (numOChildren > 1) || (boatLocation == 1) ) {		
		boatMutex.release();
		KThread.sleep();
		boatMutex.acquire();
	}	
	bg.AdultRowToOahu();
	numOAdult--;
	numMAdult++;
	boatLocation = 1;			
	boatMutex.release();
    }

    static void ChildItinerary(){
	int currentLocation = 0;	
	boolean waitingFor = false;
	while(!finish){
		boatMutex.acquire();
		while(currentLocation != boatLocation) { //wait for the boat
			boatMutex.release();			
			KThread.sleep();
			boatMutex.acquire();
		}

		if(currentLocation == 0) {

			if( numOChildren > 1) { 
				childSemaphore.P();
				while(inBoat != 2) {
					if(waitingFor) {//Espero que ingrese el otro
						boatMutex.release();	
						KThread.sleep();
						boatMutex.acquire();
					} else {
						waitingFor = true;
						inBoat++;
					}
				}				
				if(inBoat == 2) {
					bg.ChildRowToMolokai();
					inBoat--;
				} else {
					bg.ChildRideToMolokai();
					inBoat--;
					childSemaphore.V();
					childSemaphore.V();
					boatLocation = 1;
				}
				numOChildren--;
				numMChildren++;
				currentLocation = 1;	
				boatMutex.release();
				KThread.sleep();
			} else if (numOAdult == 0 && numOChildren == 1) {
				bg.ChildRowToMolokai();
				numOChildren--;
				numMChildren++;
				boatLocation = 1;
				finish = true;
				currentLocation = 1;
				boatMutex.release();
			} else {
				boatMutex.release();			
				KThread.sleep();

			}			
		} else {
			numOChildren++;
			numMChildren--;
			boatLocation = 0;
			currentLocation = 0;
			bg.ChildRowToOahu();
			boatMutex.release();			
			KThread.sleep();
		}
		
	}
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
