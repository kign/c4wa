void printf ();

void force_stack_var(long * x) {
    printf("force_stack_var(%ld)\n", *x);
}

void test_1 () {
    do {
        long a = 10;
        a *= 3;
        printf("a = %lx\n", a);
    }
    while(0);

    do {
        long b = 11;
        b *= 4;
        printf("a = %lx\n", b);
    }
    while(0);
}

void test_2 () {
    do {
        long a = 10;
        a *= 3;
        printf("a = %lx\n", a);
    }
    while(0);

    do {
        long b = 11;
        force_stack_var(&b);
        b *= 4;
        printf("a = %lx\n", b);
    }
    while(0);
}

void test_3 () {
    int k = 10;
    double g;
    printf("k = %d\n", k);

    {
        printf("k = %d\n", k);

        double k = -10;

        printf("now k = %.2f\n", k);

        g = -k - 1.5;
    }

    printf("g = %.2f\n", g);
}

extern int main () {
    int i = 1;

    for (int j = 0; j < 10;) {
        i += j;
        j ++;
    }

    for(;0;) {
        i *= 2;
        int k = i < 10000;
    }

    int j = 0;
    for(int i = 0; i < 10; i ++)
        j += i;

    {
        int i = -11;
        i += j;
        printf("[block] i = %d\n", i);
    }

    if (j > 0) {
        int j = -14;
        i -= j;
        printf("[block] j = %d\n", j);
    }

    printf("i = %d, j = %d\n", i, j);

    test_1 ();
    test_2 ();
    test_3 ();

    return 0;
}
// [block] i = 34
// [block] j = -14
// i = 60, j = 45
// a = 1e
// a = 2c
// a = 1e
// force_stack_var(11)
// a = 2c
// k = 10
// k = 10
// now k = -10.00
// g = 8.50