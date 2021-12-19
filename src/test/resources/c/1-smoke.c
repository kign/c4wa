void printf(char *, ...);

extern int add (int a, int b) {
    printf("a = %d, b = %d\n", a, b);
    return a + b;
}
extern int main() {
    int a = 7;
    int b = 14;
    printf("%d + %d = %d\n", a, b, add(a,b));
    return 0;
}
// a = 7, b = 14
// 7 + 14 = 21
