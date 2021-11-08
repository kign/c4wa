void printf();

const int N = 100;
extern int main () {
    printf("2");
    for (int p = 3; p < N; p = p + 2) {
        int found = 0;
        for (int d = 3; d*d < p; d = d + 2) {
            if (p % d == 0) {
                found = 1;
                break;
            }
        }
        if (!found)
            printf(" %d", p);
    }
    printf("\n");
    return 0;
}
// 2 3 5 7 9 11 13 17 19 23 25 29 31 37 41 43 47 49 53 59 61 67 71 73 79 83 89 97