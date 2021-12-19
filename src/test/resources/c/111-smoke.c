void printf(char *, ...);

const int N = 20;

double factorial(int n) {
    printf("factorial(%d)\n", n);

    if (n == 1)
        return 1.0;

    return (double)n * factorial(n-1);
}

extern int main() {
    printf("%d! = %.0f\n", N, factorial(N));
    return 0;
}
// factorial(20)
// factorial(19)
// factorial(18)
// factorial(17)
// factorial(16)
// factorial(15)
// factorial(14)
// factorial(13)
// factorial(12)
// factorial(11)
// factorial(10)
// factorial(9)
// factorial(8)
// factorial(7)
// factorial(6)
// factorial(5)
// factorial(4)
// factorial(3)
// factorial(2)
// factorial(1)
// 20! = 2432902008176640000
