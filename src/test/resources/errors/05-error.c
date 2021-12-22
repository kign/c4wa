long foo(void * a) {
    foo(a+1);
}

int d1() {
    void * a;
    foo(*a);
    foo(a[0]);
}
