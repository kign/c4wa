#ifdef C4WA

void printf(char *, ...);
#define NULL 0

#else

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define min(a,b) ((a) < (b))?(a):(b)

#endif

#define assert(x) if(!(x)) { printf("‼️ ASSERTION: \"" #x "\" @ line %d\n", __LINE__); abort (); }

// value > 0: we emulate WASM linear memory with fixed maximum # of pages equal to this value
// value = 0: we use malloc() and free()
// we make this value visible to C4WA mode to keep statistics consistent
#define EMULATE_LINEAR_MEMORY 100

/* ---------------------- MEMORY MANAGER START ---------------------- */
#define USE_PRINTF
#if defined(C4WA) || EMULATE_LINEAR_MEMORY

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

static void * __mm_memory = 0;


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

void * mm_malloc (int size) {
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
        return (void *)cur + 8 + j * a_size;
    }
}

void mm_free(void * address) {
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
        int j = (address - (void*)cur - 8)/a_size;

#ifdef USE_PRINTF
        if (verbose)
            printf("free::small[%d](<size %d (n = %d), j = %d>\n", idx, a_size, n, j);
#endif
        assert (j >= 0 && j < bits);
        assert (address == (void *)cur + 8 + j * a_size);
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

#endif // defined(C4WA) || EMULATE_LINEAR_MEMORY

/* ----------------------- MEMORY MANAGER END ----------------------- */

static unsigned int seed = 57;
double mulberry32() {
    seed += (unsigned int) 1831565813;
    unsigned int t = seed;
    t = (t ^ t >> (unsigned int)15) * (t | (unsigned int)1);
    t ^= t + (t ^ t >> (unsigned int)7) * (t | (unsigned int)61);
    return (double)(t ^ t >> (unsigned int)14) / 4294967296.0;
}

char * allocate_data(int id, int size) {
    assert(size >= 2*sizeof(int));
    char * data = mm_malloc(size);
    memset(data, '\0', size);
    *(int *)data = id;
    *(int *)(data + sizeof(int)) = id ^ 816191;

    return data;
}

void print_histogram() {
    const int s0 = 128;
    const int hsize = 20;
    int lim = s0/2;
    int i, j;

    for(j = hsize - 1; j >= 1 && !__mm_report_histogram[j]; j --);

    for(i = 0; i <= j; i ++) {
        lim = i > 6? (64 * __mm_min + 12) * (i - 7) + 64 * __mm_min + 8 : 2 * lim;
        if (i < hsize-1)
            printf("%6d", lim);
        else
            printf(" unlim");
        if (__mm_report_histogram[i])
            printf(" %d\n", __mm_report_histogram[i]);
        else
            printf(" -\n");
    }
}

void verify_data(char * data) {
    int id = *(int *)data;
    int integrity = *(int *)(data + sizeof(int));

    assert(integrity == (id ^ 816191));
}

void test_uniform(int n_units, int n_iter, int size) {
    char ** storage = (char **) mm_malloc(n_units * 8);
    memset(storage, '\0', n_units * 8);

    printf("Starting memory test with %d empty \"unit\" pointers and %d iterations\n", n_units, n_iter);

    for (int iter = 0; iter < n_iter; iter ++) {
        int idx = (int)(mulberry32() * (double)n_units);

        if (storage[idx]) {
            verify_data(storage[idx]);
//            printf("⬅️ Releasing index %d, id %d\n", idx, *(int*)storage[idx]);
            mm_free(storage[idx]);
            storage[idx] = NULL;
        }
        else {
//            printf("➡️ Allocating index %d, id %d\n", idx, 1 + iter);
            storage[idx] = allocate_data(1 + iter, size);
        }
    }

    char * units = mm_print_units();
    printf("%s\n", units);
    mm_free(units);
    mm_free(storage);
    printf("Finished fixed memory test\n");
}

void test_nonuniform(int n_units, int n_iter, int size) {
    char ** storage = (char **) mm_malloc(n_units * 8);
    memset(storage, '\0', n_units * 8);

    printf("Starting memory test with %d empty \"unit\" pointers and %d iterations\n", n_units, n_iter);

    for (int iter = 0; iter < n_iter; iter ++) {
        int idx = (int)(mulberry32() * (double)n_units);

        if (storage[idx]) {
            verify_data(storage[idx]);
//            printf("Releasing index %d, id %d\n", idx, *(int*)storage[idx]);
            mm_free(storage[idx]);
            storage[idx] = NULL;
        }
        else {
            double r = mulberry32();
            storage[idx] = allocate_data(1 + iter, (int)((double)(size - 8) * r * r * r * r) + 8);
//            printf("Allocating index %d, id %d\n", idx, 1 + iter);
        }
    }

    char * units = mm_print_units();
    printf("%s\n", units);
    mm_free(units);
    mm_free(storage);
    printf("Finished variable memory test\n");
}

extern int main () {
    test_uniform(1000, 100000, 108);
    test_uniform(10, 1000, 10000);
    test_nonuniform(100, 10000, 100000);
    print_histogram ();
//    abort();
    return 0;
}
// Starting memory test with 1000 empty "unit" pointers and 100000 iterations
// S<6|free=0>S<0|free=5>S<0|free=2>S<0|free=4>S<0|free=14>S<0|free=23>S<0|free=19>S<0|free=8>S<0|free=5>S<0|free=4>S<3|free=7>
// Finished fixed memory test
// Starting memory test with 10 empty "unit" pointers and 1000 iterations
// S<6|free=1>S<0|free=5>S<0|free=2>S<0|free=4>S<0|free=14>S<0|free=23>S<0|free=19>S<0|free=7>S<0|free=5>S<0|free=4>S<3|free=7>B<2>B<2>B<2>B<2>B<2>..B<2>....
// Finished fixed memory test
// Starting memory test with 100 empty "unit" pointers and 10000 iterations
// S<0|free=53>S<0|free=5>S<0|free=2>S<0|free=4>S<0|free=14>S<0|free=23>S<0|free=19>S<0|free=8>S<0|free=5>S<0|free=4>S<3|free=3>B<2>B<2>B<2>B<2>B<2>B<2>B<2>S<5|free=1>S<6|free=0>S<4|free=2>S<6|free=1>S<5|free=0>S<6|free=0>B<2>B<2>.B<4>.S<1|free=32>B<2>B<3>B<7>B<4>B<5>.B<3>..B<2>B<5>B<5>...B<4>B<12>....B<6>...........B<7>B<3>B<7>...S<2|free=15>B<10>B<9>.................B<10>.............................B<11>B<10>............................................................
// Finished variable memory test
//    128 51172
//    256 182
//    512 228
//   1024 255
//   2048 302
//   4096 327
//   8192 405
//   8200 -
//  16404 1041
//  24608 320
//  32812 299
//  41016 202
//  49220 192
//  57424 159
//  65628 153
//  73832 160
//  82036 123
//  90240 128
//  98444 115
//  unlim 15
