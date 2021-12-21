# 1 "etc/lib/mm_fixed.c"
# 1 "<built-in>" 1
# 1 "<built-in>" 3
# 367 "<built-in>" 3
# 1 "<command line>" 1
# 1 "<built-in>" 2
# 1 "etc/lib/mm_fixed.c" 2
// fixed-sized chunk allocation
# 27 "etc/lib/mm_fixed.c"
static unsigned long * __mm_start = 0;
static int __mm_first = -1;
static int __mm_capacity = 0;
static int __mm_inuse = 0;
static int __mm_extra_offset = -1;
static int __mm_size = -1;
static int __mm_expand_by = 10;

static int __mm_stat_allocated = 0;
static int __mm_stat_freed = 0;

void mm_init(int extra_offset, int size) {
    if (extra_offset < 0 || size < 1 || __mm_start)
        abort ();

    __mm_extra_offset = extra_offset;
    __mm_size = size;





    __mm_start = (unsigned long *)(__builtin_memory + __builtin_offset + __mm_extra_offset);
}

void * malloc (int size) {



    __mm_stat_allocated ++;

    if (!__mm_start)
        mm_init (0, size);

    if (size > __mm_size)
        abort ();

    const int unit_size = 1 + 8 * __mm_size; // # of "long" in one memory unit

    if (__mm_first < 0 && __mm_inuse == __mm_capacity) {




        int required = (__builtin_offset + __mm_extra_offset + (__mm_capacity + __mm_expand_by) * 8 * unit_size)/64000 + 1;
        if (required > memsize()) {




            memgrow(required - memsize());
        }
        __mm_capacity += __mm_expand_by;
    }

    if (__mm_first < 0) {




        if(!(__mm_inuse < __mm_capacity)) abort ();
        __mm_start[__mm_inuse * unit_size] = -1;
        __mm_first = __mm_inuse;
        __mm_inuse ++;
    }

    if(!(__mm_first >= 0)) abort ();
    unsigned long * cur = __mm_start + __mm_first * unit_size;
    if(!(*cur != 0)) abort ();

    int j = __builtin_ctzl(*cur);

    *cur ^= (unsigned long)1 << (unsigned long)j;
    void * result = (void*) cur + 8 + j * __mm_size;

    if (*cur == 0) {
        do {
            __mm_first ++;
        }
        while(__mm_first < __mm_inuse && !__mm_start[__mm_first * unit_size]);
        if (__mm_first == __mm_inuse)
            __mm_first = -1;
    }

    return result;
}

void free(void * box) {
    __mm_stat_freed ++;

    const int unit_size = 1 + 8 * __mm_size; // # of "long" in one memory unit

    int offset = box - (void *)__mm_start;
    int idx = offset / unit_size / 8;
    unsigned long * cur = __mm_start + idx * unit_size;
    int j = (box - (void *) cur - 8)/__mm_size;
    if(!(j >= 0)) abort ();
    if(!(j < 64)) abort ();
    if(!(box == (void *)cur + 8 + j*__mm_size)) abort ();
    if(!((*cur & (unsigned long)1 << (unsigned long)j) == 0)) abort ();
    *cur ^= (unsigned long)1 << (unsigned long)j;

    if (idx < __mm_first)
        __mm_first = idx;
}

int __mm_count_boxes () {
    const int unit_size = 1 + 8 * __mm_size; // # of "long" in one memory unit

    int res = 0;
    for (int i = 0; i < __mm_inuse; i ++)
        res += (64 - __builtin_popcountl(__mm_start[i * unit_size]));
    return res;
}

void mm_stat(int * allocated, int * freed, int * current, int * in_use, int * capacity) {
    * allocated = __mm_stat_allocated;
    * freed = __mm_stat_freed;
    * current = __mm_count_boxes();
    * in_use = __mm_inuse;
    * capacity = __mm_capacity;
}
