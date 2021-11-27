void printf ();

static int __get_index_cnt = 0;

int get_index() {
    __get_index_cnt ++;
    printf("[%d] Calling get_index()\n", __get_index_cnt);
    return 99;
}
static int N = 100;

extern int main() {
    int a[N];

    for (int i = 0; i < N; i ++)
        a[i] = 10;

    memset((char *)a, '\0', N * sizeof(int));

    a[get_index()] ++; // how many time will we call get_index?

    printf("Result is %d\n", a[get_index()]);

    return 0;
}
// [1] Calling get_index()
// [2] Calling get_index()
// Result is 1