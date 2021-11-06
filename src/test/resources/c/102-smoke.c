extern void printf(...);
extern void main() {
    int sum = 0;
    int i = 1;
    int N = 100;
    do {
        sum = sum + i * i;
        i = i + 1;
    }
    while(i <= N);
    printf("1^2 + 2^2 + ... + %d^2 = %d\n", N, sum);
}
// 1^2 + 2^2 + ... + 100^2 = 338350