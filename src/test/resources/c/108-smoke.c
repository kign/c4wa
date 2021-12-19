void printf(char *, ...);

int foo(int ret) {
    printf("called foo(%d)\n", ret);
    return ret;
}

extern int main () {
    printf("Trying to evaluate foo(10)? foo(20): foo(30)\n");
    int res = foo(10)? foo(20): foo(30);
    printf("Result is %d\n", res);

    return 0;
}
// Trying to evaluate foo(10)? foo(20): foo(30)
// called foo(10)
// called foo(20)
// Result is 20
