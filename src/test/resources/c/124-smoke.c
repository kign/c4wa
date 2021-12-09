void printf ();

extern int main () {
    long longNumber = -18;
    float floatNumber = 1.234e2;

    printf("longNumber = %ld, floatNumber = %.6f\n", longNumber, floatNumber);

    int intNumber = -57.4;
    double doubleNumber;

    doubleNumber = 11;

    printf("intNumber = %d, doubleNumber = %.6f\n", intNumber, doubleNumber);

    return 0;
}
// longNumber = -18, floatNumber = 123.400002
// intNumber = -57, doubleNumber = 11.000000
