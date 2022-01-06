void import_func_1(int, long, int);
void import_func_2(int, long, int);
double import_func_3(int, long, int);

float var_1;
float var_2;
extern float var_3 = 10;
extern long var_4 = 20;
static long var_5 = 1;

void test_1(int a) {
    import_func_1(a, 0, 0);
    var_1 = 1;
}

void test_2(int a) {
    import_func_2(0, 0, a);
    var_2 = 2;
}

extern void test_3(int a) {
    import_func_3(0, a, 0);
    var_3 = 3;
}

extern double test_4(int a) {
    import_func_1(0, a, 1);
    import_func_1(0, a, 1);
    var_4 = 4;
    return import_func_3(1, 2, a);
}

extern void xyz () {
    int a = 57;
    test_1(a);
    test_2(a);
    test_3(a);
    test_4(a);
    var_5 = 5;
}

