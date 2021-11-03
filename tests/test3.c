extern void printf(...);
extern int add (int a, int b) {
    printf("a = %d, b = %d\n", a, b);
    return a + b;
}
extern int main() {
    int a = 7;
    int b = 14;
}