/* test
 * make for child test, exec and join
 */

#include "syscall.h"
#define FNAME "helloworld2.txt"

int
main(int argc, char *argv[]) {
	
    	int filedesc; 
    	char buf[128], buf2[128]; 
	buf[0] = 'a';
	buf[1] = 'b';
	buf[2] = 'c';

	filedesc = creat(FNAME);
	filedesc = open(FNAME);
	printf("%d", argc);
	printf("%s", argv[0]);
	write(filedesc, *argv, argc);
	close(filedesc);
	filedesc = open(FNAME);	
	read(filedesc, buf2, 3);



	exit(1);

    	halt();
    	/* not reached */
}
