void printf ();

const double precision = 1.0e-9;

extern char * malloc(int);

void solve_square_equation(double a, double b, double c, double roots[2], int * p_status) {
    double d = b*b - 4.0*a*c;
    if (d < 0.) {
        *p_status = 0;
        return;
    }
    if(d == 0.) {
        roots[0] = -b/2./a;
        roots[1] = -b/2./a;
        *p_status = 1;
        return;
    }

    double sqrt_d = sqrt(d);

    roots[0] = (-b - sqrt_d)/2./a;
    roots[1] = (-b + sqrt_d)/2./a;
    *p_status = 2;
}

void try_solving(int a, int b, int c) {
//    double * roots = alloc(0, 2, double);
//    int * p_status = alloc(2 * sizeof(double), 1, int);
    double * roots = (double *) malloc(2 * sizeof(double));
    int * p_status = (int *) malloc(sizeof(int));

    solve_square_equation((double)a, (double)b, (double)c, roots, p_status);

    printf("a = %d, b = %d, c = %d: ", a, b, c);

    if (*p_status == 0)
        printf("no roots\n");
    else if (*p_status == 1)
        printf("one root %.6f\n", roots[0]);
    else if (*p_status == 2)
        printf("two roots, %.6f and %.6f\n", roots[0], roots[1]);

    free(roots);
    free(p_status);
}

extern int main () {
    try_solving(1, -2, 1);
    try_solving(10, 11, -7);
    try_solving(10, 11, 7);
    return 0;
}
// a = 1, b = -2, c = 1: one root 1.000000
// a = 10, b = 11, c = -7: two roots, -1.551249 and 0.451249
// a = 10, b = 11, c = 7: no roots
