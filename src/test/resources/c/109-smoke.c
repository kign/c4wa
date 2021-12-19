void printf(char *, ...);

static void test3_int(int, int, int);
static void test3_long(long, long, long);

int foo32(int ret) {
    printf("called foo(%d); ", ret);
    return ret;
}

long foo64(long ret) {
    printf("called foo(%d); ", ret);
    return ret;
}

void test2_int(int a, int b) {
    int res = foo32(a) && foo32(b);
    printf("➾ %d && %d = %d\n", a, b, res);
    res = foo32(a) || foo32(b);
    printf("➾ %d || %d = %d\n", a, b, res);
}

void test2_long(long a, long b) {
    int res = foo64(a) && foo64(b);
    printf("➾ [64] %d && %d = %d\n", a, b, res);
    res = foo64(a) || foo64(b);
    printf("➾ [64] %d || %d = %d\n", a, b, res);
}

void test2(int a, int b) {
    test2_int(a, b);
    test2_long((long)a, (long)b);
}

void test3(int a, int b, int c) {
    test3_int(a, b, c);
    test3_long((long)a, (long)b, (long)c);
}

void test3_int(int a, int b, int c) {
    int res = foo32(a) && foo32(b) && foo32(c);
    printf("➾ %d && %d && %d = %d\n", a, b, c, res);
    res = foo32(a) || foo32(b) || foo32(c);
    printf("➾ %d || %d || %d = %d\n", a, b, c, res);
}

void test3_long(long a, long b, long c) {
    int res = foo64(a) && foo64(b) && foo64(c);
    printf("➾ [64] %d && %d && %d = %d\n", a, b, c, res);
    res = foo64(a) || foo64(b) || foo64(c);
    printf("➾ [64] %d || %d || %d = %d\n", a, b, c, res);
}

extern int main () {
    int a, b, c;
    printf("=> Double argument testing\n");

    a = 10; b = 20;
    test2(a, b);
    a = 0; b = -2;
    test2(a, b);
    a = -5; b = 0;
    test2(a, b);
    a = 0; b = 0;
    test2(a, b);

    printf("=> Triple argument testing\n");
    a = 0; b = 0; c = 11;
    test3(a, b, c);
    a = 0; b = -13; c = 0;
    test3(a, b, c);
    a = 19; b = 0; c = 0;
    test3(a, b, c);
    a = 2; b = -1; c = 0;
    test3(a, b, c);
    a = -4; b = 0; c = 8;
    test3(a, b, c);
    a = 0; b = 13; c = 7;
    test3(a, b, c);
    a = 1; b = -9; c = 11;
    test3(a, b, c);
    a = 0; b = 0; c = 0;
    test3(a, b, c);

    return 0;
}
// => Double argument testing
// called foo(10); called foo(20); ➾ 10 && 20 = 1
// called foo(10); ➾ 10 || 20 = 1
// called foo(10); called foo(20); ➾ [64] 10 && 20 = 1
// called foo(10); ➾ [64] 10 || 20 = 1
// called foo(0); ➾ 0 && -2 = 0
// called foo(0); called foo(-2); ➾ 0 || -2 = 1
// called foo(0); ➾ [64] 0 && -2 = 0
// called foo(0); called foo(-2); ➾ [64] 0 || -2 = 1
// called foo(-5); called foo(0); ➾ -5 && 0 = 0
// called foo(-5); ➾ -5 || 0 = 1
// called foo(-5); called foo(0); ➾ [64] -5 && 0 = 0
// called foo(-5); ➾ [64] -5 || 0 = 1
// called foo(0); ➾ 0 && 0 = 0
// called foo(0); called foo(0); ➾ 0 || 0 = 0
// called foo(0); ➾ [64] 0 && 0 = 0
// called foo(0); called foo(0); ➾ [64] 0 || 0 = 0
// => Triple argument testing
// called foo(0); ➾ 0 && 0 && 11 = 0
// called foo(0); called foo(0); called foo(11); ➾ 0 || 0 || 11 = 1
// called foo(0); ➾ [64] 0 && 0 && 11 = 0
// called foo(0); called foo(0); called foo(11); ➾ [64] 0 || 0 || 11 = 1
// called foo(0); ➾ 0 && -13 && 0 = 0
// called foo(0); called foo(-13); ➾ 0 || -13 || 0 = 1
// called foo(0); ➾ [64] 0 && -13 && 0 = 0
// called foo(0); called foo(-13); ➾ [64] 0 || -13 || 0 = 1
// called foo(19); called foo(0); ➾ 19 && 0 && 0 = 0
// called foo(19); ➾ 19 || 0 || 0 = 1
// called foo(19); called foo(0); ➾ [64] 19 && 0 && 0 = 0
// called foo(19); ➾ [64] 19 || 0 || 0 = 1
// called foo(2); called foo(-1); called foo(0); ➾ 2 && -1 && 0 = 0
// called foo(2); ➾ 2 || -1 || 0 = 1
// called foo(2); called foo(-1); called foo(0); ➾ [64] 2 && -1 && 0 = 0
// called foo(2); ➾ [64] 2 || -1 || 0 = 1
// called foo(-4); called foo(0); ➾ -4 && 0 && 8 = 0
// called foo(-4); ➾ -4 || 0 || 8 = 1
// called foo(-4); called foo(0); ➾ [64] -4 && 0 && 8 = 0
// called foo(-4); ➾ [64] -4 || 0 || 8 = 1
// called foo(0); ➾ 0 && 13 && 7 = 0
// called foo(0); called foo(13); ➾ 0 || 13 || 7 = 1
// called foo(0); ➾ [64] 0 && 13 && 7 = 0
// called foo(0); called foo(13); ➾ [64] 0 || 13 || 7 = 1
// called foo(1); called foo(-9); called foo(11); ➾ 1 && -9 && 11 = 1
// called foo(1); ➾ 1 || -9 || 11 = 1
// called foo(1); called foo(-9); called foo(11); ➾ [64] 1 && -9 && 11 = 1
// called foo(1); ➾ [64] 1 || -9 || 11 = 1
// called foo(0); ➾ 0 && 0 && 0 = 0
// called foo(0); called foo(0); called foo(0); ➾ 0 || 0 || 0 = 0
// called foo(0); ➾ [64] 0 && 0 && 0 = 0
// called foo(0); called foo(0); called foo(0); ➾ [64] 0 || 0 || 0 = 0
