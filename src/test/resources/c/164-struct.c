void printf();
extern char * malloc(int);

struct Complex { float x, y; };
struct Triangle { struct Complex A, B, C; };

struct Complex * _new () {
    struct Complex * res = (struct Complex *) malloc(sizeof(struct Complex));
    return res;
}

void init(struct Complex * a, double x, double y) {
    a->x = (float)x;
    a->y = (float)y;
}

void add(struct Complex * a, struct Complex * b, struct Complex * c) {
    c->x = a->x + b->x;
    c->y = a->y + b->y;
}

void sub(struct Complex * a, struct Complex * b, struct Complex * c) {
    c->x = a->x - b->x;
    c->y = a->y - b->y;
}

void mul(struct Complex * a, struct Complex * b, struct Complex * c) {
    c->x = a->x * b->x - a->y * b->y;
    c->y = a->x * b->y + a->y * b->x;
}

void div(struct Complex * a, struct Complex * b, struct Complex * c) {
    float m2 = b->x * b->x + b->y * b->y;
    c->x = (a->x * b->x + a->y * b->y)/m2;
    c->y = (b->x * a->y - b->y * a->x)/m2;
}

void copy(struct Complex * a, struct Complex * b) {
    a->x = b->x;
    a->y = b->y;
}

float mod(struct Complex * a) {
    return sqrt(a->x * a->x + a->y * a->y);
}

void shift(struct Triangle *a, struct Triangle *b) {
    copy(&(b->B), &(a->A));
    copy(&(b->C), &(a->B));
    copy(&(b->A), &(a->C));
}

float C(struct Triangle * tri) {
    struct Complex a, b, c, t;
    sub(&(tri->A), &(tri->B), &c);
    sub(&(tri->B), &(tri->C), &a);
    sub(&(tri->C), &(tri->A), &b);

    div(&b, &a, &t);
    return mod(&c)/(t.y/mod(&t));
}

void law_of_sines(struct Triangle * tri_1) {
    struct Triangle tri_2, tri_3;
    shift(tri_1, &tri_2);
    shift(&tri_2, &tri_3);

    printf("Verifying: %.6f, %.6f, %.6f\n", C(tri_1), C(&tri_2), C(&tri_3));
}

extern int main () {
    struct Complex x, y, z, t;

    init(&x, 0.5, 0.75);
    init(&y, -1.3, 1.1);
    div(&x, &y, &z);
    printf("(%.6f,%.6f)%c(%.6f,%.6f) = (%.6f,%.6f)\n", x.x, x.y, '/', y.x, y.y, z.x, z.y);
    mul(&y, &z, &t);
    printf("(%.6f,%.6f)%c(%.6f,%.6f) = (%.6f,%.6f)\n", y.x, y.y, '*', z.x, z.y, t.x, t.y);

    struct Triangle tri;

    init(&(tri.A), 1., 2.5);
    init(&(tri.B), 4., -1.0);
    init(&(tri.C), 11., 4.);

    law_of_sines(&tri);

    return 0;
}
// (0.500000,0.750000)/(-1.300000,1.100000) = (0.060345,-0.525862)
// (-1.300000,1.100000)*(0.060345,-0.525862) = (0.500000,0.750000)
// Verifying: 10.151491, 10.151492, 10.151492
