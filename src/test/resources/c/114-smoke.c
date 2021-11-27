void printf ();

int set_value_of_a_to_57(long * a) {
    *a = 57;
    return -1;
}

extern int main() {
    long a = 0; // must be retained by compiler, stack variable

    set_value_of_a_to_57(&a);

    printf("a = %d\n", a);

    return 0;
}
// a = 57