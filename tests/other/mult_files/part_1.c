void printf (char * fmt, ...);

extern void bar(int);

void foo (int level) {
    printf("foo(%d)\n", level);
    if (level > 0)
        bar(level - 1);
}

extern int main () {
    foo(10);
    return 0;
}