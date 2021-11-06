void printf(...);
extern int add (int a, int b) {
    printf("↳add(%d, %d)\n", a, b);
    return a + b;
}
extern void main() {
    int a, b, c;
    a = c = 11;
    b = -18;
    printf("%d + %d + %d = %d\n", a, b, c, add(add(a,b),c));
}
// ↳add(11, -18)
// ↳add(-7, 11)
// 11 + -18 + 11 = 4
