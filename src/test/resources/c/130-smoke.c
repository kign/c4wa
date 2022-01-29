#ifndef C4WA
#include <math.h>
#else
#define sqrtf sqrt
#endif

void printf(char * fmt, ...);

extern int main () {
    double dv;
    float fv;

    for (int i = 0; i < 10; i ++) {
        dv = i;
        fv = i;
        printf("√%d = %.6f = %.6lf\n", i, sqrtf(fv), sqrt(dv));
    }

    return 0;
}
// √0 = 0.000000 = 0.000000
// √1 = 1.000000 = 1.000000
// √2 = 1.414214 = 1.414214
// √3 = 1.732051 = 1.732051
// √4 = 2.000000 = 2.000000
// √5 = 2.236068 = 2.236068
// √6 = 2.449490 = 2.449490
// √7 = 2.645751 = 2.645751
// √8 = 2.828427 = 2.828427
// √9 = 3.000000 = 3.000000