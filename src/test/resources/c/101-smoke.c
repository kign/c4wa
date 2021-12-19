void printf(char *, ...);
extern int main() {
    int a, b, c;
    a = 29; b = 11;
    c = a * a * a + b * b * b;
    printf("29^3 + 11^3 = %d\n", c);
    return 0;
}
// 29^3 + 11^3 = 25720