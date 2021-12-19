void printf(char *, ...);

static int __get_index_cnt = 0;

static int N = 100;

int get_index() {
    __get_index_cnt ++;
    printf("[%d] Calling get_index()\n", __get_index_cnt);
    return N - 1;
}

extern int main() {
    int a[N];
    int b[10];

    for (int i = 0; i < N; i ++)
        a[i] = 10;

    b[0] = 3;

    memset((char *)a, '\0', N * sizeof(int)); // make sure it updates `a`, but not `b`

    a[get_index()] ++; // how many time will we call get_index?
    b[0] |= 1 << 10;

    printf("a[%d] = %d, b[0] = %d\n", get_index(), a[get_index()], b[0]);

    return 0;
}
// [1] Calling get_index()
// [2] Calling get_index()
// [3] Calling get_index()
// a[99] = 1, b[0] = 1027