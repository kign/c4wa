void printf(char *, ...);

void foo(int * p_a, int * p_b, double * p_s) {
    *p_a = 11;
    *p_b = -13;
    *p_s += (double)*p_a / (double)*p_b;
}

void test_1 () {
    int a[2];
    double stat;

    a[1] = -4;
    stat = 11.;
    foo(&(a[0]), &(a[1]), &stat);
    a[0] ++;
    printf("a = [%d, %d], stat = %.6f\n", a[0], a[1], stat);
}

void test_2 () {
    int a[2];
    double stat = 4;

    a[0] = 19;
    foo(&(a[0]), &(a[1]), &stat);
    *(a + 1) *= 10;
    printf("a = [%d, %d], stat = %.6f\n", a[0], a[1], stat);
}

extern int main () {
    test_1 ();
    test_2 ();

    return 0;
}
// a = [12, -13], stat = 10.153846
// a = [11, -130], stat = 3.153846
