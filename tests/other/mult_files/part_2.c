void printf ();

void bar (int level) {
    printf("bar(%d)\n", level);
    if (level > 0)
        foo(level - 1);
}
