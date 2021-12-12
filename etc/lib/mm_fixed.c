// fixed-sized chunk allocation
#ifdef C4WA
#define assert(x) if(!(x)) abort ()
#else
#define assert(x) if(!(x)) { printf("‼️ ASSERTION: \"" #x "\" @ line %d\n", __LINE__); abort (); }
#endif

#ifdef C4WA
#define mm_malloc malloc
#define mm_free free
#else
static void * __builtin_memory;
static int __builtin_offset = 0;

static int __memsize = 1;
int memsize() {
    return __memsize;
}
void memgrow(int upgrade) {
    __memsize += upgrade;

    assert(__memsize <= EMULATE_LINEAR_MEMORY);
}
#endif // !defined(C4WA)

#define LM_PAGE 64000
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

#if !defined(C4WA) && EMULATE_LINEAR_MEMORY
    __builtin_memory = malloc(64000 * EMULATE_LINEAR_MEMORY);
#endif

    __mm_start = (unsigned long *)(__builtin_memory + __builtin_offset + __mm_extra_offset);
}

char * mm_malloc (int size) {
#ifdef USE_PRINTF
    const int verbose = 0;
#endif
    __mm_stat_allocated ++;

    if (!__mm_start)
        mm_init (0, size);

    if (size > __mm_size)
        abort ();

    const int unit_size = 1 + 8 * __mm_size; // # of "long" in one memory unit

    if (__mm_first < 0 && __mm_inuse == __mm_capacity) {
#ifdef USE_PRINTF
        if (verbose)
            printf("MM: We are at capacity with limit of %d storage units reached\n", __mm_capacity);
#endif
        int required = (__builtin_offset + __mm_extra_offset + (__mm_capacity + __mm_expand_by) * 8 * unit_size)/LM_PAGE + 1;
        if (required > memsize()) {
#ifdef USE_PRINTF
            if (verbose)
                printf("MM: Need %d more page(s) in addition to currently allocated %d\n", required - memsize(), memsize());
#endif
            memgrow(required - memsize());
        }
        __mm_capacity += __mm_expand_by;
    }

    if (__mm_first < 0) {
#ifdef USE_PRINTF
        if (verbose)
            printf("MM: Allocating additional map at index %d\n", __mm_inuse);
#endif
        assert(__mm_inuse < __mm_capacity);
        __mm_start[__mm_inuse * unit_size] = -1;
        __mm_first = __mm_inuse;
        __mm_inuse ++;
    }

    assert(__mm_first >= 0);
    unsigned long * cur = __mm_start + __mm_first * unit_size;
    assert(*cur != 0);

    int j = __builtin_ctzl(*cur);

    *cur ^= (unsigned long)1 << (unsigned long)j;
    char * result = (char*) cur + 8 + j * __mm_size;

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

void mm_free(char * box) {
    __mm_stat_freed ++;

    const int unit_size = 1 + 8 * __mm_size; // # of "long" in one memory unit

    int offset = box - (char *)__mm_start;
    int idx = offset / unit_size / 8;
    unsigned long * cur = __mm_start + idx * unit_size;
    int j = (box - (char *) cur - 8)/__mm_size;
    assert(j >= 0);
    assert(j < 64);
    assert(box == (char *)cur + 8 + j*__mm_size);
    assert((*cur & (unsigned long)1 << (unsigned long)j) == 0);
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
    * freed     = __mm_stat_freed;
    * current   = __mm_count_boxes();
    * in_use    = __mm_inuse;
    * capacity  = __mm_capacity;
}
