void printf(char * fmt, ...);
//#include <stdio.h>

static short no_argument_short(void);

float no_argument_float() {
    return -1.4118e2;
}

short no_argument_short() {
    return 57;
}

double add_11(float x) {
    return x + 11;
}

float add_19(double x) {
    return (float)x + 19;
}

extern int main() {
    double dblVal = no_argument_float ();
    long longVal = -18;
    float fltVal = 21.e4;

    fltVal = longVal;

    longVal = no_argument_short();

    printf("Result is %.6f\n", dblVal + longVal + add_11(-4) + add_11(3.2) + fltVal/2 + add_19(fltVal));

    printf("[Testing compile-time <<]\n");
    fltVal = (unsigned long)1 << 63;
    printf("2^63 = %.15e [unsigned]\n", fltVal);
    fltVal = 1<<31;
    printf("2^31 = %.2f [signed]\n", fltVal);
    fltVal = (unsigned int)1<<(unsigned int) 31;
    printf("2^31 = %.2f [unsigned]\n", fltVal);

    printf("[Testing run-time <<]\n");
    int a = 63;
    fltVal = (unsigned long)1 << a;
    printf("2^63 = %.15e [unsigned]\n", fltVal);
    fltVal = 1<<31;
    printf("2^31 = %.2f [signed]\n", fltVal);
    unsigned int b = 31;
    fltVal = (unsigned int)1<<b;
    printf("2^31 = %.2f [unsigned]\n", fltVal);

    return 0;
}
// Result is -70.979993
// [Testing compile-time <<]
// 2^63 = 9.223372036854776e+18 [unsigned]
// 2^31 = -2147483648.00 [signed]
// 2^31 = 2147483648.00 [unsigned]
// [Testing run-time <<]
// 2^63 = 9.223372036854776e+18 [unsigned]
// 2^31 = -2147483648.00 [signed]
// 2^31 = 2147483648.00 [unsigned]
