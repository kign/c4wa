void printf(char *, ...);
const int N = 100;
extern int main() {
    int sum = 0;
    int i = 1;
    do {
        sum = sum + i * i;
        i = i + 1;
    }
    while(i <= N);
    printf("1^2 + 2^2 + ... + %d^2 = %d\n", N, sum);
    return 0;
}
// 1^2 + 2^2 + ... + 100^2 = 338350