#ifdef C4WA
void printf();
#else
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define C4WA_STACK_SIZE 1024
#define C4WA_DATA_SIZE 1024

static void * __linear_memory;
#define alloc(addr, size, type) (type *)(__linear_memory + C4WA_STACK_SIZE  + C4WA_DATA_SIZE + addr)

#endif

#define assert(x) if(!(x)) { printf("‼️ ASSERTION: \"" #x "\" @ line %d\n", __LINE__); abort (); }

// void get_cells_cb(char * /* ptr */, int /* width */, int /* height */);

#define N 5
#define N0 10

struct Box0 {
    int level;
    char top, bottom, left, right;
    int x0, y0, size;
    char cells[N0 * N0];
};

struct Box {
    int level;
    char top, bottom, left, right;
    int x0, y0, size;
    struct Box * cells[N * N];
};

static struct Box * world = (struct Box *) 0;

#define CAP 1000
static unsigned long * mm_map = (unsigned long *) 0;
static int mm_size = -1;
static int mm_long = (CAP + 63)/64;
static int mm_start = (CAP + 63)/64 * 8;

void init_memory_manager() {
    mm_map = alloc(0, mm_long, unsigned long);
    memset((char *) mm_map, '\xFF', 8 * mm_long);
    mm_size = sizeof(struct Box0) > sizeof(struct Box)? sizeof(struct Box0) : sizeof(struct Box);

#if 0
    printf("Memory initialized: Box0 = %d, Box = %d, mm_size = %d\n", sizeof(struct Box0), sizeof(struct Box), mm_size);
#endif
    printf("Memory initialized: mm_long = %d, mm_start = %d\n", mm_long, mm_start);
}

struct Box * new_box (int level, int x0, int y0) {
    const int verbose = 0;
    int i;
    for (i = 0; i < mm_long && !mm_map[i]; i ++);

    if (i == mm_long) {
        printf("Out of MAP memory (CAP = %d)\n", CAP);
        abort ();
    }

    assert (mm_map[i] != 0);

    int j = (int) __builtin_ctzl(mm_map[i]);
//    printf("mm_map[%d] = %lx\n", i, mm_map[i]);

    int idx = 64*i + j;

    int size = N0;
    for (int k = 0; k < level; k ++)
        size *= N;

    if (verbose)
        printf("Allocating Box<%d>: %d = [%d, %d] | x = (%d,%d), y = (%d,%d)\n", level, idx, i, j,
                x0, x0 + size - 1, y0, y0 + size - 1);

    mm_map[i] ^= (unsigned long)1 << (unsigned long)j;
//    printf("[after ^= %lx] mm_map[%d] = %lx\n", (unsigned long)1 << (unsigned long) j, i, mm_map[i]);


    struct Box * box = alloc(mm_start + idx * mm_size, 1, struct Box);
    memset((char *) box, '\0', mm_size);
    box->level = level;
    box->x0 = x0;
    box->y0 = y0;
    box->size = size;

    return box;
}

// this will be "extern" for now to force inclusion in WAT
extern void free_box(struct Box * box) {
    // this is not yet fully implemented
    int idx = ((char *) box - (char *)(C4WA_STACK_SIZE + C4WA_DATA_SIZE))/mm_size;
    int i = idx / 64;
    int j = idx % 64;

    printf("Freeing Box: %d = [%d, %d]\n", idx, i, j);

    mm_map[i] &= ~(1 << (unsigned long) j);
}

#define new_box0(x0, y0) (struct Box0 *) new_box(0, x0, y0)
#define free_box0(x) free_box((struct Box *)(x))

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
        init_memory_manager ();
        struct Box0 * box = new_box0(x - N0/2, y - N0/2);
        box->cells[N0 * (y - box->y0) + (x - box->x0)] = '\1';

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

        struct Box * new_world = new_box(new_level, xmin - dx, ymin - dy);

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

            w->cells[N * yp + xp] = new_box(new_level, w->x0 + xp*wsize, w->y0 + yp*wsize);
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
            ((struct Box0 *)w)->cells[yp * N0 + xp] = (char) val;
            break;
        }
        else {
            if (!val) return;
            size = w->size/N;
            xp = (x - w->x0)/size;
            yp = (y - w->y0)/size;
            if (verbose)
                printf("Going down to level %d, xp = %d, yp = %d\n", w->level - 1, xp, yp);
            if (!w->cells[yp * N + xp])
                w->cells[yp * N + xp] = new_box(w->level - 1, w->x0 + xp*size, w->y0 + yp*size);
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

void dump_cells0(struct Box0 * w, int verbose) {
    if (verbose)
        printf ("[<%d,%d,%d>", w->level, w->x0, w->y0);
    for (int y = 0; y < N0; y ++)
        for (int x = 0; x < N0; x ++)
            if (w->cells[y * N0 + x]) {
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
            if (w->cells[y * N0 + x]) {
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

extern int main () {

#ifndef C4WA
    __linear_memory = malloc(64000);
#endif

/*
    set_cell(10, 20, 1);
    set_cell(200, -411, 1);
    set_cell(4, 308, 1);
*/


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
    return 0;
}
// Memory initialized: mm_long = 16, mm_start = 128
// Raw In    : (2371,-2971) (-3610,1301) (-332,1053) (-2607,153) (-26,-529) (470,1330) (-1333,-3258) (3181,2599) (-1572,-3553) (1823,3825) (4886,2280) (-99,-4440) (-688,1954) (-2862,1293) (-3032,-3204) (866,3016) (-4715,-3007) (1026,-543) (-2933,1462) (-678,-839)
// Sorted In : (-4715,-3007) (-3610,1301) (-3032,-3204) (-2933,1462) (-2862,1293) (-2607,153) (-1572,-3553) (-1333,-3258) (-688,1954) (-678,-839) (-332,1053) (-99,-4440) (-26,-529) (470,1330) (866,3016) (1026,-543) (1823,3825) (2371,-2971) (3181,2599) (4886,2280)
// Sorted Out: (-4715,-3007) (-3610,1301) (-3032,-3204) (-2933,1462) (-2862,1293) (-2607,153) (-1572,-3553) (-1333,-3258) (-688,1954) (-678,-839) (-332,1053) (-99,-4440) (-26,-529) (470,1330) (866,3016) (1026,-543) (1823,3825) (2371,-2971) (3181,2599) (4886,2280)
// Raw Out   : (-99,-4440) (-4715,-3007) (-3032,-3204) (-1572,-3553) (-1333,-3258) (2371,-2971) (-678,-839) (-26,-529) (1026,-543) (-2607,153) (-3610,1301) (-2862,1293) (-2933,1462) (-332,1053) (-688,1954) (470,1330) (4886,2280) (866,3016) (1823,3825) (3181,2599)
