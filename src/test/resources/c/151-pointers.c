void printf ();

const int N = 100;

extern int main () {
    int i;
    int * primes = alloc(0, 50, int);
    primes[0] = 2;
    // Out logical operations are bitwise, so both ares are evaluated
    // (When evaluating A && B, if A evaluates to 0, B is still evaluated)
    // so unless with put this extra 1, loop below will trigger "RuntimeError: remainder by zero"
    // This is a hack obviously, but it solves the problem.
    primes[1] = 1;
    int n = 1;
    for (int p = 3; p < N; p += 2) {
        for(i = 1; i < n && primes[i] * primes[i] < p && p % primes[i] != 0; i ++);
        if (i >= n || p % primes[i] != 0) {
            primes[n] = p;
            n ++;
        }
    }

    for (i = 0; i < n - 1; i ++)
        printf("%d ", primes[i]);
    printf("%d\n", primes[n - 1]);

    return 0;
}
// 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97
