void printf(char *, ...);

extern char * malloc(int);

char * alloc_storage(int n) {
    int s = (n + 8*sizeof(long) - 1)/8/sizeof(long);
    printf("Requested %d bits of storage, using %d long's\n", n, s);
    unsigned long * sto = (unsigned long *) malloc(s * sizeof(long));

    for (int i = 0; i < s; i ++)
        sto[i] = (long) 0;

    return (char *) sto;
}

void save(char * S, int n, int val) {
    unsigned long * sto = (unsigned long *) S;

    int a = n/sizeof(long);
    int b = n % sizeof(long);
    unsigned long two_power_b = 1 << (unsigned long) b;

    sto[a] = val? sto[a] | two_power_b
                : sto[a] & ~two_power_b;
}

int read(char * S, int n) {
    unsigned long * sto = (unsigned long *) S;

    int a = n/sizeof(long);
    int b = n % sizeof(long);
    unsigned long two_power_b = 1 << (unsigned long) b;

    return (int)((sto[a] & two_power_b) >> (unsigned long) b);
}

void print_storage(char * S, int N) {
    for (int n = 0; n < N; n ++)
        printf("%d", read(S, n));
    printf("\n");
}

extern int main () {
    int a = 1979;
    printf("%d >> 2 = %d, %d << 2 = %d\n", a, a >> 2, a, a << 2);

    int N = 8;
    char * S = alloc_storage(N);
    save(S, 0, 1);
    save(S, 5, 1);
    print_storage(S, N);

    N = 150;
    S = alloc_storage(N);
    save(S, 0, 1);
    save(S, 1, 1);
    for (int p = 2; p < N; p ++)
        for (int d = 0; d * d <= p && !read(S, p); d ++)
            if (!read(S, d) && p % d == 0)
                save(S, p, 1);

    printf("=>");
    for (int i = 0; i < N; i ++)
        if (!read(S, i))
            printf(" %d", i);
    printf("\n");

    return 0;
}
// 1979 >> 2 = 494, 1979 << 2 = 7916
// Requested 8 bits of storage, using 1 long's
// 10000100
// Requested 150 bits of storage, using 3 long's
// => 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97 101 103 107 109 113 127 131 137 139 149