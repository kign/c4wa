#include <stdio.h>
#define C4WA_MM_INCR
#include <stdlib.h>

extern int nth_prime(int n) {
    if (n < 1)
        printf("Error: n must be 1 or greater, received %d\n", n);
    if (n == 1)
        return 2;
    int * primes = malloc((n - 1) * sizeof(int));
    int k = 0;
    int iter = 0;
    for(int test = 3; k < n - 1; test += 2) {
        int j = 0;
        for (j = 0; j < k && primes[j] * primes[j] <= test && test % primes[j]; j ++);
        iter += j;
        if (j < k && test % primes[j] == 0) continue;
        primes[k] = test;
        k ++;
    }

    printf("n = %d: returning result after %d iterations\n", n, iter);
    return primes[n - 2];
}
