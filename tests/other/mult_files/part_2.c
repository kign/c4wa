void printf (char * fmt, ...);

void bar (int level) {
    printf("bar(%d)\n", level);
    if (level > 0)
        foo(level - 1);
}
