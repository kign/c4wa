void printf();
extern char * malloc(int);

struct Point { float x; float y; char color; float z; };

const    int M = sizeof(struct Point);

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

char * color(struct Point * p) {
    return p->color == 'r' ? "red" : (p->color == 'g' ? "green" : (p->color == 'b' ? "blue" : "unknown"));
}

extern int main () {
    struct Point * A, * B, * C;

    A = (struct Point *)malloc(M);
    B = (struct Point *)malloc(M);
    C = (struct Point *)malloc(M);

    // constants are automatically cast to LHS type, or it makes no difference whether we write it like that ...
    A->x = 1;
    A->y = -2;
    A->z = 3;
    A->color = 'r';

    // or like this
    B->x = 4.0;
    B->y = 5.0;
    B->z = -6.0;
    B->color = 'g';

    C->color = 'b';
    cross(A, B, C);

    double cos_a = dot(A, B)/len(A)/len(B);
    double sin_a = len(C)/len(A)/len(B);

    printf("A[%s] = %.6f, %.6f, %.6f\n", color(A), A->x, A->y, A->z);
    printf("B[%s] = %.6f, %.6f, %.6f\n", color(B), B->x, B->y, B->z);
    printf("C[%s] = %.6f, %.6f, %.6f\n", color(C), C->x, C->y, C->z);
    printf("sin(⍺) = %.6f, cos(⍺) = %.6f\n", sin_a, cos_a);
    printf("sin(⍺)^2 + cos(⍺)^2 = %.6f\n", sin_a*sin_a + cos_a*cos_a);

    return 0;
}
// A[red] = 1.000000, -2.000000, 3.000000
// B[green] = 4.000000, 5.000000, -6.000000
// C[blue] = -3.000000, 18.000000, 13.000000
// sin(⍺) = 0.682405, cos(⍺) = -0.730974
// sin(⍺)^2 + cos(⍺)^2 = 1.000000
