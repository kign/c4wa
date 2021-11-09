void printf();

extern int main () {
    int *a = alloc(10, 2, int);
    a[0] = 15;
    *(a + 1) = 13;
    a[2] = a[0] * a[1];
    printf("%d * %d = %d\n", a[0], a[1], *(a + 2));

    return 0;
}
// 15 * 13 = 195