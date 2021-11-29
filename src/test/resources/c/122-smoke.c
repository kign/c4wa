void printf();

static int d = 3;

int mod_ceiling(int x) {
    return x % d == 0? x
           : x > 0?    x + d - x % d
           :           x - x % d;
}

int mod_floor(int x) {
    return x % d == 0? x
           : x > 0?    x - x % d
           :           x - d - x % d;
}

extern int main() {
    printf("x   x%%%d  floor ceil\n=====================\n", d);
    for (int i = -7; i <= 7; i ++)
        printf("%2d  %2d   %2d    %2d\n", i, i % d, mod_ceiling(i), mod_floor(i));

    return 0;
}
// x   x%3  floor ceil
// =====================
// -7  -1   -6    -9
// -6   0   -6    -6
// -5  -2   -3    -6
// -4  -1   -3    -6
// -3   0   -3    -3
// -2  -2    0    -3
// -1  -1    0    -3
//  0   0    0     0
//  1   1    3     0
//  2   2    3     0
//  3   0    3     3
//  4   1    6     3
//  5   2    6     3
//  6   0    6     6
//  7   1    9     6
