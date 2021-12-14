// universal memory manager
#ifdef C4WA
#define assert(x) if(!(x)) abort ()
#else
#define assert(x) if(!(x)) { printf("‼️ ASSERTION: \"" #x "\" @ line %d\n", __LINE__); abort (); }
#endif

#ifdef C4WA
#define mm_malloc malloc
#define mm_free free
#endif

#ifndef C4WA
static char * __builtin_memory;
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

static char * __mm_memory = 0;


// __mm_avail[i] = first available block of type MEM_MIN * 2^i, 0 <= i <= 6
static int * __mm_avail = 0;


// WASM page size, 64K
#define LM_PAGE 64000
// Minimal memory unit, 2^7
static int __mm_min = 0;

/* Memory block is a unit of size 64 * MEM_MIN + 12
 * <header: 4 bytes>  <map: 8 bytes>  <1=MEM_MIN> .... <64>
 * header = 0: block is free
 * header < 0: this is start of "big block", up to (64 * MEM_MIN + 8 + (-header - 1) * (64 * MEM_MIN + 12)) bytes
 * header > 0: is is collection of "small blocks" of size MEM_MIN * 2^(header - 1)
 *
 * malloc(size < MEM_MIN) : using size = MEM_MIN
 * malloc(MEM_MIN <= size <= 64 * MEM_MIN) : using "small collection" of {min 2^n | 2^n >= size}
 * malloc(size > 64 * MEM_MIN) : using N big blocks,
 *     N = (int) Math.ceil(size - (64 * MEM_MIN + 8))/(64 * MEM_MIN + 12)
 */

static int __mm_inuse = 0;
static int __mm_stat_allocated = 0;
static int __mm_stat_freed = 0;
static int __mm_extra_offset = -1;

#define MM_HIST_SIZE 20
static int * __mm_report_histogram = 0;

void mm_init(int mm_min) {
    if (__mm_min)
        abort ();

    __mm_min = mm_min;

#if !defined(C4WA) && EMULATE_LINEAR_MEMORY
    __builtin_memory = malloc(LM_PAGE * __memsize * EMULATE_LINEAR_MEMORY);
#endif

    __mm_extra_offset = 10*sizeof(int) + MM_HIST_SIZE*sizeof(int *);
    __mm_memory =  __builtin_memory + __builtin_offset + __mm_extra_offset;
    __mm_avail = (int *)(__builtin_memory + __builtin_offset);

    int i;
    for (i = 0; i <= 6; i ++)
        __mm_avail[i] = -1;

    __mm_report_histogram = (int *)(__builtin_memory + __builtin_offset + 10*sizeof(int));
    for (i = 0; i <= 6; i ++)
        __mm_report_histogram[i] = 0;
}

char * mm_malloc (int size) {
#ifdef USE_PRINTF
    const int verbose = 0;
#endif
    __mm_stat_allocated ++;

    if (!__mm_min)
        mm_init (128);

    int i, j, n, m, idx, state, required;
    const int unit = 64 * __mm_min + 12;

#ifdef USE_PRINTF
    if (verbose)
        printf("malloc(%d)\n", size);
#endif

    if (size > 64 * __mm_min) {
        // big block
        n = 2 + (size - (64 * __mm_min + 8))/(64 * __mm_min + 12);
#ifdef USE_PRINTF
        if (verbose)
            printf("Allocating %d big blocks\n", n);
#endif
        __mm_report_histogram[min(n + 6, MM_HIST_SIZE - 1)] ++;

        idx = 0;
        do {
            if (idx >= __mm_inuse) break;
            state = *(int *)(__mm_memory + idx * unit);
            if (state == 0) {
                for (j = idx + 1; j < __mm_inuse && j < idx + n && *(int *)(__mm_memory + j * unit) == 0; j ++);
                if (j - idx >= n) {
#ifdef USE_PRINTF
                    if (verbose)
                        printf("Found free stretch from %d to %d\n", idx, j - 1);
#endif
                    *(int *)(__mm_memory + idx * unit) = -n;
                    return __mm_memory + idx * unit + 4;
                }
                if (j == __mm_inuse)
                    break;
                idx = j;
            }
            else if (state < 0)
                idx += -state;
            else
                idx ++;
        }
        while(1);
        assert (idx <= __mm_inuse);

        int need = n - (__mm_inuse - idx);
#ifdef USE_PRINTF
        if (verbose)
            printf("Need %d more units\n", need);
#endif
        assert (need > 0);
        assert (need <= n);
        required = (__builtin_offset + __mm_extra_offset + (__mm_inuse + need) * unit)/LM_PAGE + 1;
        if (required > memsize()) {
#ifdef USE_PRINTF
            if (verbose)
                printf("Need %d more page(s) in addition to currently allocated %d\n", required - memsize(), memsize());
#endif
            memgrow(required - memsize());
        }
        for (i = 0; i < need; i ++)
            *(int *)(__mm_memory + (i + __mm_inuse) * unit) = 0;
#ifdef USE_PRINTF
        if (verbose)
            printf("Bumping __mm_inuse from %d to %d\n", __mm_inuse, __mm_inuse + need);
#endif
        __mm_inuse += need;
#ifdef USE_PRINTF
        if (verbose)
            printf("Following expansion, returning stretch from %d to %d\n", idx, idx + n - 1);
#endif
        *(int *)(__mm_memory + idx * unit) = -n;
        return __mm_memory + idx * unit + 4;
    }
    else {
        // small block
        int a_size = __mm_min;
        for(n = 0; a_size < size; n ++, a_size *= 2)
        assert(n <= 6);
        __mm_report_histogram[n] ++;
        idx = __mm_avail[n];
#ifdef USE_PRINTF
        if (verbose)
            printf("Actual size %d, n = %d, idx = %d\n", a_size, n, idx);
#endif
        if (idx < 0) {
            idx = 0;
            do {
                if (idx >= __mm_inuse) break;
                state = *(int *)(__mm_memory + idx * unit);
                if (state == 0 || (state == n + 1 && *(long *)(__mm_memory + idx * unit + 4) != 0) ) {
                    break;
                }
                else if (state > 0)
                    idx ++;
                else // state < 0
                    idx += -state;
            }
            while(1);
            assert (idx >= 0);
            assert (idx <= __mm_inuse);
            if (idx == __mm_inuse) {
                required = (__builtin_offset + __mm_extra_offset + (__mm_inuse + 1) * unit)/LM_PAGE + 1;
                if (required > memsize()) {
#ifdef USE_PRINTF
                    if (verbose)
                        printf("Need %d more page(s) in addition to currently allocated %d\n", required - memsize(), memsize());
#endif
                    memgrow(required - memsize());
                }
                __mm_inuse ++;
#ifdef USE_PRINTF
                if (verbose)
                    printf("Bumping __mm_inuse to %d\n", __mm_inuse);
#endif
                *(int *)(__mm_memory + idx * unit) = 0;
            }
#ifdef USE_PRINTF
            if (verbose)
                printf("Found available space at unit %d [%s]\n", idx,
                    *(int *)(__mm_memory + idx * unit) == 0? "not initialized" : "initialized");
            if (verbose)
                printf("Setting __mm_avail[%d] = %d\n", n, idx);
#endif
            __mm_avail[n] = idx;
        }

        state = *(int *)(__mm_memory + idx * unit);
        assert(state == 0 || state == n + 1);
        if (state == 0) {
            *(int *)(__mm_memory + idx * unit) = n + 1;
            // Setting last ("trailing") 2^(6-n) bits to 1 (= "available", rest to 0)
            int bits = 1 << (6-n);
            *(unsigned long *)(__mm_memory + idx * unit + 4) = (bits == 64? (unsigned long)0 : ((unsigned long)1 << (unsigned long)bits)) - 1;
#ifdef USE_PRINTF
            if (verbose)
                printf("Initializing unit %d to n = %d (actual size = %d) | %lx\n", idx, n, a_size, *(unsigned long *)(__mm_memory + idx * unit + 4));
#endif
        }
        unsigned long * cur = (unsigned long *)(__mm_memory + idx * unit + 4);
        assert(*cur != 0);
        j = __builtin_ctzl(*cur);

#ifdef USE_PRINTF
        if (verbose)
            printf("Using internal index %d\n", j);
#endif
        *cur ^= (unsigned long)1 << (unsigned long)j;
        if (*cur == 0) {
#ifdef USE_PRINTF
            if (verbose)
                printf("Unit at index %d is exhausted, setting __mm_avail[%d] = -1\n", idx, n);
#endif
            __mm_avail[n] = -1;
        }
        return (char *)cur + 8 + j * a_size;
    }
}

void mm_free(char * address) {
#ifdef USE_PRINTF
    const int verbose = 0;
#endif
    __mm_stat_freed ++;

    const int unit = 64 * __mm_min + 12;

    int i;
    int idx = (address - __mm_memory) / unit;
    int state = *(int *)(__mm_memory + idx * unit);

    assert(state != 0);

    if (state < 0) {
#ifdef USE_PRINTF
        if (verbose)
            printf("free::large[%d]<%d units>\n", idx, -state);
#endif
        assert(address == __mm_memory + idx * unit + 4);
        for (i = 0; i < -state; i ++)
            *(int *)(__mm_memory + (idx + i) * unit) = 0;
    }
    else {
        unsigned long * cur = (unsigned long *)(__mm_memory + idx * unit + 4);
        int n = state - 1;
        assert(n <= 6);
        int a_size = __mm_min;
        for (i = 0; i < n; i ++)
            a_size *= 2;
        int bits = 1 << (6-n);
        int j = (address - (char*)cur - 8)/a_size;

#ifdef USE_PRINTF
        if (verbose)
            printf("free::small[%d](<size %d (n = %d), j = %d>\n", idx, a_size, n, j);
#endif
        assert (j >= 0 && j < bits);
        assert (address == (char *)cur + 8 + j * a_size);
        assert((*cur & (unsigned long)1 << (unsigned long)j) == 0);
        *cur ^= (unsigned long)1 << (unsigned long)j;
        if (__mm_avail[n] < 0) {
#ifdef USE_PRINTF
            if (verbose)
                printf("Setting __mm_avail[%d] = %d\n", n, idx);
#endif
            __mm_avail[n] = idx;
        }
        else {
#ifdef USE_PRINTF
            if (verbose)
                printf("Not changing __mm_avail[%d] == %d\n", n, __mm_avail[n]);
#endif
        }

        assert (1 <= __builtin_popcountl(*cur));
        assert (__builtin_popcountl(*cur) <= bits);
        if (__mm_avail[n] != idx && __builtin_popcountl(*cur) == bits) {
#ifdef USE_PRINTF
            if (verbose)
                printf("__mm_avail[%d] = %d != %d; *cur = %lx; seems like we can reset state from %d to 0\n",
                        n, __mm_avail[n], idx, *cur, state);
#endif
           *(int *)(__mm_memory + idx * unit) = 0;
        }
    }
}

void __mm_itoa(int a, char ret[]) {
    const int N = 10;
    char buf[N];
    int n = N;

    do {
        int d = a % 10;
        a = a / 10;

        n --;
        buf[n] = (char)(48 + (int)d);
    } while(a > 0);

    memcpy(ret, buf + n, N - n);
    ret[N - n] = '\0';
}

void __mm_append_string(char * dst, char * src) {
    int len_dst, len_src;
    for(len_src = 0; src[len_src]; len_src ++);
    for(len_dst = 0; dst[len_dst]; len_dst ++);
    memcpy(dst + len_dst, src, len_src + 1);
}

void __mm_append_number(char * dst, int num) {
    char buf[10];
    __mm_itoa(num, buf);
    __mm_append_string(dst, buf);
}

char * mm_print_units() {
    const int unit = 64 * __mm_min + 12;
    char * buf = mm_malloc(1000);
    buf[0] = '\0';

    for (int idx = 0; idx < __mm_inuse; idx ++) {
        int state = *(int *)(__mm_memory + idx * unit);
        if (state == 0)
            __mm_append_string(buf, ".");
        else if (state < 0) {
            __mm_append_string(buf, "B<");
            __mm_append_number(buf, -state);
            __mm_append_string(buf, ">");
            idx += (-state - 1);
        }
        else {
            __mm_append_string(buf, "S<");
            __mm_append_number(buf, state - 1);
            __mm_append_string(buf, "|free=");
            __mm_append_number(buf, __builtin_popcountl(*(unsigned long *)(__mm_memory + idx * unit + 4)));
            __mm_append_string(buf, ">");
        }
    }

    return buf;
}

int * mm_histogram(int * p_count) {
    * p_count = MM_HIST_SIZE;
    return __mm_report_histogram;
}
