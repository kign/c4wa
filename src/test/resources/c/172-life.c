/* -------------------------------------------------- *\
|*             COMMON COMPATIBILITY LAYER             *|
\* -------------------------------------------------- */

#ifdef C4WA
void printf();
extern char * malloc (int);
extern void free(char *);
extern void mm_stat(int*, int*, int*, int*, int*);
extern void mm_init(int, int);
extern int strlen(char *);
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
#define N0 5

struct Box0 {
    int level;
    int x0, y0, size, age;
    char cells0[2 * N0 * N0];
};

struct Box {
    int level;
    int x0, y0, size, age;
    struct Box * cells[N * N];
};

static struct Box * world = (struct Box *) 0;

/* -------------------------------------------------- *\
|*              ALLOCATION WRAPPERS                   *|
\* -------------------------------------------------- */

static int mm_allocated = 0;
static int mm_freed = 0;

struct Box * alloc_new_box (int level, int x0, int y0) {
    mm_allocated ++;
    struct Box * box = (struct Box *) malloc(level == 0? sizeof(struct Box0): sizeof(struct Box));
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

    free((char *) box);
}

void memory_stat() {
#ifdef C4WA
    int allocated, freed, current, in_use, capacity;

    mm_stat(&allocated, &freed, &current, &in_use, &capacity);
    // printf("A/R/C: %d/%d/%d; CAP: %d/%d\n", allocated, freed, current, in_use, capacity);
#endif
    printf("A/R %d/%d\n", mm_allocated, mm_freed);
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

void set_cell(int x, int y, int val, int plane, int age) {
    struct Box * w;
    int t, xp, yp;
    const int verbose = 0;

    if (verbose)
        printf("set_cell(%d, %d, %d)\n", x, y, val);

    if (!world) {
        if (!val) return;
        struct Box0 * box = alloc_new_box0(x - N0/2, y - N0/2);
        box->cells0[N0 * N0 * plane + N0 * (y - box->y0) + (x - box->x0)] = (char)val;

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
        if (val) w->age = age;
        if (w->level == 0) {
            xp = x - w->x0;
            yp = y - w->y0;
            if (verbose)
                printf("Assigned <%d,%d,%d>[%d] = %d\n", w->level, w->x0, w->y0, yp * N0 + xp, val);
            ((struct Box0 *)w)->cells0[N0 * N0 * plane + N0 * yp + xp] = (char) val;
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

int get_cell(int x, int y, int plane) {
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
            return (int)((struct Box0 *)w)->cells0[N0 * N0 * plane + N0 * yp + xp];
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
|*                 LIFE (FINITE)                      *|
\* -------------------------------------------------- */
struct Stat {unsigned int hash; int count; };
const unsigned int rand_x = 179424673;
const unsigned int rand_y = 376424971;

void life_fin_prepare (char * cells, int X, int Y, struct Stat * stat) {
    int cnt = 0;
    unsigned int hash = 0;

    for(int y = 0; y < Y; y ++)
        for(int x = 0; x < X; x ++) {
            int idx = X * y + x;
            if (cells[idx] == 1) {
                cnt ++;
                hash ^= (unsigned int)x * rand_x + (unsigned int)y * rand_y;
                for (int dx = -1; dx <= 1; dx ++)
                    for (int dy = -1; dy <= 1; dy ++) {
                        int didx = X * ((y + dy + Y) % Y) + ((x + dx + X) % X);
                        if (cells[didx] == 0)
                            cells[didx] = 2;
                    }
            }
        }
    stat->count = cnt;
    stat->hash = hash;
}

void life_fin_step (char * cells, char * cellsnew, int X, int Y, struct Stat * stat) {
    int                  ind, x, y,  n, newv;
    int                  n00, n01, n02, n10, n12, n20, n21, n22;
    int                  v00, v01, v02, v10, v11, v12, v20, v21, v22;
    int                  cnt = 0;
    unsigned int         hash = 0;
    char                 * p = cells - 1;

    memset ( cellsnew, (char)0, X * Y );

    do {
        do {
            p ++;
        }
        while (*p == (char)0);

        if (*p == 3)
            break;

        //assert ( *p == 1 || *p == 2 );

        ind = p - cells;

        y = ind / X; x = ind - y * X;

        if ( x > 0 & x < X - 1 & y > 0 & y < Y - 1 ) {
            n00 = X * (y - 1) + (x - 1);
            n01 = n00 + 1;
            n02 = n01 + 1;
            n10 = ind - 1;
            n12 = ind + 1;
            n20 = n10 + X;
            n21 = n20 + 1;
            n22 = n21 + 1;
        }
        else {
#define POS(dy,dx)  (X * ((y + dy + Y) % Y) + ((x + dx + X) % X))
            n00 = POS(-1,-1);
            n01 = POS(-1,0);
            n02 = POS(-1,1);
            n10 = POS(0,-1);
            n12 = POS(0,1);
            n20 = POS(1,-1);
            n21 = POS(1,0);
            n22 = POS(1,1);
#undef POS
        }
        v00 = (1 == cells[n00]);
        v01 = (1 == cells[n01]);
        v02 = (1 == cells[n02]);
        v10 = (1 == cells[n10]);
        v11 = (1 == *p);
        v12 = (1 == cells[n12]);
        v20 = (1 == cells[n20]);
        v21 = (1 == cells[n21]);
        v22 = (1 == cells[n22]);

        n = v00 + v01 + v02 + v10 + v12 + v20 + v21 + v22;

        newv = (n == 3) | ((n == 2) & v11);

        if ( newv ) {
            cnt ++;
            hash ^= (unsigned int)x * rand_x + (unsigned int)y * rand_y;

            cellsnew[ind] = (char)newv;
            if (cellsnew[n00] != 1) cellsnew[n00] = 2;
            if (cellsnew[n01] != 1) cellsnew[n01] = 2;
            if (cellsnew[n02] != 1) cellsnew[n02] = 2;
            if (cellsnew[n10] != 1) cellsnew[n10] = 2;
            if (cellsnew[n12] != 1) cellsnew[n12] = 2;
            if (cellsnew[n20] != 1) cellsnew[n20] = 2;
            if (cellsnew[n21] != 1) cellsnew[n21] = 2;
            if (cellsnew[n22] != 1) cellsnew[n22] = 2;
        }
    }
    while(1);

    stat->count = cnt;
    stat->hash = hash;
}

void life_fin_print(char * pos, int X, int x0, int x1, int y0, int y1, int dbg) {
    for(int y = y0; y <= y1; y ++) {
        for(int x = x0; x <= x1; x ++) {
            int val = (int) pos[X * y + x];
            printf(val == 1? "X" : (((val == 2) & dbg)?"+":"."));
        }
        printf("\n");
    }
}

void life_fin_read(char * pos, int X, int Y, int x0, int y0, char * src, int sX, int sY) {
    memset(pos, '\0', X * Y);
    for(int y = 0; y < sY; y ++)
        for (int x = 0; x < sX; x ++)
            pos[(y + y0) * X + (x + x0)] = src[y * sX + x] == 'x' ? (char)1 : (char)0;
}

/* -------------------------------------------------- *\
|*                LIFE (INFINITE)                     *|
\* -------------------------------------------------- */

void life_prepare_box(struct Box * w, struct Stat * stat) {
    if (w->level > 0) {
        for(int idx = 0; idx < N*N; idx ++)
            if (w->cells[idx])
                life_prepare_box(w->cells[idx], stat);
    }
    else {
        unsigned int hash = 0;
        int cnt = 0;
        for(int idx = 0; idx < N0*N0; idx ++)
            if (((struct Box0 *)w)->cells0[idx] == 1) {
                int y = idx / N0;
                int x = idx % N0;

                cnt ++;
                hash ^= (unsigned int)(x + w->x0) * rand_x + (unsigned int)(y + w->y0) * rand_y;

                for (int j = 0; j < 9; j ++) {
                    if (j == 4) continue;
                    int vx = x + j % 3 - 1;
                    int vy = y + j / 3 - 1;
                    if (0 <= vx && vx < N0 && 0 <= vy && vy < N0) {
                        int ind = vy * N0 + vx;
                        if (0 == ((struct Box0 *)w)->cells0[ind])
                            ((struct Box0 *)w)->cells0[ind] = 2;
                    }
                    else
                        if (0 == get_cell(vx + w->x0, vy + w->y0, 0))
                            set_cell(vx + w->x0, vy + w->y0, 2, 0, 0);
                }
            }
        stat->hash ^= hash;
        stat->count += cnt;
    }
}

void life_step_box(struct Box * w, int dst, int age, struct Stat * stat) {
    assert(w);
    if (w->level > 0) {
        for(int idx = 0; idx < N*N; idx ++)
            if (w->cells[idx]) {
                if (w->cells[idx]->age >= age - 1) {
                    life_step_box(w->cells[idx], dst, age, stat);
                    if (w->cells[idx]->age == age)
                        w->age = age;
                }
                // we can adjust here how aggressively release empty boxes
                else if (w->cells[idx]->age < age - 3) {
                    release_box(w->cells[idx]);
                    w->cells[idx] = (struct Box*) 0;
                }

            }
    }
    else {
#define w0 ((struct Box0 *)w)
        unsigned int hash = 0;
        int cnt = 0;
        char * start = w0->cells0 + N0*N0*(1 - dst);
        char * end = w0->cells0 + N0*N0*(2 - dst);
        for (char * p = start; p < end; p ++) {
            if (!*p) continue;
            int idx = p - start;
            int y = idx / N0;
            int x = idx % N0;
            int n = 0;
            for (int j = 0; j < 9; j ++) {
                if (j == 4) continue;
                int vx = x + j % 3 - 1;
                int vy = y + j / 3 - 1;
                n += 1 == (0 <= vx && vx < N0 && 0 <= vy && vy < N0
                            ? (int) start[vy * N0 + vx]
                            : get_cell(vx + w->x0, vy + w->y0, 1 - dst));
            }
            if ((n == 3) | ((n == 2) & (*p == 1))) {
                cnt ++;
                if (cnt == 1) w->age = age;

                hash ^= (unsigned int)(x + w->x0) * rand_x + (unsigned int)(y + w->y0) * rand_y;

                char * dst_st = w0->cells0 + N0*N0*dst;
                dst_st[idx] = (char)1;

                for (int j = 0; j < 9; j ++) {
                    if (j == 4) continue;
                    int vx = x + j % 3 - 1;
                    int vy = y + j / 3 - 1;
                    if (0 <= vx && vx < N0 && 0 <= vy && vy < N0) {
                        char * xd = dst_st + vy * N0 + vx;
                        if (*xd != 1) *xd = (char)2;
                    }
                    else if (1 != get_cell(vx + w->x0, vy + w->y0, dst))
                        set_cell(vx + w->x0, vy + w->y0, 2, dst, age);
                }
            }
        }
        stat->hash ^= hash;
        stat->count += cnt;
#undef w0
    }
}

void life_clean_plane(struct Box * w, int dst) {
    assert(w);
    if (w->level > 0) {
        for(int idx = 0; idx < N*N; idx ++)
            if (w->cells[idx])
                life_clean_plane(w->cells[idx], dst);
    }
    else
        memset((char *)(((struct Box0 *)w)->cells0 + N0*N0*dst), '\0', N0*N0);
}

void life_prepare (struct Stat * stat) {
    if (world) {
        stat->count = 0;
        stat->hash = 0;
        life_prepare_box(world, stat);
    }
}

void life_step (int dst, int age, struct Stat * stat) {
    if (world) {
        stat->count = 0;
        stat->hash = 0;
        life_clean_plane(world, dst);
        life_step_box(world, dst, age, stat);
    }
}

void life_infin_read(int X, int Y, int x0, int y0, char * src, int sX, int sY) {
    for(int y = 0; y < sY; y ++)
        for (int x = 0; x < sX; x ++)
            set_cell(x + x0, y + y0, src[y * sX + x] == 'x', 0, 0);
}

void life_infin_print(int x0, int x1, int y0, int y1, int plane, int dbg) {
    for(int y = y0; y <= y1; y ++) {
        for(int x = x0; x <= x1; x ++) {
            int val = (int) get_cell(x,y,plane);
            printf(val == 1? "X" : (((val == 2) & dbg)?"+":"."));
        }
        printf("\n");
    }
}

/* -------------------------------------------------- *\
|*                     TESTING                        *|
\* -------------------------------------------------- */

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

extern int main () {
    const int X = 500;
    const int Y = 500;

#ifdef C4WA
    char * pos_0 = __builtin_memory + __builtin_offset;
    char * pos_1 = __builtin_memory + __builtin_offset + (X*Y + 1);
    mm_init(2*(X*Y+1), max(sizeof(struct Box0), sizeof(struct Box)));
#else
    char * pos_0 = malloc(X*Y + 1);
    char * pos_1 = malloc(X*Y + 1);
#endif


    char * initial_pos = "........."
                         ".....x..."
                         "...xxx..."
                         "....x...."
                         ".........";
    const int sX = 9;
    const int sY = 5;
    assert(sX * sY == strlen(initial_pos));

    /* This call would expands memory to fit pos_0 and pos_1 */
    life_infin_read(X, Y, (X - sX)/2, (Y - sY)/2, initial_pos, sX, sY);

    memset(pos_0, '\0', X*Y);
    memset(pos_1, '\0', X*Y);
    pos_0[X*Y] = 3;
    pos_1[X*Y] = 3;

    life_fin_read(pos_0, X, Y, (X - sX)/2, (Y - sY)/2, initial_pos, sX, sY);

    for(int x = 0; x < X; x ++)
        for(int y = 0; y < Y; y ++)
            assert(get_cell(x, y, 0) == pos_0[y*X+x]);

    struct Stat stat_f, stat_i;
    life_fin_prepare(pos_0, X, Y, &stat_f);
    life_prepare(&stat_i);
    assert(stat_f.count == stat_i.count);
    assert(stat_f.hash == stat_i.hash);

    int iter;
    int ok = 1;
    unsigned int hash[4];
    hash[3] = stat_i.hash;
    for (iter = 0; iter < 10000 && ok; iter ++) {
        int i = iter % 2;
        life_fin_step(i?pos_1:pos_0, i?pos_0:pos_1, X, Y, &stat_f);
        life_step(1 - i, iter + 1, &stat_i);

        int j;
        for(j = 0; j < 4 && hash[j] != stat_i.hash; j ++);
        if (j < 4) {
            printf("Cycle detected at iter = %d\n", iter);
            break;
        }
        for(j = 0; j < 4; j ++)
            hash[j] = j < 3? hash[j+1]: stat_i.hash;

        for(int x = 0; x < X && ok; x ++)
            for(int y = 0; y < Y && ok; y ++) {
                ok = get_cell(x, y, 1-i) == (i?pos_0:pos_1)[y*X+x];
                if (!ok) {
                    printf("(%d,%d): ⑩=%d, ∞=%d\n", x, y, (i?pos_0:pos_1)[y*X+x], get_cell(x, y, 1-i));

                    printf("Consistency broke down on iteration %d\n", iter);
                    int i = iter % 2;

                    const int xwin = 40;
                    const int ywin = 10;
                    int x0 = x - xwin/2;
                    int x1 = x0 + xwin - 1;
                    if (x0 < 0) {
                        x1 += -x0;
                        x0 = 0;
                    }
                    if (x1 > X - 1)
                        x1 = X - 1;

                    int y0 = y - ywin/2;
                    int y1 = y0 + ywin - 1;
                    if (y0 < 0) {
                        y1 += -y0;
                        y0 = 0;
                    }
                    if (y1 > Y - 1)
                        y1 = Y - 1;

                    printf("Showing window %d <= x <= %d, %d <= y <= %d\n", x0, x1, y0, y1);
                    printf("⑩ FINITE\n");
                    life_fin_print(i?pos_0:pos_1, X, x0, x1, y0, y1, 1);
                    printf("∞ INFINITE\n");
                    life_infin_print(x0, x1, y0, y1, 1-i, 1);
                }
            }
        assert(stat_f.count == stat_i.count);
        assert(stat_f.hash == stat_i.hash);
    }

    if (ok)
        printf("Successfully completed %d iterations\n", iter);

#ifdef C4WA
    int allocated, freed, current, in_use, capacity;

    mm_stat(&allocated, &freed, &current, &in_use, &capacity);
    printf("A/R/C: %d/%d/%d; CAP: %d/%d\n", allocated, freed, current, in_use, capacity);
#else
    printf("A/R/C: 1712/1600/112; CAP: 3/10\n");
#endif

    return 0;
}
// (499,51): ⑩=2, ∞=0
// Consistency broke down on iteration 1036
// Showing window 479 <= x <= 499, 46 <= y <= 55
// ⑩ FINITE
// .....................
// .....................
// .....................
// .....................
// .....................
// ....................+
// ....................+
// ....................+
// .....................
// .....................
// ∞ INFINITE
// .....................
// .....................
// .....................
// .....................
// .....................
// .....................
// .....................
// .....................
// .....................
// .....................
// A/R/C: 1712/1600/112; CAP: 3/10