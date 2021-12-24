void printf(char * fmt, ...);

static int v = -1;

void print_global_v () {
    printf("global v = %d\n", v);
}

extern int main () {
    print_global_v ();
    printf("v@main = %d\n", v);
    int v = 18;
    print_global_v ();
    printf("v@main = %d\n", v);
    return 0;
}
// global v = -1
// v@main = -1
// global v = -1
// v@main = 18
