void printf ();

const int N = 100;

int gen1(int * primes) {
    primes[0] = 2;
    int i;
    int n = 1;
    for (int p = 3; p < N; p += 2) {
        for(i = 1; i < n && primes[i] * primes[i] < p && p % primes[i] != 0; i ++);
        if (i >= n || p % primes[i] != 0) {
            primes[n] = p;
            n ++;
        }
    }

    return n;
}

int gen2(int * primes) {
    primes[0] = 2;
    int i;
    int n = 1;
    for (int p = 3; p < N; p += 2) {
        int d = 0;
        for(i = 1; i < n; i ++) {
            d = primes[i];
            if (d * d >= p || p % d == 0)
                break;
        }
        if (d > 0 && d < p && p % d == 0)
                continue;

        primes[n] = p;
        n ++;
    }
    return n;
}

extern int main () {
    int i;
    int * primes = alloc(0, 50, int);

    for (int iter = 0; iter < 2; iter ++) {
        int n = (iter == 0) ? gen1(primes) : gen2(primes);

        for (i = 0; i < n - 1; i ++)
            printf("%d ", primes[i]);
        printf("%d\n", primes[n - 1]);
    }

    return 0;
}
// 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97
// 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97
