#ifdef C4WA
#define clz(x) __builtin_clz(x)
#define ctz(x) __builtin_ctz(x)
#define clzl(x) __builtin_clzl(x)
#define ctzl(x) __builtin_ctzl(x)
#else
#define clz(x) ((x)? __builtin_clz(x) : 32)
#define ctz(x) ((x)? __builtin_ctz(x) : 32)
#define clzl(x) ((x)? __builtin_clzl(x) : 64)
#define ctzl(x) ((x)? __builtin_ctzl(x) : 64)
#endif

void printf();

void test_int(int x) {
    printf("x = %d, left = %d, right = %d\n", x, clz(x), ctz(x));
}

void test_long(unsigned long x) {
    printf("x = %ld, left = %d, right = %d\n", x, clzl(x), ctzl(x));
}

extern int main () {
    int a, b, c;

    a = 1 << 10, b = 1 << 20, c = 1 << 30;

    test_int(0);
    test_int(a);
    test_int(b);
    test_int(c);
    test_int(a|b);
    test_int(a|c);
    test_int(b|c);
    test_int(a|b|c);

    unsigned long A, B, C;

    A = (unsigned long)1 << (unsigned long)20, B = (unsigned long)1 << (unsigned long)40, C = (unsigned long)1 << (unsigned long)60;

    test_long((unsigned long)0);
    test_long(A);
    test_long(B);
    test_long(C);
    test_long(A|B);
    test_long(A|C);
    test_long(B|C);
    test_long(A|B|C);

    unsigned long x = (unsigned long)1 << (unsigned long)63;
    unsigned long y = 63;
    unsigned long z = (unsigned long)1 << y;

    printf("%d, %d, %d, %d\n", clzl(x), ctzl(x), clzl(z), ctzl(z));

    int v = 1 << 30;
    int w = 1 << 31;

    printf("[signed:32] v = %d, w = %d, min = %d, max = %d\n", v, w,  min(v, w), max(v, w));

    unsigned int uv = 1 << 30;
    unsigned int uw = 1 << 31;

    printf("[unsigned:32] v = %u, w = %u, min = %u, max = %u\n", uv, uw,  min(uv, uw), max(uv, uw));

    long r = (long)1 << 62;
    long s = (long)1 << 63;

    printf("[signed:64] r = %ld, s = %ld, min = %ld, max = %ld\n", r, s,  min(r, s), max(r, s));

    unsigned long ur = (unsigned long)1 << 62;
    unsigned long us = (unsigned long)1 << 63;

    printf("[unsigned:64] r = %lu, s = %lu, min = %lu, max = %lu\n", ur, us,  min(ur, us), max(ur, us));

    return 0;
}
// x = 0, left = 32, right = 32
// x = 1024, left = 21, right = 10
// x = 1048576, left = 11, right = 20
// x = 1073741824, left = 1, right = 30
// x = 1049600, left = 11, right = 10
// x = 1073742848, left = 1, right = 10
// x = 1074790400, left = 1, right = 20
// x = 1074791424, left = 1, right = 10
// x = 0, left = 64, right = 64
// x = 1048576, left = 43, right = 20
// x = 1099511627776, left = 23, right = 40
// x = 1152921504606846976, left = 3, right = 60
// x = 1099512676352, left = 23, right = 20
// x = 1152921504607895552, left = 3, right = 20
// x = 1152922604118474752, left = 3, right = 40
// x = 1152922604119523328, left = 3, right = 20
// 0, 63, 0, 63
// [signed:32] v = 1073741824, w = -2147483648, min = -2147483648, max = 1073741824
// [unsigned:32] v = 1073741824, w = 2147483648, min = 1073741824, max = 2147483648
// [signed:64] r = 4611686018427387904, s = -9223372036854775808, min = -9223372036854775808, max = 4611686018427387904
// [unsigned:64] r = 4611686018427387904, s = 9223372036854775808, min = 4611686018427387904, max = 9223372036854775808
