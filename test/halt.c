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
	
    	int filedesc; 
    	char buf[128], buf2[128]; 
	buf[0] = 'a';
	buf[1] = 'b';
	buf[2] = 'c';
	filedesc = creat(FNAME);
	filedesc = open(FNAME);
 	write(filedesc, buf, 3);
	read(filedesc, buf2, 3);
	close(filedesc);
	filedesc = open(FNAME);
	read(filedesc, buf2, 3);
	close(filedesc);
	read(filedesc, buf2, 1); 
	filedesc = open(FNAME);
	unlink(FNAME);
	read(filedesc, buf2, 1);
	close(filedesc);
	open(filedesc);
	read(filedesc, buf2, 1);

    	halt();
    	/* not reached */
}
