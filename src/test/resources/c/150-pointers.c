void printf();

extern int main () {
    int *a = alloc(10, 3, int);
    a[0] = 15;
    int* b = a + 1;
    *b = 13;
    *(a + 2) = 11;
    a[3] = *a * *b * a[2];
    printf("%d * %d * %d = %d\n", a[0], a[1], a[2], *(a + 3));

    return 0;
}
// 15 * 13 * 11 = 2145