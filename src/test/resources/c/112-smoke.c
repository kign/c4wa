void printf(char *, ...);

int add(int a, int b) {
    return a + b;
}

extern int main () {
    int a[2];
    a[0] = -17;
    a[1] = 11;
    int res = add(a[0], a[1]);
    printf("%d + %d = %d\n", a[0], a[1], res);
    return 0;
}
// -17 + 11 = -6