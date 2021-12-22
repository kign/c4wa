long foo(void * a) {
    void g;
}

extern int main () {
    float f_ptr[3];
    double * d_ptr;

    d_ptr = f_ptr;

    return 0;
}