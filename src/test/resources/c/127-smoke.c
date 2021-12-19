#include <stdio.h>
#include <stdarg.h>

// example from: https://www.cprogramming.com/tutorial/c/lesson17.html

/* this function will take the number of values to average
   followed by all of the numbers to average */
double average ( int num, ... ) {
    va_list arguments;
    double sum = 0;

    /* Initializing arguments to store all values after num */
    va_start ( arguments, num );
    /* Sum all the inputs; we still rely on the function caller to tell us how
     * many there are */
    for ( int x = 0; x < num; x++ ) {
        sum += va_arg ( arguments, double );
    }
    va_end ( arguments );                  // Cleans up the list

    return sum / (double)num;
}

extern int main() {
    /* this computes the average of 13.2, 22.3 and 4.5 (3 indicates the number of values to average) */
    printf( "%.2f\n", average ( 3, 12.2, 22.3, 4.5 ) );
    /* here it computes the average of the 5 values 3.3, 2.2, 1.1, 5.5 and 3.3 */
    printf( "%.2f\n", average ( 5, 3.3, 2.2, 1.1, 5.5, 3.3 ) );

    return 0;
}
// 13.00
// 3.08
