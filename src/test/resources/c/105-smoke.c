void printf();
static int test(int);

const int N = 100;
void v1 () {
    printf("2");
    for (int p = 3; p < N; p += 2) {
        int found = 0;
        for (int d = 3; d*d <= p; d += 2) {
            if (p % d == 0) {
                found = 1;
                break;
            }
        }
        if (!found)
            printf(" %d", p);
    }
    printf("\n");
}
void v2 () {
    printf("2");
    for (int p = 3; p < N; p += 2) {
        int d;
        for (d = 3; d*d <= p && p % d != 0; d += 2);
        if (d * d > p)
            printf(" %d", p);
    }
    printf("\n");
}
void v3 () {
    printf("2");
    for (int p = 3; p < N; p += 2)
        if (test(p))
            printf(" %d", p);
    printf("\n");
}
int test(int p) {
    if (p == 3)
        return 1;
    int d = 3;
    for (; d*d <= p && p % d != 0; d += 2);
    return p % d != 0;
}
extern int main () {
    v1();
    v2();
    v3();
    return 0;
}
// 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97
// 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97
// 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97
