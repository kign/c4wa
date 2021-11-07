void printf(...);

const double precision = 1.0e-9;

double sqrt(double x) {
    double a = 0.;
    double b = x;
    do {
        double c = (a + b)/2.;

        if (c * c > x)
            b = c;
        else
            a = c;
    }
    while(b - a > 1.0e-9);

    return (a + b)/2.0;
}

extern int main() {
    int i = 2;
    do {
        printf("√%d = %f\n", i, sqrt((double)i));
        i = i + 1;
    }
    while(i <= 10);
    return 0;
}
// √2 = 1.4142135619185865
// √3 = 1.7320508075645193
// √4 = 2.0000000004656613
// √5 = 2.236067977210041
// √6 = 2.4494897428667173
// √7 = 2.645751311269123
// √8 = 2.8284271243028343
// √9 = 3.0000000000873115
// √10 = 3.162277660157997
