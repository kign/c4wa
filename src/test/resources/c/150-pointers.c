void printf(char *, ...);

extern char * malloc(int);

extern int main () {
    int *a = (int *) malloc(3 * sizeof(int));
    a[0] = 15;
    int* b = a + 1;
    *b = 13;
    *(a + 2) = 11;
    a[3] = *a * *b * a[2];
    printf("%d * %d * %d = %d\n", a[0], a[1], a[2], *(a + 3));

    return 0;
}
// 15 * 13 * 11 = 2145