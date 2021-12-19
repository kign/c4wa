#ifdef C4WA

void printf(char *, ...);
extern char * malloc (int);
extern void free(char *);
extern void mm_stat(int*, int*, int*, int*, int*);
extern void mm_init(int, int);
#define NULL 0

#else

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#endif

#define assert(x) if(!(x)) { printf("‼️ ASSERTION: \"" #x "\" @ line %d\n", __LINE__); abort (); }

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
    struct Unit * unit = (struct Unit *) malloc(sizeof(struct Unit));
    memset((char *) unit, '\0', sizeof(struct Unit));
    unit->id = id;
    unit->integrity = id ^ 816191;

    return unit;
}

void verify(struct Unit * unit) {
    assert(unit->integrity == (unit->id ^ 816191));
}

extern int main () {
#ifdef C4WA
    storage = (struct Unit **) (__builtin_memory + __builtin_offset);
    mm_init(N_UNITS * sizeof(struct Unit *), sizeof(struct Unit));
#else
    storage = malloc(N_UNITS * sizeof(struct Unit *));
#endif

    memset((char *) storage, '\0', N_UNITS * sizeof(struct Unit *));

    const int n_iter = 100000;

    printf("Starting memory test with %d empty \"unit\" pointers and %d iterations\n", N_UNITS, n_iter);

    for (int iter = 0; iter < n_iter; iter ++) {
        int idx = (int)(mulberry32() * (double)N_UNITS);

        if (storage[idx]) {
            verify(storage[idx]);
//            printf("Releasing index %d, id %d\n", idx, storage[idx]->id);
            free((char *)storage[idx]);
            storage[idx] = (struct Unit *) NULL;
        }
        else {
            storage[idx] = allocate(1 + iter);
//            printf("Allocating index %d, id %d\n", idx, 1 + iter);
        }
    }

    printf("Finished fixed memory test\n");

#ifdef C4WA
    int allocated, freed, current, in_use, capacity;

    mm_stat(&allocated, &freed, &current, &in_use, &capacity);
    printf("A/R/C: %d/%d/%d; CAP: %d/%d\n", allocated, freed, current, in_use, capacity);
#else
    printf("A/R/C: 50246/49754/492; CAP: 9/10\n");
#endif

    return 0;
}
// Starting memory test with 1000 empty "unit" pointers and 100000 iterations
// Finished fixed memory test
// A/R/C: 50246/49754/492; CAP: 9/10
