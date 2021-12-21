#ifdef C4WA

void printf(char *, ...);

extern void * malloc (int size);
extern void free(void * address);
extern int * mm_histogram(int *);
extern char * mm_print_units();
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
    char * data = malloc(size);
    memset(data, '\0', size);
    *(int *)data = id;
    *(int *)(data + sizeof(int)) = id ^ 816191;

    return data;
}

#ifdef C4WA
void print_histogram() {
    const int s0 = 128;
    int lim = s0/2;
    int i, j, hsize;
    int * histogram = mm_histogram(&hsize);

    for(j = hsize - 1; j >= 1 && !histogram[j]; j --);

    for(i = 0; i <= j; i ++) {
        lim = i > 6? (64 * s0 + 12) * (i - 7) + 64 * s0 + 8 : 2 * lim;
        if (i < hsize-1)
            printf("%6d", lim);
        else
            printf(" unlim");
        if (histogram[i])
            printf(" %d\n", histogram[i]);
        else
            printf(" -\n");
    }
}
#endif

void verify_data(char * data) {
    int id = *(int *)data;
    int integrity = *(int *)(data + sizeof(int));

    assert(integrity == (id ^ 816191));
}

void test_uniform(int id, int n_units, int n_iter, int size) {
    char ** storage = (char **) malloc(n_units * 8);
    memset(storage, '\0', n_units * 8);

    printf("Starting memory test with %d empty \"unit\" pointers and %d iterations\n", n_units, n_iter);

    for (int iter = 0; iter < n_iter; iter ++) {
        int idx = (int)(mulberry32() * (double)n_units);

        if (storage[idx]) {
            verify_data(storage[idx]);
            free(storage[idx]);
            storage[idx] = NULL;
        }
        else {
            storage[idx] = allocate_data(1 + iter, size);
        }
    }

#ifdef C4WA
    char * units = mm_print_units();
    printf("%s\n", units);
    free(units);
#else
    printf("%s\n", id == 1? "S<6|free=0>S<0|free=5>S<0|free=2>S<0|free=4>S<0|free=14>S<0|free=23>S<0|free=19>S<0|free=8>S<0|free=5>S<0|free=4>S<3|free=7>"
                          : "S<6|free=1>S<0|free=5>S<0|free=2>S<0|free=4>S<0|free=14>S<0|free=23>S<0|free=19>S<0|free=7>S<0|free=5>S<0|free=4>S<3|free=7>B<2>B<2>B<2>B<2>B<2>..B<2>....");
#endif
    free(storage);
    printf("Finished fixed memory test\n");
}

void test_nonuniform(int id, int n_units, int n_iter, int size) {
    char ** storage = (char **) malloc(n_units * 8);
    memset(storage, '\0', n_units * 8);

    printf("Starting memory test with %d empty \"unit\" pointers and %d iterations\n", n_units, n_iter);

    for (int iter = 0; iter < n_iter; iter ++) {
        int idx = (int)(mulberry32() * (double)n_units);

        if (storage[idx]) {
            verify_data(storage[idx]);
            free(storage[idx]);
            storage[idx] = NULL;
        }
        else {
            double r = mulberry32();
            storage[idx] = allocate_data(1 + iter, (int)((double)(size - 8) * r * r * r * r) + 8);
        }
    }

#ifdef C4WA
    char * units = mm_print_units();
    printf("%s\n", units);
    free(units);
#else
    printf("%s\n", "S<0|free=53>S<0|free=5>S<0|free=2>S<0|free=4>S<0|free=14>S<0|free=23>S<0|free=19>S<0|free=8>S<0|free=5>S<0|free=4>S<3|free=3>B<2>B<2>B<2>B<2>B<2>B<2>B<2>S<5|free=1>S<6|free=0>S<4|free=2>S<6|free=1>S<5|free=0>S<6|free=0>B<2>B<2>.B<4>.S<1|free=32>B<2>B<3>B<7>B<4>B<5>.B<3>..B<2>B<5>B<5>...B<4>B<12>....B<6>...........B<7>B<3>B<7>...S<2|free=15>B<10>B<9>.................B<10>.............................B<11>B<10>............................................................");
#endif
    free(storage);
    printf("Finished variable memory test\n");
}

extern int main () {
    test_uniform(1, 1000, 100000, 108);
    test_uniform(2, 10, 1000, 10000);
    test_nonuniform(3, 100, 10000, 100000);
#ifdef C4WA
    print_histogram ();
#else
    printf(
"   128 51172\n"
"   256 182\n"
"   512 228\n"
"  1024 255\n"
"  2048 302\n"
"  4096 327\n"
"  8192 405\n"
"  8200 -\n"
" 16404 1041\n"
" 24608 320\n"
" 32812 299\n"
" 41016 202\n"
" 49220 192\n"
" 57424 159\n"
" 65628 153\n"
" 73832 160\n"
" 82036 123\n"
" 90240 128\n"
" 98444 115\n"
" unlim 15\n"
);
#endif
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
