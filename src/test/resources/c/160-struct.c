void printf();

struct Point { char color; float x; float y; float z; };

float dot(struct Point * A, struct Point * B) {
    return A->x * B->x + A->y * B->y + A->z * B->z;
}

extern int main () {
    struct Point * A, * B, * C;
    int M = sizeof(struct Point);

    A = alloc(0, 1, struct Point);
    B = alloc(M, 1, struct Point);
    B = alloc(2*M, 1, struct Point);

    // constants are automatically cast to LHS type, or it makes no difference whether we write it like that ...
    A->x = 1;
    A->y = -2;
    A->z = 3;

    // or like this
    B->x = 4.0;
    B->y = 5.0;
    B->z = -6.0;

    printf("A = %.6f, %.6f, %.6f\n", A->x, A->y, A->z);
    printf("B = %.6f, %.6f, %.6f\n", B->x, B->y, B->z);
    printf("A . B = %.6f\n", dot(A, B));

    return 0;
}
// A = 1.000000, -2.000000, 3.000000
// B = 4.000000, 5.000000, -6.000000
// A . B = -24.000000
