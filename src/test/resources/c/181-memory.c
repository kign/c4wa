#ifdef C4WA

void printf();
/*
extern char * malloc (int);
extern void free(char *);
extern void mm_stat(int*, int*, int*, int*, int*);
extern void mm_init(int, int);
*/
#define NULL 0

#else

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

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

void mm_init(int mm_min) {
    if (__mm_min)
        abort ();

    __mm_min = mm_min;

#if !defined(C4WA) && EMULATE_LINEAR_MEMORY
    __builtin_memory = malloc(LM_PAGE * __memsize * EMULATE_LINEAR_MEMORY);
#endif

    __mm_extra_offset = 10*sizeof(int);
    __mm_memory =  __builtin_memory + __builtin_offset + __mm_extra_offset;
    __mm_avail = (int *)(__builtin_memory + __builtin_offset);
    for (int i = 0; i <= 6; i ++)
        __mm_avail[i] = -1;
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

    if (verbose)
        printf("malloc(%d)\n", size);

    if (size > 64 * __mm_min) {
        // big block
        n = 1 + (size - (64 * __mm_min + 8))/(64 * __mm_min + 12);
        if (verbose)
            printf("Allocating %d big blocks\n", n);

        idx = 0;
        do {
            if (idx >= __mm_inuse) break;
            state = *(int *)(__mm_memory + idx * unit);
            if (state == 0) {
                for (j = idx + 1; j < __mm_inuse && j < i + n && *(int *)(__mm_memory + j * unit) == 0; j ++);
                if (j - i >= n) {
                    if (verbose)
                        printf("Found free stretch from %d to %d\n", idx, j - 1);
                    *(int *)(__mm_memory + idx * unit) = -n;
                    return __mm_memory + idx * unit;
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
        if (verbose)
            printf("Need %d more units\n", need);
        assert (need > 0);
        assert (need <= n);
        required = (__builtin_offset + __mm_extra_offset + (__mm_inuse + need) * unit)/LM_PAGE + 1;
        if (required > memsize()) {
            if (verbose)
                printf("Need %d more page(s) in addition to currently allocated %d\n", required - memsize(), memsize());
            memgrow(required - memsize());
        }
        for (i = 0; i < need; i ++)
            *(int *)(__mm_memory + (i + __mm_inuse) * unit) = 0;
        if (verbose)
            printf("Bumping __mm_inuse from %d to %d\n", __mm_inuse, __mm_inuse + need);
        __mm_inuse += need;
        if (verbose)
            printf("Following expansion, returning stretch from %d to %d\n", idx, idx + n - 1);
        *(int *)(__mm_memory + idx * unit) = -n;
        return __mm_memory + idx * unit;
    }
    else {
        // small block
        int a_size = __mm_min;
        for(n = 0; a_size < size; n ++, a_size *= 2)
        assert(n <= 6);
        idx = __mm_avail[n];
        if (verbose)
            printf("Actual size %d, n = %d, idx = %d\n", a_size, n, idx);
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
                    if (verbose)
                        printf("Need %d more page(s) in addition to currently allocated %d\n", required - memsize(), memsize());
                    memgrow(required - memsize());
                }
                __mm_inuse ++;
                if (verbose)
                    printf("Bumping __mm_inuse to %d\n", __mm_inuse);
                *(int *)(__mm_memory + idx * unit) = 0;
            }
            if (verbose)
                printf("Found available space at unit %d [%s]\n", idx,
                    *(int *)(__mm_memory + idx * unit) == 0? "not initialized" : "initialized");
            if (verbose)
                printf("Setting __mm_avail[%d] = %d\n", n, idx);
            __mm_avail[n] = idx;
        }

        state = *(int *)(__mm_memory + idx * unit);
        assert(state == 0 || state == n + 1);
        if (state == 0) {
            *(int *)(__mm_memory + idx * unit) = n + 1;
            // Setting last ("trailing") 2^(6-n) bits to 1 (= "available", rest to 0)
            int bits = 1 << (6-n);
            *(unsigned long *)(__mm_memory + idx * unit + 4) = (bits == 64? (unsigned long)0 : ((unsigned long)1 << (unsigned long)bits)) - 1;
            if (verbose)
                printf("Initializing unit %d to n = %d (actual size = %d) | %lx\n", idx, n, a_size, *(unsigned long *)(__mm_memory + idx * unit + 4));
        }
        unsigned long * cur = (unsigned long *)(__mm_memory + idx * unit + 4);
        assert(*cur != 0);
        j = __builtin_ctzl(*cur);

        if (verbose)
            printf("Using internal index %d\n", j);
        *cur ^= (unsigned long)1 << (unsigned long)j;
        if (*cur == 0) {
            if (verbose)
                printf("Unit at index %d is exhausted, setting __mm_avail[%d] = -1\n", idx, n);
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
        if (verbose)
            printf("free::large[%d]<%d units>\n", idx, -state);
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

        if (verbose)
            printf("free::small[%d](<size %d (n = %d), j = %d>\n", idx, a_size, n, j);

        assert (j >= 0 && j < bits);
        assert (address == (char *)cur + 8 + j * a_size);
        assert((*cur & (unsigned long)1 << (unsigned long)j) == 0);
        *cur ^= (unsigned long)1 << (unsigned long)j;
        if (__mm_avail[n] < 0) {
            if (verbose)
                printf("Setting __mm_avail[%d] = %d\n", n, idx);
            __mm_avail[n] = idx;
        }
        else {
            if (verbose)
                printf("Not changing __mm_avail[%d] == %d\n", n, __mm_avail[n]);
        }

        assert (1 <= __builtin_popcountl(*cur));
        assert (__builtin_popcountl(*cur) <= bits);
        if (__mm_avail[n] != idx && __builtin_popcountl(*cur) == bits) {
            if (verbose)
                printf("__mm_avail[%d] = %d != %d; *cur = %lx; seems like we can reset state from %d to 0\n",
                        n, __mm_avail[n], idx, *cur, state);
           *(int *)(__mm_memory + idx * unit) = 0;
        }
    }
}

void mm_print_units() {
    const int unit = 64 * __mm_min + 12;

    for (int idx = 0; idx < __mm_inuse; idx ++) {
        int state = *(int *)(__mm_memory + idx * unit);
        if (state == 0)
            printf("A");
        else if (state < 0) {
            printf("B<%d>\n", -state);
            idx += (-state - 1);
        }
        else
            printf("S<%d|free=%d>", state - 1, __builtin_popcountl(*(unsigned long *)(__mm_memory + idx * unit + 4)));
    }
    printf("\n");
}

#endif // defined(C4WA) || EMULATE_LINEAR_MEMORY

/* ----------------------- MEMORY MANAGER END ----------------------- */

struct Unit {
    int id;
    int integrity;
    char data[100];
};

#define N_UNITS 1000
static struct Unit ** storage = NULL;

static unsigned int seed = 57;
double mulberry32() {
    seed += (unsigned int) 1831565813;
    unsigned int t = seed;
    t = (t ^ t >> (unsigned int)15) * (t | (unsigned int)1);
    t ^= t + (t ^ t >> (unsigned int)7) * (t | (unsigned int)61);
    return (double)(t ^ t >> (unsigned int)14) / 4294967296.0;
}

struct Unit * allocate(int id) {
    struct Unit * unit = (struct Unit *) mm_malloc(sizeof(struct Unit));
    memset((char *) unit, '\0', sizeof(struct Unit));
    unit->id = id;
    unit->integrity = id ^ 816191;

    return unit;
}

void verify(struct Unit * unit) {
    assert(unit->integrity == (unit->id ^ 816191));
}

extern int main () {
    storage = (struct Unit **) mm_malloc(N_UNITS * 8 /*sizeof(struct Unit *)*/);
    memset((char *) storage, '\0', N_UNITS * sizeof(struct Unit *));

    const int n_iter = 100000;

    printf("Starting memory test with %d empty \"unit\" pointers and %d iterations\n", N_UNITS, n_iter);

    for (int iter = 0; iter < n_iter; iter ++) {
        int idx = (int)(mulberry32() * (double)N_UNITS);

        if (storage[idx]) {
            verify(storage[idx]);
//            printf("Releasing index %d, id %d\n", idx, storage[idx]->id);
            mm_free((char *)storage[idx]);
            storage[idx] = (struct Unit *) NULL;
        }
        else {
            storage[idx] = allocate(1 + iter);
//            printf("Allocating index %d, id %d\n", idx, 1 + iter);
        }
    }

    mm_print_units();
    mm_free((char *)storage);
    printf("Finished fixed memory test\n");

    return 0;
}
// Starting memory test with 1000 empty "unit" pointers and 100000 iterations
// S<6|free=0>S<0|free=5>S<0|free=2>S<0|free=4>S<0|free=14>S<0|free=23>S<0|free=19>S<0|free=8>S<0|free=5>S<0|free=4>
// Finished fixed memory test
