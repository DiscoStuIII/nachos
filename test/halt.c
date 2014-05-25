/* halt.c
 *	Simple program to test whether running a user program works.
 *	
 *	Just do a "syscall" that shuts down the OS.
 *
 * 	NOTE: for some reason, user programs with global data structures 
 *	sometimes haven't worked in the Nachos environment.  So be careful
 *	out there!  One option is to allocate data structures as 
 * 	automatics within a procedure, but if you do this, you have to
 *	be careful to allocate a big enough stack to hold the automatics!
 */

#include "syscall.h"
#define FNAME "helloworld.txt"

int
main() {
	
    	int filedes, nbytes; 
    	char buf[128], buf2[128]; 
	buf[0] = 'a';
	filedes = creat(FNAME);
	filedes = open(FNAME);
 	write(FNAME, buf, 1);
	read(FNAME, buf2, 1);
	filedes = close(FNAME);
	read(FNAME, buf2, 10); 
	//close(filedes); 
	//filedes = open(FNAME); 
	//nbytes = read(0, buf, 128); // no debe dar error y no debe hacer nada aun 
	//write(1, buf, nbytes); // ya que estos 2 metodos son de consola close(filedes); 
	//unlink();
    	halt();
    	/* not reached */
}
