/* test
 * make for child test, exec and join
 */

#include "syscall.h"
#define FNAME "helloworld2.txt"

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
	
	exit(0);

    	halt();
    	/* not reached */
}
