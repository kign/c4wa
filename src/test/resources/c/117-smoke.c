void printf();

int do_i_return_a_value_or_not (int x) {
    if (x > 0)
        return 1;
    else
        return -1;
}

extern int main () {
    int a[3];
    a[0] = 10;
    a[1] = -10;
    a[2] = 0;

    for (int i = 0; i < 3; i ++)
        printf("do_i_return_a_value_or_not(%d) = %d\n", a[i], do_i_return_a_value_or_not(a[i]));

    return 0;
}
// do_i_return_a_value_or_not(10) = 1
// do_i_return_a_value_or_not(-10) = -1
// do_i_return_a_value_or_not(0) = -1