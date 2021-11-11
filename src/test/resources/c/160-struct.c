void printf();

struct Point { char color; float x; float y; float z; };

double sqrt(double x) {
    double precision = 1.0e-9;
    double a = 0.;
    double b = x;
    do {
        double c = (a + b)/2.;

        if (c * c > x)
            b = c;
        else
            a = c;
    }
    while(b - a > precision);

    return (a + b)/2.0;
}

double dot(struct Point * A, struct Point * B) {
    return (double)(A->x * B->x + A->y * B->y + A->z * B->z);
}

double len(struct Point * p) {
    return sqrt(dot(p,p));
}

void cross(struct Point * A, struct Point * B, struct Point * C) {
    C->x = A->y * B->z - A->z * B->y;
    C->y = A->z * B->x - A->x * B->z;
    C->z = A->x * B->y - A->y * B->x;
}

extern int main () {
    struct Point * A, * B, * C;
    int M = sizeof(struct Point);

    A = alloc(0, 1, struct Point);
    B = alloc(M, 1, struct Point);
    C = alloc(2*M, 1, struct Point);

    // constants are automatically cast to LHS type, or it makes no difference whether we write it like that ...
    A->x = 1;
    A->y = -2;
    A->z = 3;

    // or like this
    B->x = 4.0;
    B->y = 5.0;
    B->z = -6.0;

    cross(A, B, C);

    double cos_a = dot(A, B)/len(A)/len(B);
    double sin_a = len(C)/len(A)/len(B);

    printf("A = %.6f, %.6f, %.6f\n", A->x, A->y, A->z);
    printf("B = %.6f, %.6f, %.6f\n", B->x, B->y, B->z);
    printf("sin(⍺) = %.6f, cos(⍺) = %.6f\n", sin_a, cos_a);
    printf("sin(⍺)^2 + cos(⍺)^2 = %.6f\n", sin_a*sin_a + cos_a*cos_a);

    return 0;
}
// A = 1.000000, -2.000000, 3.000000
// B = 4.000000, 5.000000, -6.000000
// sin(⍺) = 0.682405, cos(⍺) = -0.730974
// sin(⍺)^2 + cos(⍺)^2 = 1.000000
