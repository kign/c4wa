/* -------------------------------------------------- *\
|*             COMMON COMPATIBILITY LAYER             *|
\* -------------------------------------------------- */

// value > 0: we emulate WASM linear memory with fixed maximum # of pages equal to this value
// value = 0: we use malloc() and free()
// we make this value visible to C4WA mode to keep statistics consistent
#define EMULATE_LINEAR_MEMORY 100

#ifdef C4WA
void printf(char *, ...);
#else
#define max(a,b) ((a) < (b))?(b):(a)
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#endif


/* -------------------------------------------------- *\
|*               COMMON DEFINITIONS                   *|
\* -------------------------------------------------- */

#define assert(x) if(!(x)) { printf("‼️ ASSERTION: \"" #x "\" @ line %d\n", __LINE__); abort (); }

// void get_cells_cb(char * /* ptr */, int /* width */, int /* height */);

#define N 5
#define N0 10

struct Box0 {
    int level;
    int x0, y0, size;
    char cells0[2 * N0 * N0];
};

struct Box {
    int level;
    int x0, y0, size;
    struct Box * cells[N * N];
};

static struct Box * world = (struct Box *) 0;

/* -------------------------------------------------- *\
|*                 MEMORY MANAGER                     *|
\* -------------------------------------------------- */
#define USE_PRINTF
#if defined(C4WA) || EMULATE_LINEAR_MEMORY

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
#endif // defined(C4WA) || EMULATE_LINEAR_MEMORY

#if !defined(C4WA) && !EMULATE_LINEAR_MEMORY
#define mm_init(offset, size)
#endif // !defined(C4WA) && !EMULATE_LINEAR_MEMORY

/* -------------------------------------------------- *\
|*              ALLOCATION WRAPPERS                   *|
\* -------------------------------------------------- */

static int mm_allocated = 0;
static int mm_freed = 0;

struct Box * alloc_new_box (int level, int x0, int y0) {
    mm_allocated ++;
    struct Box * box = (struct Box *) mm_malloc(level == 0? sizeof(struct Box0): sizeof(struct Box));
    memset((char *)box, '\0', level == 0? sizeof(struct Box0): sizeof(struct Box));

    int size = N0;
    for (int k = 0; k < level; k ++)
        size *= N;

    box->level = level;
    box->x0 = x0;
    box->y0 = y0;
    box->size = size;

    return box;
}

#define alloc_new_box0(x0, y0) (struct Box0 *) alloc_new_box(0, x0, y0)

void release_box(struct Box * box) {
    mm_freed ++;

    if (box->level > 0) {
        for (int y = 0; y < N; y ++)
            for (int x = 0; x < N; x ++)
                if (box->cells[y * N + x])
                    release_box(box->cells[y * N + x]);
    }

    mm_free((char *) box);
}

void memory_stat() {
#if EMULATE_LINEAR_MEMORY
    int allocated, freed, current, in_use, capacity;

    mm_stat(&allocated, &freed, &current, &in_use, &capacity);
    printf("A/R/C: %d/%d/%d; CAP: %d/%d\n", allocated, freed, current, in_use, capacity);
#else
    printf("A/R\n", mm_allocated, mm_freed);
#endif
}

/* -------------------------------------------------- *\
|*                 INFINITE BOARD                     *|
\* -------------------------------------------------- */

void verify(struct Box * w) {
    if (w->level == 0)
        return;

    for (int y = 0; y < N; y ++)
        for (int x = 0; x < N; x ++) {
            struct Box * g = w->cells[y * N + x];
            if (g) {
                assert(g->level == w->level - 1);
                assert(g->size == w->size/N);
                assert(g->x0 == w->x0 + g->size * x);
                assert(g->y0 == w->y0 + g->size * y);

                verify(g);
            }
        }
}

void set_cell(int x, int y, int val) {
    struct Box * w;
    int t, xp, yp;
    const int verbose = 0;

    if (verbose)
        printf("set_cell(%d, %d, %d)\n", x, y, val);

    if (!world) {
        if (!val) return;
        struct Box0 * box = alloc_new_box0(x - N0/2, y - N0/2);
        box->cells0[N0 * (y - box->y0) + (x - box->x0)] = '\1';

        world = (struct Box *) box;
        return;
    }

    int size = world->size;
    if (!(world->x0 <= x && x < world->x0 + size && world->y0 <= y && y < world->y0 + size)) {
        if (!val) return;
        if (verbose)
            printf("Point %d, %d is outside the world %d, %d, %d, %d\n", x, y,
                    world->x0, world->x0 + size-1, world->y0, world->y0 + size-1);

        int xmin = world->x0;
        if (x < xmin) {
            t = xmin - x;
            if (t % size != 0) t += (size - t % size);
            xmin -= t;
        }
        int ymin = world->y0;
        if (y < ymin) {
            t = ymin - y;
            if (t % size != 0) t += (size - t % size);
            ymin -= t;
        }
        int xmax = world->x0 + size;
        if (x + 1 > xmax) {
            t = x + 1 - xmax;
            if (t % size != 0) t += (size - t % size);
            xmax += t;
        }
        int ymax = world->y0 + size;
        if (y + 1 > ymax) {
            t = y + 1 - ymax;
            if (t % size != 0) t += (size - t % size);
            ymax += t;
        }

        if (verbose)
            printf("xmin = %d, xmax = %d, ymin = %d, ymax = %d\n", xmin, xmax, ymin, ymax);

        int new_level = world->level;
        int new_size = size;
        do {
            new_size *= N;
            new_level ++;
        }
        while (new_size < xmax - xmin || new_size < ymax - ymin);

        if (verbose)
            printf("Level: %d => %d, Size: %d => %d\n", world->level, new_level, size, new_size);

        int dx = (new_size - xmax + xmin)/2;
        if (dx % size != 0) dx += (size - dx % size);
        int dy = (new_size - ymax + ymin)/2;
        if (dy % size != 0) dy += (size - dy % size);

        struct Box * new_world = alloc_new_box(new_level, xmin - dx, ymin - dy);

        w = new_world;
        int wsize = new_size/N;
        do {
            xp = (world->x0 - w->x0)/wsize;
            yp = (world->y0 - w->y0)/wsize;
            if (wsize == world->size) {
                if (verbose)
                    printf("Converged to original box @ %d, %d (size %d)\n", w->x0 + xp*wsize, w->y0 + yp*wsize, wsize);
                assert(w->x0 + xp*wsize == world->x0);
                assert(w->y0 + yp*wsize == world->y0);
                w->cells[N * yp + xp] = world;
                break;
            }
            new_level --;
            if (verbose)
                printf("w->x0 = %d, xp = %d, wsize = %d, w->y0 = %d, yp = %d\n", w->x0, xp, wsize, w->y0, yp);

            w->cells[N * yp + xp] = alloc_new_box(new_level, w->x0 + xp*wsize, w->y0 + yp*wsize);
            w = w->cells[N * yp + xp];
            wsize /= N;
        }
        while(1);
        world = new_world;
    }

    w = world;
    do {
        if (verbose)
            printf("Entering (%d,%d) into <%d,%d,%d,%d>\n", x, y, w->level, w->x0, w->y0, w->size);
        assert (w->x0 <= x && x < w->x0 + w->size && w->y0 <= y && y < w->y0 + w->size);
        if (w->level == 0) {
            xp = x - w->x0;
            yp = y - w->y0;
            if (verbose)
                printf("Assigned <%d,%d,%d>[%d] = %d\n", w->level, w->x0, w->y0, yp * N0 + xp, val);
            ((struct Box0 *)w)->cells0[yp * N0 + xp] = (char) val;
            break;
        }
        else {
            size = w->size/N;
            xp = (x - w->x0)/size;
            yp = (y - w->y0)/size;
            if (verbose)
                printf("Going down to level %d, xp = %d, yp = %d\n", w->level - 1, xp, yp);
            if (!w->cells[yp * N + xp]) {
                if (!val) return;
                w->cells[yp * N + xp] = alloc_new_box(w->level - 1, w->x0 + xp*size, w->y0 + yp*size);
            }
            int t1 = w->level - 1;
            int t2 = w->x0 + xp*size;
            int t3 = w->y0 + yp*size;
            w = w->cells[yp * N + xp];
            assert(t1 == w->level);
            assert(t2 == w->x0);
            assert(t3 == w->y0);
            assert(size == w->size);
//            if (verbose)
//                printf("Expected cell <%d,%d,%d,%d>, found/created <%d,%d,%d,%d>\n",
//                        t1, t2, t3, size,
//                        w->level, w->x0, w->y0, w->size);
        }
    }
    while(1);
    if (verbose)
        printf("Done!\n");
}

int get_cell(int x, int y) {
    const int verbose = 0;
    int xp, yp, size;

    if (!world)
        return 0;

    struct Box * w = world;
    if (!(w->x0 <= x && x < w->x0 + w->size && w->y0 <= y && y < w->y0 + w->size))
        return 0;

    do {
        if (verbose)
            printf("Trying to locate (%d,%d) in <%d,%d,%d,%d>\n", x, y, w->level, w->x0, w->y0, w->size);
        assert (w->x0 <= x && x < w->x0 + w->size && w->y0 <= y && y < w->y0 + w->size);
        if (w->level == 0) {
            xp = x - w->x0;
            yp = y - w->y0;
            if (verbose)
                printf("Got to the bottom <%d,%d,%d>[%d]\n", w->level, w->x0, w->y0, yp * N0 + xp);
            return (int)((struct Box0 *)w)->cells0[yp * N0 + xp];
        }
        else {
            size = w->size/N;
            xp = (x - w->x0)/size;
            yp = (y - w->y0)/size;
            if (verbose)
                printf("Going down to level %d, xp = %d, yp = %d\n", w->level - 1, xp, yp);
            w = w->cells[yp * N + xp];
        }
    }
    while(w);
    return 0;
}

/* -------------------------------------------------- *\
|*                     TESTING                        *|
\* -------------------------------------------------- */

void dump_cells0(struct Box0 * w, int verbose) {
    if (verbose)
        printf ("[<%d,%d,%d>", w->level, w->x0, w->y0);
    for (int y = 0; y < N0; y ++)
        for (int x = 0; x < N0; x ++)
            if (w->cells0[y * N0 + x]) {
                printf(" (%d,%d)", w->x0 + x, w->y0 + y);
            }
    if (verbose)
        printf ("]");
}

void dump_cells(struct Box * w, int verbose) {
    if (verbose)
        printf ("[<%d,%d,%d>", w->level, w->x0, w->y0);
    for (int y = 0; y < N; y ++)
        for (int x = 0; x < N; x ++)
            if (w->cells[y * N + x]) {
                if (w->level == 1)
                    dump_cells0((struct Box0 *) w->cells[y * N + x], verbose);
                else
                    dump_cells(w->cells[y * N + x], verbose);
            }
            else if (verbose)
                printf (".");
    if (verbose)
        printf ("]");
}

void dump_all(int verbose) {
    if (world->level == 0)
        dump_cells0((struct Box0 *) world, verbose);
    else
        dump_cells(world, verbose);
    printf("\n");
}

void to_arr0(struct Box0 * w, int n, int pts[], int *k) {
    for (int y = 0; y < N0; y ++)
        for (int x = 0; x < N0; x ++)
            if (w->cells0[y * N0 + x]) {
                assert(*k < n);
                pts[*k * 2]     = w->x0 + x;
                pts[*k * 2 + 1] = w->y0 + y;
                *k = *k + 1;
            }
}

void to_arr1(struct Box * w, int n, int pts[], int *k) {
    for (int y = 0; y < N; y ++)
        for (int x = 0; x < N; x ++)
            if (w->cells[y * N + x]) {
                if (w->level == 1)
                    to_arr0((struct Box0 *) w->cells[y * N + x], n, pts, k);
                else
                    to_arr1(w->cells[y * N + x], n, pts, k);
            }
}

void to_arr(int n, int pts[]) {
    int k = 0;
    if (world->level == 0)
        to_arr0((struct Box0 *) world, n, pts, &k);
    else
        to_arr1(world, n, pts, &k);
}

static unsigned int seed = 57;

double mulberry32() {
    seed += (unsigned int) 1831565813;
    unsigned int t = seed;
    t = (t ^ t >> (unsigned int)15) * (t | (unsigned int)1);
    t ^= t + (t ^ t >> (unsigned int)7) * (t | (unsigned int)61);
    return (double)(t ^ t >> (unsigned int)14) / 4294967296.0;
}

int rand_int() {
    return (int)(10000.0 * (mulberry32() - 0.5));
}

void sort_points(int n, int pts[]) {
    int cnt, i;
    do {
        for (i = 0, cnt = 0; i < n - 1; i ++) {
            if (pts[2*i] < pts[2*i + 2] || (pts[2*i] == pts[2*i + 2] && pts[2*i+1] < pts[2*i + 3]))
                continue;
            int t = pts[2*i];
            pts[2*i] = pts[2*i + 2];
            pts[2*i + 2] = t;
            t = pts[2*i + 1];
            pts[2*i + 1] = pts[2*i + 3];
            pts[2*i + 3] = t;

            cnt ++;
        }
    }
    while(cnt > 0);
}

void test_1 () {
    int i;
    const int n_pts = 20;
    int pts[2 * n_pts];

    for(i = 0; i < 2 * n_pts; i ++)
        pts[i] = rand_int ();

    for(i = 0; i < n_pts; i ++)
        set_cell(pts[2 * i], pts[2 * i + 1], 1);

    verify(world);

    printf("Raw In    :");
    for(i = 0; i < n_pts; i ++)
        printf(" (%d,%d)", pts[2 * i], pts[2 * i + 1]);

    sort_points(n_pts, pts);

    printf("\nSorted In :");
    for(i = 0; i < n_pts; i ++)
        printf(" (%d,%d)", pts[2 * i], pts[2 * i + 1]);

    to_arr(n_pts, pts);
    sort_points(n_pts, pts);

    printf("\nSorted Out:");

    for(i = 0; i < n_pts; i ++)
        printf(" (%d,%d)", pts[2 * i], pts[2 * i + 1]);

    printf("\nRaw Out   :");
    dump_all (0);
}

void test_2 () {
    int x, y;
    const unsigned int original_seed = seed;
    const double density = 0.4;
    const int X = 1200;
    const int Y = 1600;

    for (y = 0; y < Y; y ++)
        for(x = 0; x < X; x ++)
            set_cell(x, y, mulberry32() < density);

    seed = original_seed;

    for (y = 0; y < Y; y ++)
        for(x = 0; x < X; x ++)
            assert(get_cell(x,y) == mulberry32() < density);

    printf("OK\n");
}

extern int main () {
    mm_init(0, max(sizeof(struct Box0), sizeof(struct Box)));

    for (int test = 1; test <= 2; test ++) {
        printf("🧪 Test %d\n", test);
        if (test == 1)
            test_1 ();
        else
            test_2 ();
        verify(world);
        if (test > 0) {
            release_box(world);
            world = 0;
        }
        memory_stat ();
    }
    printf("Done!\n");

    return 0;
}
// 🧪 Test 1
// Raw In    : (2371,-2971) (-3610,1301) (-332,1053) (-2607,153) (-26,-529) (470,1330) (-1333,-3258) (3181,2599) (-1572,-3553) (1823,3825) (4886,2280) (-99,-4440) (-688,1954) (-2862,1293) (-3032,-3204) (866,3016) (-4715,-3007) (1026,-543) (-2933,1462) (-678,-839)
// Sorted In : (-4715,-3007) (-3610,1301) (-3032,-3204) (-2933,1462) (-2862,1293) (-2607,153) (-1572,-3553) (-1333,-3258) (-688,1954) (-678,-839) (-332,1053) (-99,-4440) (-26,-529) (470,1330) (866,3016) (1026,-543) (1823,3825) (2371,-2971) (3181,2599) (4886,2280)
// Sorted Out: (-4715,-3007) (-3610,1301) (-3032,-3204) (-2933,1462) (-2862,1293) (-2607,153) (-1572,-3553) (-1333,-3258) (-688,1954) (-678,-839) (-332,1053) (-99,-4440) (-26,-529) (470,1330) (866,3016) (1026,-543) (1823,3825) (2371,-2971) (3181,2599) (4886,2280)
// Raw Out   : (-99,-4440) (-4715,-3007) (-3032,-3204) (-1572,-3553) (-1333,-3258) (2371,-2971) (-678,-839) (-26,-529) (1026,-543) (-2607,153) (-3610,1301) (-2862,1293) (-2933,1462) (-332,1053) (-688,1954) (470,1330) (4886,2280) (866,3016) (1823,3825) (3181,2599)
// A/R/C: 81/81/0; CAP: 2/10
// 🧪 Test 2
// OK
// A/R/C: 20434/20434/0; CAP: 319/320
// Done!
