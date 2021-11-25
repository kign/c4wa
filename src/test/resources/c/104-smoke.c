void printf();

const double precision = 1.0e-9;

double my_sqrt(double x) {
    double a = 0.;
    double b = x;
    do {
        double c = (a + b)/2.;

        if (c * c > x)
            b = c;
        else
            a = c;
    }
    while(b - a > precision);

    return (a + b)/2.0;
}

extern int main() {
    int i = 2;
    do {
        printf("√%d = %.8f\n", i, my_sqrt((double)i));
        i ++;
    }
    while(i <= 10);
    return 0;
}
// √2 = 1.41421356
// √3 = 1.73205081
// √4 = 2.00000000
// √5 = 2.23606798
// √6 = 2.44948974
// √7 = 2.64575131
// √8 = 2.82842712
// √9 = 3.00000000
// √10 = 3.16227766
