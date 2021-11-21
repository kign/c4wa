void printf();

void foo(int * p_a, int * p_b, double * p_s) {
    *p_a = 11;
    *p_b = -13;
    *p_s = (double)*p_a / (double)*p_b;
}

extern int main () {
    int a[2];
    double stat;

    a[0] = 19;
    foo(&(a[0]), &(a[1]), &stat);
    printf("a = [%d, %d], stat = %.6f\n", a[0], a[1], stat);

    return 0;
}
// a = [11, -13], stat = -0.846154